package cn.wch.wchuartdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.base.error.SerialErrorType;
import cn.wch.uartlib.callback.IDataCallback;
import cn.wch.uartlib.callback.IUsbStateChange;
import cn.wch.uartlib.callback.IModemStatus;
import cn.wch.uartlib.chip.type.ChipType2;
import cn.wch.uartlib.exception.ChipException;
import cn.wch.uartlib.exception.NoPermissionException;
import cn.wch.uartlib.exception.UartLibException;
import cn.wch.wchuartdemo.adapter.DeviceAdapter;
import cn.wch.wchuartdemo.entity.DeviceEntity;
import cn.wch.wchuartdemo.entity.ModemErrorEntity;
import cn.wch.wchuartdemo.entity.SerialEntity;
import cn.wch.wchuartdemo.ui.CustomTextView;
import cn.wch.wchuartdemo.ui.DeviceListDialog;
import cn.wch.wchuartdemo.ui.GPIODialog;
import cn.wch.wchuartdemo.utils.FormatUtil;

public class MainActivity extends AppCompatActivity {
    RecyclerView deviceRecyclerVIew;
    DeviceAdapter deviceAdapter;
    private Context context;

    //接收区
    TextView readBuffer;
    CustomTextView clearRead;
    SwitchCompat scRead;
    TextView readCount;
    //保存各个串口的接收计数
    HashMap<String, Integer> readCountMap=new HashMap<>();
    //已打开的设备列表
    final Set<UsbDevice> devices= Collections.synchronizedSet(new HashSet<UsbDevice>());
    
    // 存储权限请求码
    private static final int STORAGE_PERMISSION_CODE = 1001;
    //读线程
    Thread readThread;
    boolean flag=false;

    //接收文件测试。文件默认保存在-->内部存储\Android\data\cn.wch.wchuartdemo\files\TestFile下
    private static boolean FILE_TEST=false;

    private final boolean useReadThread=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.context=this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 检查并请求存储权限
        checkStoragePermission();

        // 初始化LogManager
        LogManager.getInstance().initLogFile(this);

