package cn.wch.wchuartdemo.adapter;

import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.callback.IDataCallback;
import cn.wch.wchuartdemo.LogUtil;
import cn.wch.wchuartdemo.R;
import cn.wch.wchuartdemo.entity.DeviceEntity;
import cn.wch.wchuartdemo.entity.ModemEntity;
import cn.wch.wchuartdemo.entity.ModemErrorEntity;
import cn.wch.wchuartdemo.entity.SerialEntity;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private ArrayList<DeviceEntity> deviceList;
    private HashMap<DeviceEntity,SerialListAdapter> maps;
    private FragmentActivity activity;
    private OnActionListener onActionListener;

    private final int ITEM_TYPE_EMPTY = 3;
    private final int ITEM_TYPE_NORMAL = 4;
    private View mEmptyView=null;

    public DeviceAdapter(@NonNull FragmentActivity activity) {
        this.activity=activity;
        deviceList=new ArrayList<>();
        maps=new HashMap<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType){
            case ITEM_TYPE_EMPTY:
                return new MyViewHolder(mEmptyView);
            case ITEM_TYPE_NORMAL:
                return new MyViewHolder(LayoutInflater.from(activity).inflate(R.layout.device_item,parent,false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        int type=getItemViewType(position);
        if(type == ITEM_TYPE_EMPTY)
            return;

        DeviceEntity entity = deviceList.get(position);
        //绘制
        holder.description.setText(String.format(Locale.getDefault(),"%s(%s)",entity.getUsbDevice().getDeviceName(),entity.getDescription()));
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDevice(entity.getUsbDevice(),entity);
            }
        });
        //初始化recyclerview
        RecyclerView recyclerView = holder.recyclerView;
        ArrayList<SerialEntity> serialEntities = entity.getSerialEntities();
        SerialListAdapter serialListAdapter=new SerialListAdapter(activity,serialEntities);
        maps.put(entity, serialListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false));
        recyclerView.setAdapter(serialListAdapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    public int getItemCount() {
        int itemCount=deviceList.size();
        if (null != mEmptyView && itemCount == 0) itemCount++;
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (null != mEmptyView && deviceList.size() == 0)
            return ITEM_TYPE_EMPTY;
        return ITEM_TYPE_NORMAL;
    }

    public boolean hasExist(@NonNull DeviceEntity deviceEntity){
        for (DeviceEntity entity : deviceList) {
            if(entity.getUsbDevice().equals(deviceEntity.getUsbDevice())){
                //已经存在
                LogUtil.d("已经存在");
                return true;
            }
        }
        return false;
    }

    public void addDevice(@NonNull DeviceEntity deviceEntity){
        if(!hasExist(deviceEntity)){
            deviceList.add(deviceEntity);
            notifyItemInserted(deviceList.size());
        }
    }

    public void removeDevice(@NonNull DeviceEntity deviceEntity){
        int index=-1;
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceEntity entity = deviceList.get(i);
            if(entity.getUsbDevice().equals(deviceEntity.getUsbDevice())){
                index=i;
            }
        }
        if(index>=0){
            maps.remove(deviceEntity);
            deviceList.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void removeDevice(@NonNull UsbDevice usbDevice){

        int index=-1;
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceEntity entity = deviceList.get(i);
            if(entity.getUsbDevice().equals(usbDevice)){
                index=i;
                break;
            }
        }
        if(index>=0){
            maps.remove(deviceList.get(index));
            deviceList.remove(index);
            notifyItemRemoved(index);
        }
        if(onActionListener!=null){
            onActionListener.onRemove(usbDevice);
        }
        WCHUARTManager.getInstance().disconnect(usbDevice);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()){
            onBindViewHolder(holder,position);
        }else {
            DeviceEntity deviceEntity = deviceList.get(position);
            SerialListAdapter serialListAdapter = maps.get(deviceEntity);
            if(serialListAdapter==null){
                return;
            }
            Object o = payloads.get(0);
            if(o instanceof ModemEntity){
                serialListAdapter.updateModemStatus((ModemEntity) o);
            }else if( o instanceof ModemErrorEntity){
                serialListAdapter.updateModemErrorStatus((ModemErrorEntity) o);
            }
        }

    }

    public void updateDeviceModemStatus(@NonNull UsbDevice usbDevice, int serialNumber, boolean DCD, boolean DSR, boolean CTS, boolean RING){
        int index=-1;
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceEntity entity = deviceList.get(i);
            if(entity.getUsbDevice().equals(usbDevice)){
                index=i;
            }
        }
        if(index>=0){
            notifyItemChanged(index,new ModemEntity(serialNumber, DCD, DSR, CTS, RING));
        }
    }

    public void updateDeviceModemErrorStatus(@NonNull UsbDevice usbDevice,@NonNull ModemErrorEntity error){
        int index=-1;
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceEntity entity = deviceList.get(i);
            if(entity.getUsbDevice().equals(usbDevice)){
                index=i;
            }
        }
        if(index>=0){
            notifyItemChanged(index,error);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView description;
        Button button;
        RecyclerView recyclerView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            if(itemView==mEmptyView){
                return;
            }
            description=itemView.findViewById(R.id.tvDescription);
            button=itemView.findViewById(R.id.btnClose);
            recyclerView=itemView.findViewById(R.id.rvSerial);
        }
    }

    public void setEmptyView(View view){
        mEmptyView=view;
    }

    public interface OnActionListener{
        void onRemove(UsbDevice usbDevice);
    }

    public void setActionListener(OnActionListener actionListener){
        this.onActionListener=onActionListener;
    }

    //关闭设备
    private void closeDevice(@NonNull UsbDevice usbDevice, DeviceEntity entity){
        WCHUARTManager.getInstance().disconnect(usbDevice);
        removeDevice(entity);
        if(onActionListener!=null){
            onActionListener.onRemove(usbDevice);
        }
    }

}
