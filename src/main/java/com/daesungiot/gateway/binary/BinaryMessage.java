package com.daesungiot.gateway.binary;

public abstract class BinaryMessage {

    public abstract short getBodyLength();
    public abstract short getHeaderLength();

    protected static String toHexString(byte buf[]){
        System.out.println("BinaryMessage -> toHexString CALLED");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            sb.append(Integer.toHexString(0x0100 + (buf[i] & 0x00FF)).substring(1));
        }
        return sb.toString();
    }

}
