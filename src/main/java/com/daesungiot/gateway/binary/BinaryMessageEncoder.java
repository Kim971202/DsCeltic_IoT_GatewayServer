package com.daesungiot.gateway.binary;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public abstract class BinaryMessageEncoder extends MessageToByteEncoder<BinaryMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BinaryMessage msg, ByteBuf out) throws Exception {
        System.out.println("BinaryMessageEncoder -> BinaryMessageEncoder CALLED");
        writeHeader(ctx, msg, out);
        writeBody(ctx, msg, out);
    }

    abstract protected void writeHeader(ChannelHandlerContext ctx, BinaryMessage msg, ByteBuf out);

    abstract protected void writeBody(ChannelHandlerContext ctx, BinaryMessage msg, ByteBuf out);

}
