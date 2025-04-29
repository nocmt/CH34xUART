package cn.wch.wchuartdemo.adapter;

import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cn.wch.uartlib.WCHUARTManager;

import cn.wch.uartlib.base.error.SerialErrorType;
import cn.wch.uartlib.exception.ChipException;
import cn.wch.uartlib.exception.UartLibException;
import cn.wch.wchuartdemo.LogManager;
import cn.wch.wchuartdemo.R;
import cn.wch.wchuartdemo.WCHApplication;
import cn.wch.wchuartdemo.entity.ModemEntity;
import cn.wch.wchuartdemo.entity.ModemErrorEntity;
import cn.wch.wchuartdemo.entity.SerialBaudBean;
import cn.wch.wchuartdemo.entity.SerialEntity;
import cn.wch.wchuartdemo.ui.CustomTextView;
import cn.wch.wchuartdemo.ui.SerialConfigDialog;
import cn.wch.wchuartdemo.utils.FormatUtil;
import cn.wch.wchuartdemo.utils.ToastUtil;

import android.util.Log;
import cn.wch.wchuartdemo.LogUtil;

public class SerialListAdapter extends RecyclerView.Adapter<SerialListAdapter.MyViewHolder> {

    private FragmentActivity activity;
    private ArrayList<SerialEntity> serialEntities;
    private HashMap<Integer, Integer> writeCountMap;

    private Handler handler=new Handler(Looper.getMainLooper());

