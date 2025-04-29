package cn.wch.wchuartdemo.ui;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.base.gpio.GPIO_DIR;
import cn.wch.uartlib.base.gpio.GPIO_Status;
import cn.wch.uartlib.base.gpio.GPIO_VALUE;
import cn.wch.uartlib.chip.type.ChipType2;
import cn.wch.wchuartdemo.LogUtil;
import cn.wch.wchuartdemo.R;

public class GPIOAdapter extends RecyclerView.Adapter<GPIOAdapter.MyViewHolder> {

    private Context context;
    private List<GPIO_Status> gpioStatusList;
    private UsbDevice device;
    public GPIOAdapter(Context context,UsbDevice device) {
        this.context=context;
        this.device=device;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.dialog_gpio_item,null,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        LogUtil.d("position:"+position);
        holder.cbVal.setOnCheckedChangeListener(null);
        holder.cbEnable.setOnCheckedChangeListener(null);
        holder.rbIn.setOnCheckedChangeListener(null);
        holder.rbOut.setOnCheckedChangeListener(null);
        holder.cbVal.setOnCheckedChangeListener(null);

        GPIO_Status gpio_status = gpioStatusList.get(position);

        holder.cbEnable.setChecked(gpio_status.isEnabled());
        ChipType2 chipType=null;
        try {
            chipType = WCHUARTManager.getInstance().getChipType(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(chipType==null){
            return;
        }
//        if(ChipType2.CHIP_CH9104L==chipType){
//            holder.tvLabel.setText(getCH9104GPIOLabel(gpio_status.getGpioIndex()));
//        }else {
//            holder.tvLabel.setText(String.format(Locale.getDefault(),"GPIO%d",gpio_status.getGpioIndex()));
//        }
        holder.tvLabel.setText(String.format(Locale.getDefault(),"GPIO%d",gpio_status.getGpioIndex()));
        GPIO_DIR dir = gpio_status.getDir();
        if(dir==GPIO_DIR.IN){
            holder.rbIn.setChecked(true);

        }else {
            holder.rbOut.setChecked(true);

        }
        GPIO_VALUE value = gpio_status.getValue();
        holder.cbVal.setChecked(value==GPIO_VALUE.HIGH);

        //change status
        holder.cbEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setCurrentStatus(holder.getAdapterPosition(),isChecked);
            }
        });
        holder.rbIn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                setCurrentStatus(gpio_status.getGpioIndex(),isChecked ? GPIO_DIR.IN:GPIO_DIR.OUT);
                setCurrentStatus(holder.getAdapterPosition(),isChecked ? GPIO_DIR.IN:GPIO_DIR.OUT);

            }
        });
        holder.rbOut.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                setCurrentStatus(gpio_status.getGpioIndex(),isChecked ? GPIO_DIR.OUT:GPIO_DIR.IN);
                setCurrentStatus(holder.getAdapterPosition(),isChecked ? GPIO_DIR.OUT:GPIO_DIR.IN);
            }
        });
        holder.cbVal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setCurrentStatus(holder.getAdapterPosition(),isChecked ? GPIO_VALUE.HIGH:GPIO_VALUE.LOW);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gpioStatusList==null ?0:gpioStatusList.size();
    }

    public void update(List<GPIO_Status> list){
        this.gpioStatusList=list;
        notifyDataSetChanged();
    }

    public List<GPIO_Status> getCurrentStatus(){
        return gpioStatusList;
    }

    public void setCurrentStatus(int position,boolean enable){
        LogUtil.d("setCurrentStatus "+position);
        GPIO_Status gpio_status = gpioStatusList.get(position);
        gpio_status.setEnabled(enable);
        gpioStatusList.set(position,gpio_status);
    }

    public void setCurrentStatus(int position,GPIO_DIR dir){
        LogUtil.d("setCurrentStatus "+position);
        GPIO_Status gpio_status = gpioStatusList.get(position);
        gpio_status.setDir(dir);
        gpioStatusList.set(position,gpio_status);
    }

    public void setCurrentStatus(int position,GPIO_VALUE value){
        LogUtil.d("setCurrentStatus "+position);
        GPIO_Status gpio_status = gpioStatusList.get(position);
        gpio_status.setValue(value);
        gpioStatusList.set(position,gpio_status);
    }




    public static class MyViewHolder extends RecyclerView.ViewHolder{

        AppCompatCheckBox cbEnable;
        TextView tvLabel;
        AppCompatRadioButton rbIn;
        AppCompatRadioButton rbOut;
        AppCompatCheckBox cbVal;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cbEnable=itemView.findViewById(R.id.cbEnable);
            tvLabel=itemView.findViewById(R.id.tvLabel);
            rbIn=itemView.findViewById(R.id.rbIn);
            rbOut=itemView.findViewById(R.id.rbOut);
            cbVal=itemView.findViewById(R.id.cbVal);
        }

    }

    private void showToast(String message){
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }

    private String getCH9104GPIOLabel(int gpioIndex){
        int group = gpioIndex / 6;
        int index=gpioIndex % 6;
        return String.format(Locale.getDefault(),"GPIO%d%d",group,index);
    }
}
