package com.daesungiot.gateway.daesung;


import com.daesungiot.gateway.binary.BinaryMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RemoteMessage extends BinaryMessage {

    public final static short headerSize = 31;
    private byte protocolVersion;
    private short length;
    private short bodyLength;
    private short cmdCode;
    private String modelCode;
    private String serialNumber;
    private byte[] body = {0x00};

    @Override
    public short getHeaderLength() {
        return headerSize;
    }

    @Override
    public String toString() {
        return Integer.toHexString(0xFF&protocolVersion)+","+length+","+cmdCode+","+modelCode+","+serialNumber+","+new String(body);
    }

}