    public SerialListAdapter(@NonNull FragmentActivity activity, @NonNull ArrayList<SerialEntity> serialEntities) {
        this.activity=activity;
        this.serialEntities = serialEntities;
        writeCountMap=new HashMap<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(activity).inflate(R.layout.serial_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        SerialEntity serialEntity = serialEntities.get(position);
        writeCountMap.put(serialEntity.getSerialNumber(),0);
        holder.tvDescription.setText(String.format(Locale.getDefault(),"串口%d",serialEntity.getSerialNumber()));
        //设置串口
        holder.cbDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setDTR(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), isChecked);
            }
        });
        holder.cbRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setRTS(serialEntity.getUsbDevice(),serialEntity.getSerialNumber(),isChecked);

            }
        });
        holder.cbBREAK.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setBreak(serialEntity.getUsbDevice(),serialEntity.getSerialNumber(),isChecked);
            }
        });

        holder.setSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SerialConfigDialog dialog=SerialConfigDialog.newInstance(null);
                dialog.setCancelable(false);
                dialog.show(activity.getSupportFragmentManager(),SerialConfigDialog.class.getName());
                dialog.setListener(new SerialConfigDialog.onClickListener() {
                    @Override
                    public void onSetBaud(SerialBaudBean data) {

                        if(setSerialParameter(serialEntity.getUsbDevice(),serialEntity.getSerialNumber(),data )){
                            holder.serialInfo.setText(data.toString());
                            showToast("设置成功");
                        }else {
                            showToast("设置失败");
                        }
                    }
                });
            }
        });
        //发送
        holder.write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = holder.writeBuffer.getText().toString();
                if(TextUtils.isEmpty(s)){
                    showToast("发送内容为空");
                    return;
                }
                byte[] bytes = null;
                if(holder.scWrite.isChecked()){
                    if(!s.matches("([0-9|a-f|A-F]{2})*")){
                        showToast("发送内容不符合HEX规范");
                        return;
                    }
                    bytes= FormatUtil.hexStringToBytes(s);
                }else {
                    bytes = s.getBytes(StandardCharsets.UTF_8);
                }
                int ret = writeData(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), bytes, bytes.length);
                if(ret>0){
                    //更新发送计数
                    int writeCount = getWriteCount(serialEntity.getSerialNumber());
                    writeCount+=ret;
                    setWriteCount(serialEntity.getSerialNumber(),writeCount);
                    holder.writeCount.setText(String.format(Locale.getDefault(),"发送计数：%d字节",writeCount));
                    //showToast("发送成功");
                }else {
                    showToast("发送失败");
                }
            }
        });
        holder.clearWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWriteCount(serialEntity.getSerialNumber(),0);
                holder.writeBuffer.setText("");
                holder.writeCount.setText(String.format(Locale.getDefault(),"发送计数：%d字节",getWriteCount(serialEntity.getSerialNumber())));
            }
        });

        holder.queryError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int i1 = WCHUARTManager.getInstance().querySerialErrorCount(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), SerialErrorType.OVERRUN);
                    int i2 = WCHUARTManager.getInstance().querySerialErrorCount(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), SerialErrorType.PARITY);
                    int i3 = WCHUARTManager.getInstance().querySerialErrorCount(serialEntity.getUsbDevice(), serialEntity.getSerialNumber(), SerialErrorType.FRAME);
                    showToast(String.format(Locale.getDefault(),"overrun error:%d parity error:%d frame error:%d ",i1,i2,i3));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()){
            onBindViewHolder(holder,position);
        }else {
            Object o = payloads.get(0);
            if(o instanceof ModemEntity){
                ModemEntity o1 = (ModemEntity) o;
                holder.cbDCD.setChecked(o1.DCD);
                holder.cbDSR.setChecked(o1.DSR);
                holder.cbCTS.setChecked(o1.CTS);
                holder.cbRING.setChecked(o1.RING);
            }else if(o instanceof ModemErrorEntity){
                ModemErrorEntity o2 = (ModemErrorEntity) o;
                ModemErrorEntity.ErrorType errorType = o2.errorType;
                if(errorType!=null){
                    switch (errorType){
                        case FRAME:
                            holder.cbFrame.setChecked(true);
                            break;
                        case OVERRUN:
                            holder.cbOverrun.setChecked(true);
                            break;
                        case PARITY:
                            holder.cbParity.setChecked(true);
                            break;
                    }
                }

            }
        }


    }

    public void updateModemStatus(ModemEntity modemEntity){
        int index=-1;
        for (int i = 0; i < serialEntities.size(); i++) {
            SerialEntity serialEntity = serialEntities.get(i);
            if(serialEntity.getSerialNumber()==modemEntity.serialNumber){
                index=i;
                break;
            }
        }
        if(index>=0){
            notifyItemChanged(index,modemEntity);
        }
    }

    public void updateModemErrorStatus(ModemErrorEntity errorEntity){
        int index=-1;
        for (int i = 0; i < serialEntities.size(); i++) {
            SerialEntity serialEntity = serialEntities.get(i);
            if(serialEntity.getSerialNumber()==errorEntity.serialNumber){
                index=i;
                break;
            }
        }
        if(index>=0){
            notifyItemChanged(index,errorEntity);
        }
    }

    @Override
    public int getItemCount() {
        return serialEntities==null? 0:serialEntities.size();
    }

    public SerialEntity get(int position){
        return serialEntities==null? null :serialEntities.get(position);
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView tvDescription;
        TextView serialInfo;
        CustomTextView setSerial;

        AppCompatCheckBox cbDTR;
        AppCompatCheckBox cbRTS;
        AppCompatCheckBox cbBREAK;

        AppCompatCheckBox cbDCD;
        AppCompatCheckBox cbDSR;
        AppCompatCheckBox cbCTS;
        AppCompatCheckBox cbRING;

        AppCompatCheckBox cbOverrun;
        AppCompatCheckBox cbParity;
        AppCompatCheckBox cbFrame;

        CustomTextView queryError;

        TextView write;
        CustomTextView clearWrite;
        SwitchCompat scWrite;
        TextView writeCount;


        EditText writeBuffer;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription=itemView.findViewById(R.id.tvSerialDescription);
            serialInfo=itemView.findViewById(R.id.tvSerialInfo);
            setSerial=itemView.findViewById(R.id.tvSerialConfig);

            cbDTR=itemView.findViewById(R.id.cbDTR);
            cbRTS=itemView.findViewById(R.id.cbRTS);
            cbBREAK=itemView.findViewById(R.id.cbBreak);

            cbDCD=itemView.findViewById(R.id.cbDCD);
            cbDSR=itemView.findViewById(R.id.cbDSR);
            cbCTS=itemView.findViewById(R.id.cbCTS);
            cbRING=itemView.findViewById(R.id.cbRing);

            cbOverrun=itemView.findViewById(R.id.cbOverrun);
            cbParity=itemView.findViewById(R.id.cbParity);
            cbFrame=itemView.findViewById(R.id.cbFrame);

            queryError=itemView.findViewById(R.id.queryErrorStatus);

            write=itemView.findViewById(R.id.tvWrite);
            writeBuffer=itemView.findViewById(R.id.send_data);
            writeCount=itemView.findViewById(R.id.tvWriteCount);

            clearWrite=itemView.findViewById(R.id.tvClearWrite);

            scWrite=itemView.findViewById(R.id.scWrite);

        }
    }
    //设置串口参数
    boolean setSerialParameter(UsbDevice usbDevice,int serialNumber,SerialBaudBean baudBean){
        try {
            boolean b = WCHUARTManager.getInstance().setSerialParameter(usbDevice, serialNumber,
                    baudBean.getBaud(), baudBean.getData(), baudBean.getStop(), baudBean.getParity(),baudBean.isFlow());
            return b;
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return false;
    }

    public void setDTR(UsbDevice usbDevice,int serialNumber,boolean checked){
        try {
            boolean b=WCHUARTManager.getInstance().setDTR(usbDevice, serialNumber, checked);
            if(!b){
                showToast("设置DTR失败");
            }
            //showToast("设置DTR"+(b?"成功":"失败"));
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    public void setRTS(UsbDevice usbDevice,int serialNumber,boolean checked){
        try {
            boolean b=WCHUARTManager.getInstance().setRTS(usbDevice, serialNumber, checked);
            if(!b){
                showToast("设置RTS失败");
            }
            //showToast("设置RTS"+(b?"成功":"失败"));
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    public void setBreak(UsbDevice usbDevice,int serialNumber,boolean checked){
        try {
            boolean b=WCHUARTManager.getInstance().setBreak(usbDevice, serialNumber, checked);
            if(!b){
                showToast("设置Break失败");
            }
            //showToast("设置Break"+(b?"成功":"失败"));
        } catch (Exception e) {
            showToast(e.getMessage());
        }
    }

    //写数据
    int writeData(UsbDevice usbDevice,int serialNumber,@NonNull byte[] data,int length){
        try {
            int write = WCHUARTManager.getInstance().syncWriteData(usbDevice, serialNumber, data,length,2000);
            if (write > 0) {
                // 记录发送的数据到日志文件
                LogManager.getInstance().logData("发送", serialNumber, data, length);
            }
            return write;
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return -2;
    }

    public int getWriteCount(int serialNumber){
        return writeCountMap.get(serialNumber);
    }

    public void setWriteCount(int serialNumber,int newValue){
        writeCountMap.put(serialNumber,newValue);
    }



    void showToast(String message){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WCHApplication.getContext(),message,Toast.LENGTH_SHORT).show();
                //ToastUtil.create(activity,message).show();
            }
        });

    }

}
