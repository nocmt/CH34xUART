package cn.wch.wchuartdemo.entity;

public class ModemEntity {

    public int serialNumber;
    public boolean DCD;
    public boolean DSR;
    public boolean CTS;
    public boolean RING;

    public ModemEntity(int serialNumber, boolean DCD, boolean DSR, boolean CTS, boolean RING) {
        this.serialNumber = serialNumber;
        this.DCD = DCD;
        this.DSR = DSR;
        this.CTS = CTS;
        this.RING = RING;
    }
}
