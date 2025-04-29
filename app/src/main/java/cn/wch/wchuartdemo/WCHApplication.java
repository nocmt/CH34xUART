package cn.wch.wchuartdemo;

import android.app.Application;
import android.content.Context;

import cn.wch.uartlib.WCHUARTManager;
import cn.wch.uartlib.chip.type.ChipType2;

public class WCHApplication extends Application {
    private static Application application;
    @Override
    public void onCreate() {
        super.onCreate();
        application=this;
        WCHUARTManager.getInstance().init(this);
        //WCHUARTManager.setReadTimeout(0);
        //WCHUARTManager.addNewHardware(0x1a86,0x7523);
        WCHUARTManager.setDebug(true);
        //增加0x1a86:0x55D4 并且强制指定类型为CH9102X
//        WCHUARTManager.addNewHardwareAndChipType(0x1a86,0x55D3, ChipType2.CHIP_CH343GP);
        //解决GPIO不识别问题
        WCHUARTManager.addNewHardwareAndChipType(0x1a86,0x55D8, ChipType2.CHIP_CH9101UH);
//        WCHUARTManager.addNewHardwareAndChipType(0x1a86,0x55D4,ChipType2.CHIP_CH9102F);
    }

    public static Context getContext(){
        return application;
    }


}
