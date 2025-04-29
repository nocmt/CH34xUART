package cn.wch.wchuartdemo.ui;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cn.wch.wchuartdemo.R;
import cn.wch.wchuartdemo.adapter.UsbDeviceDialogAdapter;

public class DeviceListDialog extends DialogFragment {

    private ArrayList<UsbDevice> usbDeviceList;
    private UsbDeviceDialogAdapter dialogAdapter;
    private RecyclerView recyclerView;
    private Button cancel;
    private OnClickListener onClickListener;

    public static DeviceListDialog newInstance(@NonNull ArrayList<UsbDevice> usbDeviceList) {
        Bundle args = new Bundle();
        DeviceListDialog fragment = new DeviceListDialog(usbDeviceList);
        fragment.setArguments(args);
        return fragment;
    }

    public DeviceListDialog(ArrayList<UsbDevice> usbDeviceList) {
        this.usbDeviceList=usbDeviceList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View inflate = inflater.inflate(R.layout.dialog_devicelist, null);
        init(inflate);
        return inflate;
    }

    void init(View v){
        recyclerView=v.findViewById(R.id.list);
        cancel=v.findViewById(R.id.cancel);
        //init recyclerView
        dialogAdapter=new UsbDeviceDialogAdapter(getContext(),usbDeviceList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));
        DividerItemDecoration dividerItemDecoration=new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(dialogAdapter);
        //监听设备选择
        dialogAdapter.setOnClickListener(new UsbDeviceDialogAdapter.OnClickListener() {
            @Override
            public void onClick(UsbDevice usbDevice) {
                if(onClickListener!=null){
                    onClickListener.onClick(usbDevice);
                }
                dismiss();
            }
        });
        cancel.setOnClickListener((view)->{
            dismiss();
        });


    }

    public void setOnClickListener(OnClickListener onClickListener){
        this.onClickListener=onClickListener;
    }

    public static interface OnClickListener{
        void onClick(UsbDevice usbDevice);
    }
}
