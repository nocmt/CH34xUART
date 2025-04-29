package cn.wch.wchuartdemo.adapter;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.chip.type.ChipType2;
import cn.wch.wchuartdemo.R;

public class UsbDeviceDialogAdapter extends RecyclerView.Adapter<UsbDeviceDialogAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<UsbDevice> usbDevices;
    private OnClickListener onClickListener;

    public UsbDeviceDialogAdapter(@NonNull Context context, @NonNull ArrayList<UsbDevice> usbDevices) {
        this.context = context;
        this.usbDevices = usbDevices;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.dialog_devicelist_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        UsbDevice usbDevice = usbDevices.get(position);
        holder.name.setText(usbDevice.getDeviceName());
        holder.id.setText(String.format(Locale.getDefault(),"VID:%04X PID:%04X",usbDevice.getVendorId(),usbDevice.getProductId()));
        ChipType2 chipType = null;
        try {
            chipType = WCHUARTManager.getInstance().getChipType(usbDevice);
            if(chipType!=null){
                holder.type.setText(chipType.getDescription());
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(onClickListener!=null){
                        onClickListener.onClick(usbDevice);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public int getItemCount() {
        return usbDevices==null ? 0:usbDevices.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        TextView id;
        TextView type;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name=itemView.findViewById(R.id.name);
            id=itemView.findViewById(R.id.id);
            type=itemView.findViewById(R.id.type);
        }
    }

    public void setOnClickListener(OnClickListener onClickListener){
        this.onClickListener=onClickListener;
    }

    public interface OnClickListener{
        void onClick(UsbDevice usbDevice);
    }
}
