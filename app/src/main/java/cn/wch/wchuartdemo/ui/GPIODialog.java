package cn.wch.wchuartdemo.ui;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Locale;

import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.base.gpio.GPIO_Status;
import cn.wch.uartlib.base.gpio.GPIO_VALUE;
import cn.wch.uartlib.chip.type.ChipType2;
import cn.wch.wchuartdemo.LogUtil;
import cn.wch.wchuartdemo.databinding.DialogGpioBinding;

public class GPIODialog extends DialogFragment {

    DialogGpioBinding binding;
    UsbDevice device;
    GPIOAdapter adapter;

    public static GPIODialog newInstance(UsbDevice device) {

        Bundle args = new Bundle();

        GPIODialog fragment = new GPIODialog(device);
        fragment.setArguments(args);
        return fragment;
    }

    public GPIODialog(UsbDevice device) {
        this.device = device;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogGpioBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }


    private void init() {

        adapter=new GPIOAdapter(getActivity(),device);

        binding.rvGPIO.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false));
        binding.rvGPIO.setAdapter(adapter);

        binding.tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        binding.tvGetAllGPIO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGPIOStatus();
            }
        });
        binding.tvConfigGPIO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configGPIO();
            }
        });

        binding.tvSetGPIOVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGPIOVal();
            }
        });
        binding.tvGetGPIOVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGPIOVal();
            }
        });

        //初始化界面

        try {
            boolean supportGPIOFeature = WCHUARTManager.getInstance().isSupportGPIOFeature(device);
            if(!supportGPIOFeature){
                showToast("目前不支持该设备的GPIO相关的配置");
                return;
            }
        } catch (Exception e) {
            showToast("1--"+e.getMessage());
            return;
        }

        try {
            int i = WCHUARTManager.getInstance().queryGPIOCount(device);
            showToast(String.format(Locale.getDefault(),"一共有%d个GPIO",i));
            ChipType2 chipType = WCHUARTManager.getInstance().getChipType(device);
            if(chipType!=null){
                String des=String.format(Locale.getDefault(),"设备型号:%s,支持%d个GPIO",chipType.getDescription(),i);
                binding.tvDes.setText(des);
            }

        } catch (Exception e) {
            showToast("2--"+e.getMessage());
            return;
        }

        getGPIOStatus();
    }
    //获取所有GPIO状态
    private void getGPIOStatus(){
        try {
            List<GPIO_Status> statuses = WCHUARTManager.getInstance().queryAllGPIOStatus(device);
            adapter.update(statuses);
            LogUtil.d("get GPIO status "+statuses.toString());
            showToast("获取成功");
        } catch (Exception e) {
            e.printStackTrace();
            showToast(e.getMessage());
        }
    }

    //配置GPIO
    private void configGPIO(){
        int childCount = binding.rvGPIO.getLayoutManager().getItemCount();
        LogUtil.d("item count "+childCount);
        if(childCount==0){
            return;
        }
        for (int i = 0; i < childCount; i++) {

            List<GPIO_Status> currentStatus = adapter.getCurrentStatus();
            GPIO_Status gpio_status = currentStatus.get(i);
            try {
                WCHUARTManager.getInstance().enableGPIO(device,gpio_status.getGpioIndex(),gpio_status.isEnabled(),gpio_status.getDir());
            } catch (Exception e) {
                showToast(e.getMessage());
                return;
            }


        }
        showToast("配置成功");
    }


    //设置电平
    private void setGPIOVal(){
        int childCount = binding.rvGPIO.getLayoutManager().getItemCount();
        if(childCount==0){
            return;
        }
        for (int i = 0; i < childCount; i++) {
            List<GPIO_Status> currentStatus = adapter.getCurrentStatus();
            GPIO_Status gpio_status = currentStatus.get(i);
            try {

                WCHUARTManager.getInstance().setGPIOVal(device,gpio_status.getGpioIndex(),gpio_status.getValue());
            } catch (Exception e) {
                LogUtil.d(e.getMessage());
            }
        }
        showToast("设置电平结束");
    }

    //获取电平
    private void getGPIOVal(){
        int childCount = binding.rvGPIO.getLayoutManager().getItemCount();
        if(childCount==0){
            return;
        }
        List<GPIO_Status> currentStatus = adapter.getCurrentStatus();
        for (int i = 0; i < currentStatus.size(); i++) {
            //cbVal
            try {
                GPIO_VALUE gpioVal = WCHUARTManager.getInstance().getGPIOVal(device, currentStatus.get(i).getGpioIndex());
                GPIO_Status gpio_status = currentStatus.get(i);
                gpio_status.setValue(gpioVal);
                currentStatus.set(i,gpio_status);
                adapter.update(currentStatus);
            } catch (Exception e) {
                showToast(e.getMessage());
                return;
            }

        }
        showToast("获取电平结束");

    }

    private void showToast(String message){
        Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
    }





}
