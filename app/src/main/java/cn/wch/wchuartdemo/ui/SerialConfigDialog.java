package cn.wch.wchuartdemo.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import cn.wch.wchuartdemo.LogUtil;
import cn.wch.wchuartdemo.R;
import cn.wch.wchuartdemo.WCHApplication;
import cn.wch.wchuartdemo.entity.SerialBaudBean;

public class SerialConfigDialog extends DialogFragment {

    private SerialBaudBean bean;

    private Button confirm;
    private Button cancel;

    private Spinner baud;
    private Spinner data;
    private Spinner stop;
    private Spinner parity;
    private SwitchCompat switchCompat;

    private onClickListener listener;

    private Handler handler=new Handler(Looper.getMainLooper());

    public static SerialConfigDialog newInstance(SerialBaudBean bean) {
        Bundle args = new Bundle();
        SerialConfigDialog fragment = new SerialConfigDialog(bean);
        fragment.setArguments(args);
        return fragment;
    }

    public SerialConfigDialog(SerialBaudBean bean) {
        this.bean = bean;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        //必须设置dialog的window背景为透明颜色，不然圆角无效或者是系统默认的颜色
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View view = inflater.inflate(R.layout.dialog_serial, null);
        init(view);
        return view;
    }

    private void init(View view) {
        confirm=view.findViewById(R.id.set);
        cancel=view.findViewById(R.id.cancel);
        switchCompat=view.findViewById(R.id.scFLow);
        initSpinner(view);
        if(bean!=null){
            loadParameter(bean);
        }

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismiss();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener!=null){
                    listener.onSetBaud(getBaudData());
                }
                dismiss();
            }
        });

    }
    public void setListener(@NonNull onClickListener listener){
        this.listener=listener;

    }

    public interface onClickListener{
        void onSetBaud(SerialBaudBean data);
    }

    private void initSpinner(View view){
        baud=view.findViewById(R.id.spinner_baud);
        ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter
                .createFromResource(WCHApplication.getContext(), R.array.baud,
                        R.layout.my_spinner_textview);
        baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        baud.setAdapter(baudAdapter);
        baud.setGravity(0x10);
        baud.setSelection(22);

        data=view.findViewById(R.id.spinner_data);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter
                .createFromResource(WCHApplication.getContext(), R.array.data,
                        R.layout.my_spinner_textview);
        baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        data.setAdapter(dataAdapter);
        data.setGravity(0x10);
        data.setSelection(3);

        stop=view.findViewById(R.id.spinner_stop);
        ArrayAdapter<CharSequence> stopAdapter = ArrayAdapter
                .createFromResource(WCHApplication.getContext(), R.array.stop,
                        R.layout.my_spinner_textview);
        baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        stop.setAdapter(stopAdapter);
        stop.setGravity(0x10);
        stop.setSelection(0);

        parity=view.findViewById(R.id.spinner_parity);
        ArrayAdapter<CharSequence> parityAdapter = ArrayAdapter
                .createFromResource(WCHApplication.getContext(), R.array.parity,
                        R.layout.my_spinner_textview);
        baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        parity.setAdapter(parityAdapter);
        parity.setGravity(0x10);
        parity.setSelection(0);
    }


    /**
     * 加载Dialog状态
     * @param bean
     */
    private void loadParameter(SerialBaudBean bean){
        if(bean==null){
            LogUtil.d("serialBaudBean is null");
            return;
        }
        baud.setSelection(getIndexFromStringArray(WCHApplication.getContext(),R.array.baud,Integer.toString(bean.getBaud())));
        data.setSelection(getIndexFromStringArray(WCHApplication.getContext(),R.array.data,Integer.toString(bean.getData())));
        stop.setSelection(getIndexFromStringArray(WCHApplication.getContext(),R.array.stop,Integer.toString(bean.getStop())));
        String parityString="";
        switch (bean.getParity()){
            case 0:
                parityString="无";
                break;
            case 1:
                parityString="奇校验";
                break;
            case 2:
                parityString="偶校验";
                break;
            case 3:
                parityString="标志位";
                break;
            case 4:
                parityString="空白位";
                break;
            default:
                parityString="无";
                break;
        }
        parity.setSelection(getIndexFromStringArray(WCHApplication.getContext(),R.array.parity,parityString));
        switchCompat.setChecked(bean.isFlow());
    }

    private int getIndexFromStringArray(Context context, int arrayId, String source){

        String[] stringArray = context.getResources().getStringArray(arrayId);
        for (int i = 0; i < stringArray.length; i++) {
            if(source.equals(stringArray[i])){
                LogUtil.d("初始化: "+source+" 位置："+i);
                return i;
            }
        }
        return 0;
    }

    /**
     * 获取波特率详细信息，用来蓝牙通信
     * @return
     */
    private SerialBaudBean getBaudData(){
        SerialBaudBean bean=new SerialBaudBean();
        bean.setBaud(Integer.parseInt(baud.getSelectedItem().toString()));
        bean.setData(Integer.parseInt(data.getSelectedItem().toString()));
        bean.setStop(Integer.parseInt(stop.getSelectedItem().toString()));
        String p=parity.getSelectedItem().toString();
        int i=0;
        switch (p){
            case "无":
                i=0;
                break;
            case "奇校验":
                i=1;
                break;
            case "偶校验":
                i=2;
                break;
            case "标志位":
                i=3;
                break;
            case "空白位":
                i=4;
                break;
            default:
                i=0;
                break;
        }
        bean.setParity(i);
        bean.setFlow(switchCompat.isChecked());
        return bean;
    }

}
