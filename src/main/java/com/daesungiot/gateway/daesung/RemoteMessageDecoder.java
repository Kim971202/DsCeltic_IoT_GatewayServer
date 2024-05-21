package com.daesungiot.gateway.daesung;


import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.BinaryMessageDecoder;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component(value = "decoder")
public class RemoteMessageDecoder extends BinaryMessageDecoder {
    private final static byte PROTOCOL_VER = 0x10;

    Logger logger = LoggerFactory.getLogger(RemoteMessageDecoder.class);

    @Override
    protected RemoteMessage parseHeader(ByteBuf inBuf) {
        System.out.println("RemoteMessageDecoder -> parseHeader CALLED");
        RemoteMessage bmsg = new RemoteMessage();

        byte protoclVersion = inBuf.readByte();
        if(protoclVersion != PROTOCOL_VER){
            //System.out.println("입력 Protocl 오류 : " + protoclVersion);
            return null;
        }

        bmsg.setProtocolVersion(protoclVersion);
        bmsg.setLength(inBuf.readShort());
        bmsg.setCmdCode(inBuf.readShort());

        byte[] modelBytes = new byte[10];
        inBuf.readBytes(modelBytes, 0, modelBytes.length);
        bmsg.setModelCode(new java.math.BigInteger(modelBytes).toString(16));

        byte[] serialBytes = new byte[16];
        inBuf.readBytes(serialBytes, 0, serialBytes.length);
        bmsg.setSerialNumber(new java.math.BigInteger(serialBytes).toString(16));

        bmsg.setBodyLength((short)(bmsg.getLength() - getHeaderSize()));

        return bmsg;
    }

    @Override
    protected BinaryMessage parseBody(ByteBuf inBuf, BinaryMessage msg) {
        System.out.println("RemoteMessageDecoder -> parseBody CALLED");
        System.out.println("RemoteMessageDecoder msg: " + msg);
        RemoteMessage dMsg = (RemoteMessage)msg;
        byte[] body = new byte[dMsg.getBodyLength()];
        inBuf.readBytes(body, 0, body.length);
        dMsg.setBody(body);
        return dMsg;
    }

    @Override
    protected short getHeaderSize() {
        System.out.println("RemoteMessageDecoder -> getHeaderSize CALLED");
        System.out.println("RemoteMessage.headerSize: " + RemoteMessage.headerSize);
        return RemoteMessage.headerSize;
    }
}
