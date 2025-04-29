package cn.wch.wchuartdemo.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import cn.wch.wchuartdemo.R;

public class ToastUtil {
    public static Toast create(@NonNull Context context,String message){
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER,0,0);
        View inflate = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
        TextView textView = inflate.findViewById(R.id.text);
        textView.setText(message);
        toast.setView(inflate);
        toast.setDuration(Toast.LENGTH_SHORT);
        return toast;
    }
}
