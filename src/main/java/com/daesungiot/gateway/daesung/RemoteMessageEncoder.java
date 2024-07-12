package com.daesungiot.gateway.daesung;

import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.BinaryMessageEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

@Sharable
@Component(value = "encoder")
public class RemoteMessageEncoder extends BinaryMessageEncoder {

    @Override
    protected void writeHeader(ChannelHandlerContext ctx, BinaryMessage bmsg, ByteBuf out) {
        System.out.println("RemoteMessageEncoder -> writeHeader CALLED");
        RemoteMessage msg = (RemoteMessage)bmsg;
        System.out.println("RemoteMessage msg: " + msg);

        out.writeByte(msg.getProtocolVersion());
        out.writeShort(msg.getLength());
        out.writeShort(msg.getCmdCode());

        byte[] modelBytes = new byte[10];
        if(msg.getModelCode() != null && msg.getModelCode().length() > 0) {
            byte[] mBytes = new java.math.BigInteger(msg.getModelCode(), 16).toByteArray();
            System.arraycopy(mBytes, 0, modelBytes, 10-mBytes.length, mBytes.length);
        }
        out.writeBytes(modelBytes);

        byte[] serialBytes = new byte[16];
        if(msg.getSerialNumber() != null && msg.getSerialNumber().length() > 0) {
            byte[] sBytes = new java.math.BigInteger(msg.getSerialNumber(), 16).toByteArray();
            System.arraycopy(sBytes, 0, serialBytes, 16-sBytes.length, sBytes.length);
        }
        out.writeBytes(serialBytes);
    }

    @Override
    protected void writeBody(ChannelHandlerContext ctx, BinaryMessage bmsg, ByteBuf out) {
        System.out.println("RemoteMessageEncoder -> writeBody CALLED");

        RemoteMessage msg = (RemoteMessage)bmsg;
        System.out.println("RemoteMessage msg: " + msg);
        out.writeBytes(msg.getBody());
    }
}
