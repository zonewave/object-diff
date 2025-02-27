package io.github.zonewave.objectdiff.samples;


public class Demo {

    private int intVal;
    private String strVal;
    private DemoInner inner;
    public String pStrVal;

    private boolean isBool;
    private boolean exist;
    private boolean is;

    // construct

    public Demo(int intVal, String strVal, DemoInner inner, String pStrVal, boolean isBool, boolean exist, boolean is) {
        this.intVal = intVal;
        this.strVal = strVal;
        this.inner = inner;
        this.pStrVal = pStrVal;
        this.isBool = isBool;
        this.exist = exist;
        this.is = is;
    }

    // generate


    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public String getStrVal() {
        return strVal;
    }

    public void setStrVal(String strVal) {
        this.strVal = strVal;
    }

    public DemoInner getInner() {
        return inner;
    }

    public void setInner(DemoInner inner) {
        this.inner = inner;
    }

    public String getpStrVal() {
        return pStrVal;
    }

    public void setpStrVal(String pStrVal) {
        this.pStrVal = pStrVal;
    }

    public boolean isBool() {
        return isBool;
    }

    public void setBool(boolean bool) {
        isBool = bool;
    }

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public boolean isIs() {
        return is;
    }

    public void setIs(boolean is) {
        this.is = is;
    }

}