        if(!UsbFeatureSupported()){
            showToast("系统不支持USB Host功能");
            System.exit(0);
            return;
        }
        initUI();
        if(useReadThread){
            readThread=new ReadThread();
            readThread.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maim,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId==R.id.enumDevice){
            enumDevice();
        }else if(itemId==R.id.configGPIO){
            openGPIODialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止读线程
        if(useReadThread){
            stopReadThread();
        }
        //停止文件测试
        if(FILE_TEST){
            cancelLinks();
        }
        //关闭所有连接设备
        closeAll();
        //释放资源
        WCHUARTManager.getInstance().close(WCHApplication.getContext());
        // 关闭日志文件
        LogManager.getInstance().closeLogFile();
        LogUtil.d("onDestroy");
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * 系统是否支持USB Host功能
     *
     * @return true:系统支持USB Host false:系统不支持USB Host
     */
    public boolean UsbFeatureSupported() {
        boolean bool = this.getPackageManager().hasSystemFeature(
                "android.hardware.usb.host");
        return bool;
    }

    void initUI(){
        deviceRecyclerVIew=findViewById(R.id.rvDevice);
        readBuffer=findViewById(R.id.tvReadData);
        clearRead=findViewById(R.id.tvClearRead);
        scRead=findViewById(R.id.scRead);
        readCount=findViewById(R.id.tvReadCount);
        //初始化recyclerview
        deviceRecyclerVIew.setNestedScrollingEnabled(false);
        deviceAdapter =new DeviceAdapter(this);

        deviceRecyclerVIew.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        deviceAdapter.setEmptyView(LayoutInflater.from(this).inflate(R.layout.empty_view,deviceRecyclerVIew,false));
        deviceRecyclerVIew.setAdapter(deviceAdapter);
        deviceAdapter.setActionListener(new DeviceAdapter.OnActionListener() {
            @Override
            public void onRemove(UsbDevice usbDevice) {
                removeReadDataDevice(usbDevice);
            }
        });

        readBuffer.setMovementMethod(ScrollingMovementMethod.getInstance());
        clearRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearReadData();
            }
        });
        //监测USB插拔状态
        monitorUSBState();
        //动态申请权限
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},111);
        }

    }

    void openGPIODialog(){
        //simply,select first device
        UsbDevice device=null;
        Iterator<UsbDevice> iterator = devices.iterator();
        while (iterator.hasNext()){
            device = iterator.next();
        }
        if(device!=null){
            GPIODialog dialog=GPIODialog.newInstance(device);
            dialog.setCancelable(false);
            dialog.show(getSupportFragmentManager(),GPIODialog.class.getName());

        }

    }

    /**
     * 枚举当前所有符合要求的设备，显示设备列表
     */
    void enumDevice(){
        try {
            //枚举符合要求的设备
            ArrayList<UsbDevice> usbDeviceArrayList = WCHUARTManager.getInstance().enumDevice();
            if(usbDeviceArrayList.size()==0){
                showToast("no matched devices");
                return;
            }
            //显示设备列表dialog
            DeviceListDialog deviceListDialog=DeviceListDialog.newInstance(usbDeviceArrayList);
            deviceListDialog.setCancelable(false);
            deviceListDialog.show(getSupportFragmentManager(),DeviceListDialog.class.getName());
            deviceListDialog.setOnClickListener(new DeviceListDialog.OnClickListener() {
                @Override
                public void onClick(UsbDevice usbDevice) {
                    //选择了某一个设备打开
                    open(usbDevice);
                }
            });
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }

    /**
     * 从设备列表中打开某个设备
     *
     * @param usbDevice
     */
    void open(@NonNull UsbDevice usbDevice){
        if(WCHUARTManager.getInstance().isConnected(usbDevice)){
            showToast("当前设备已经打开");
            return;
        }
        try {
            boolean b = WCHUARTManager.getInstance().openDevice(usbDevice);
            if(b){
                //打开成功
                //更新显示的ui
                update(usbDevice);
                //初始化接收计数
                int serialCount = 0;
                try {
                    serialCount = WCHUARTManager.getInstance().getSerialCount(usbDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < serialCount; i++) {
                    readCountMap.put(FormatUtil.getSerialKey(usbDevice,i),0);
                }
                //将该设备添加至已打开设备列表,在读线程ReadThread中,将会读取该设备的每个串口数据
                addToReadDeviceSet(usbDevice);
                //用作文件对比测试,在打开每个设备时，对每个串口新建对应的保存数据的文件
                if(FILE_TEST){
                    for (int i = 0; i < serialCount; i++) {
                        linkSerialToFile(usbDevice,i);
                    }
                }
                registerModemStatusCallback(usbDevice);
                if(!useReadThread){
                   registerDataCallback(usbDevice);
                }
            }else {
                showToast("打开失败");
            }
        } catch (ChipException e) {
            LogUtil.d(e.getMessage());
        } catch (NoPermissionException e) {
            //没有权限打开该设备
            //申请权限
            showToast("没有权限打开该设备");
            requestPermission(usbDevice);
        } catch (UartLibException e) {
            e.printStackTrace();
        }
    }

    /**
     * 申请读写权限
     * @param usbDevice
     */
    private void requestPermission(@NonNull UsbDevice usbDevice){
        try {
            WCHUARTManager.getInstance().requestPermission(this,usbDevice);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }

    /**
     * 监测USB的状态
     */
    private void monitorUSBState(){
        WCHUARTManager.getInstance().setUsbStateListener(new IUsbStateChange() {
            @Override
            public void usbDeviceDetach(UsbDevice device) {
                //设备移除
                removeReadDataDevice(device);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //从界面上移除
                        if(deviceAdapter!=null){
                            deviceAdapter.removeDevice(device);
                        }
                    }
                });
                if(FILE_TEST){
                    cancelDeviceLinks(device);
                }
            }

            @Override
            public void usbDeviceAttach(UsbDevice device) {
                //设备插入
            }

            @Override
            public void usbDevicePermission(UsbDevice device, boolean result) {
                //请求打开设备权限结果
            }
        });
    }

    /**
     * //recyclerView更新UI
     * @param usbDevice
     */
    void update(UsbDevice usbDevice){
        //根据vid/pid获取芯片类型
        ChipType2 chipType = null;
        try {
            chipType = WCHUARTManager.getInstance().getChipType(usbDevice);
            //获取芯片串口数目,为负则代表出错
            int serialCount = WCHUARTManager.getInstance().getSerialCount(usbDevice);
            //构建recyclerView所绑定的数据,添加设备
            ArrayList<SerialEntity> serialEntities=new ArrayList<>();
            for (int i = 0; i < serialCount; i++) {
                SerialEntity serialEntity=new SerialEntity(usbDevice,i);
                serialEntities.add(serialEntity);
            }
            DeviceEntity deviceEntity=new DeviceEntity(usbDevice,chipType.getDescription(),serialEntities);
            if(deviceAdapter.hasExist(deviceEntity)){
                //已经显示
                showToast("该设备已经存在");
            }else {
                deviceAdapter.addDevice(deviceEntity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 关闭所有设备
     */
    void closeAll(){
        ArrayList<UsbDevice> usbDeviceArrayList = null;
        try {
            usbDeviceArrayList = WCHUARTManager.getInstance().enumDevice();
            for (UsbDevice usbDevice : usbDeviceArrayList) {
                if(WCHUARTManager.getInstance().isConnected(usbDevice)){
                    WCHUARTManager.getInstance().disconnect(usbDevice);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addToReadDeviceSet(@NonNull UsbDevice usbDevice){
        synchronized (devices){
            devices.add(usbDevice);
        }

    }

    private void removeReadDataDevice(@NonNull UsbDevice usbDevice){
        synchronized (devices){
            devices.remove(usbDevice);
        }
    }

    private void registerModemStatusCallback(UsbDevice usbDevice){
        try {
            WCHUARTManager.getInstance().registerModemStatusCallback(usbDevice, new IModemStatus() {
                @Override
                public void onStatusChanged(int serialNumber, boolean isDCDRaised, boolean isDSRRaised, boolean isCTSRaised, boolean isRINGRaised) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.updateDeviceModemStatus(usbDevice,serialNumber,isDCDRaised,isDSRRaised,isCTSRaised,isRINGRaised);
                        }
                    });
                }

                @Override
                public void onOverrunError(int serialNumber) {
                    try {
                        int count=WCHUARTManager.getInstance().querySerialErrorCount(usbDevice,serialNumber, SerialErrorType.OVERRUN);
                        LogUtil.d("overrun error: "+count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.updateDeviceModemErrorStatus(usbDevice,new ModemErrorEntity(serialNumber, ModemErrorEntity.ErrorType.OVERRUN));

                        }
                    });

                }

                @Override
                public void onParityError(int serialNumber) {
                    try {
                        int count=WCHUARTManager.getInstance().querySerialErrorCount(usbDevice,serialNumber, SerialErrorType.PARITY);
                        LogUtil.d("parity error: "+count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("parity error!");
                            deviceAdapter.updateDeviceModemErrorStatus(usbDevice,new ModemErrorEntity(serialNumber, ModemErrorEntity.ErrorType.PARITY));

                        }
                    });
                }

                @Override
                public void onFrameError(int serialNumber) {
                    try {
                        int count=WCHUARTManager.getInstance().querySerialErrorCount(usbDevice,serialNumber, SerialErrorType.FRAME);
                        LogUtil.d("frame error: "+count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("frame error!");
                            deviceAdapter.updateDeviceModemErrorStatus(usbDevice,new ModemErrorEntity(serialNumber, ModemErrorEntity.ErrorType.FRAME));

                        }
                    });
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerDataCallback(UsbDevice usbDevice){
        try {
            WCHUARTManager.getInstance().registerDataCallback(usbDevice, new IDataCallback() {
                @Override
                public void onData(int serialNumber, byte[] buffer, int length) {
                    //LogUtil.d(String.format(Locale.getDefault(),"serial %d receive data %d:%s", serialNumber,length, FormatUtil.bytesToHexString(buffer, length)));
                    //1.注意回调的执行线程与调用回调方法的线程属于同一线程
                    //2.此处所在的线程将是线程池中多个端点的读取线程，可打印线程id查看
                    //3.buffer是底层数组，如果此处将其传给其他线程使用，例如通过runOnUiThread显示数据在界面上,
                    //涉及到线程切换需要一定时间，buffer可能被读到的新数据覆盖，可以新建一个临时数组保存数据
                    LogUtil.d("Application onData");
                    byte[] data=new byte[length];
                    System.arraycopy(buffer,0,data,0,data.length);
                    // 记录接收到的数据到日志文件
                    LogManager.getInstance().logData("接收", serialNumber, data, length);
                    if(FILE_TEST){
                        updateReadDataToFile(usbDevice,serialNumber,data,length);
                    }else {
                        updateReadData(usbDevice,serialNumber,data,length);
                    }


                }
            });
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
    }
    public class ReadThread extends Thread{

        public ReadThread() {
            flag=true;
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            LogUtil.d("---------------开始读取数据");
            while (flag){
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(devices.isEmpty()){
                    continue;
                }
                //遍历已打开的设备列表中的设备
                synchronized (devices){
                    Iterator<UsbDevice> iterator = devices.iterator();
                    while (iterator.hasNext()){
                        UsbDevice device = iterator.next();
                        int serialCount =0;
                        try {
                            serialCount = WCHUARTManager.getInstance().getSerialCount(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //读取该设备每个串口的数据
                        for (int i = 0; i < serialCount; i++) {
                            try {
                                //用于使用readData的方法读数据，该函数作用为查询当前缓冲区数据的长度
//                                int currentReadDataLength = WCHUARTManager.getInstance().getCurrentReadDataLength(device, i);
//                                if(currentReadDataLength<=0){
//                                    continue;
//                                }
//                                当前缓冲区有数据
                                byte[] bytes = WCHUARTManager.getInstance().readData(device, i,200);
                                if(bytes!=null){
                                    //使用获取到的数据
                                    //updateReadData(device,i,bytes,bytes.length);
                                    if(FILE_TEST){
                                        updateReadDataToFile(device,i,bytes,bytes.length);
                                    }else {
                                        updateReadData(device,i,bytes,bytes.length);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                LogUtil.d("异常:" + e.getMessage());
                                break;
                            }
                        }
                    }
                }
            }
            LogUtil.d("读取数据线程结束");
        }
    }

    public void stopReadThread(){
        if(readThread!=null && readThread.isAlive()){
            flag=false;
        }
    }

    private void updateReadData(@NonNull UsbDevice usbDevice,int serialNumber, byte[] buffer, int length){
        if(buffer==null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (length == 0){
                    return;
                }
                Integer integer = readCountMap.get(FormatUtil.getSerialKey(usbDevice, serialNumber));
                if(integer==null){
                    //不包含此key
                    return;
                }
                //更新计数
                integer+=length;
                readCountMap.put(FormatUtil.getSerialKey(usbDevice, serialNumber),integer);
                //

                String result="";
                if (readBuffer.getText().toString().length() >= 1500) {
                    readBuffer.setText("");
                    readBuffer.scrollTo(0, 0);
                }

                if(scRead.isChecked()){
                    result= FormatUtil.bytesToHexString(buffer, length);

                }else {
                    result=new String(buffer,0,length);
                }
                String readBufferLogPrefix = FormatUtil.getReadBufferLogPrefix(usbDevice, serialNumber,integer);
                //LogUtil.d(readBufferLogPrefix);
                LogUtil.d("result:" + result);
                readBuffer.append(readBufferLogPrefix+result+"\r\n");

                int offset = readBuffer.getLineCount() * readBuffer.getLineHeight();
                //int maxHeight = usbReadValue.getMaxHeight();
                int height = readBuffer.getHeight();
                //USBLog.d("offset: "+offset+"  maxHeight: "+maxHeight+" height: "+height);
                if (offset > height) {
                    //USBLog.d("scroll: "+(offset - usbReadValue.getHeight() + usbReadValue.getLineHeight()));
                    readBuffer.scrollTo(0, offset - readBuffer.getHeight() + readBuffer.getLineHeight());
                }
            }
        });
    }


    private void clearReadData(){
        readBuffer.scrollTo(0,0);
        readBuffer.setText("");
        for (String s : readCountMap.keySet()) {
            readCountMap.put(s,0);
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已被授予
                LogUtil.d("存储权限已授予");
            } else {
                // 权限被拒绝
                showToast("需要存储权限以保存日志文件");
            }
        }
    }

    private void showToast(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //ToastUtil.create(context,message).show();
                Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            }
        });
    }
    ///////////////////////////////////////将数据保存至文件,与发送文件对比测试////////////////////////////////////////////////////

    //该Map的key是每个设备的串口，value是其对应的保存数据的文件的fileStream
    private HashMap<String, FileOutputStream> fileOutputStreamMap=new HashMap<>();

    //用作文件对比测试,在打开每个设备时，每个串口都新建对应的保存数据的文件，其映射关系保存到fileOutputStreamMap中
    private void linkSerialToFile(UsbDevice usbDevice,int serialNumber){
        LogUtil.d("linkSerialToFile:");
        File testFile = getExternalFilesDir("TestFile");
        File file=new File(testFile,WCHUARTManager.getInstance().getChipType(usbDevice).toString()+"_"+serialNumber+".txt");
        if(file.exists()){
            file.delete();
        }
        try {
            boolean ret = file.createNewFile();
            LogUtil.d("新建文件:"+ret);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            if(!fileOutputStreamMap.containsKey(FormatUtil.getSerialKey(usbDevice,serialNumber))){
                fileOutputStreamMap.put(FormatUtil.getSerialKey(usbDevice,serialNumber),fileOutputStream);
            }
        } catch (IOException e) {
            LogUtil.d(e.getMessage());
        }

    }

    //将接收到的数据保存至文件，用作对比
    private void updateReadDataToFile(@NonNull UsbDevice usbDevice,int serialNumber, byte[] buffer, int length){
        updateToFile(usbDevice, serialNumber, buffer, length);
    }

    private void updateToFile(@NonNull UsbDevice usbDevice,int serialNumber, byte[] buffer, int length){
        if(fileOutputStreamMap.containsKey(FormatUtil.getSerialKey(usbDevice,serialNumber))){
            FileOutputStream fileOutputStream = fileOutputStreamMap.get(FormatUtil.getSerialKey(usbDevice, serialNumber));
            LogUtil.d("save data to file");
            try {
                fileOutputStream.write(buffer,0,length);
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //结束保存至文件的功能,关闭Stream
    private void cancelLinks(){
        for (String s : fileOutputStreamMap.keySet()) {
            FileOutputStream fileOutputStream = fileOutputStreamMap.get(s);
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
                fileOutputStream=null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelDeviceLinks(UsbDevice usbDevice){
        if(fileOutputStreamMap==null){
            return;
        }
        for (int i = 0; i < 8; i++) {
            FileOutputStream fileOutputStream = fileOutputStreamMap.get(FormatUtil.getSerialKey(usbDevice, i));
            if(fileOutputStream==null){
                continue;
            }
            LogUtil.d("close file"+usbDevice.getDeviceName()+" "+i);
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
                fileOutputStream=null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}