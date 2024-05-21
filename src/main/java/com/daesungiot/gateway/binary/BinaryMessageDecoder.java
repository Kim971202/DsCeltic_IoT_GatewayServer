package com.daesungiot.gateway.binary;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

public abstract class BinaryMessageDecoder extends ByteToMessageDecoder {
    private BinaryMessage msg;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf inBuf, List<Object> out){
        System.out.println("BinaryMessageDecoder -> decode CALLED");

        if(msg == null) {
            if(inBuf.readableBytes() < getHeaderSize()){
                return;
            }
            msg = parseHeader(inBuf);
            if(msg == null){
                inBuf.readBytes(inBuf.readableBytes());
                return;
            }
        }

        if(inBuf.readableBytes() < msg.getBodyLength())
            return;

        msg = parseBody(inBuf, msg);

        out.add(msg);
        msg = null;
    }

    /**
     * 메시지 헤더를 파싱하는 루틴 구현하고 리턴 값으로 BinaryMessage 인스터스를 반환한다.
     * 바디 크기를 셋팅하여야 한다.
     * @return BinaryMessage
     */
    abstract protected BinaryMessage parseHeader(ByteBuf inBuf);

    /**
     * 메시지 바디를 파싱하는 루틴 구현하고 리턴 값으로 바이너리메시지 인스턴스를 반환한다.
     * @return BinaryMessage
     */
    abstract protected BinaryMessage parseBody(ByteBuf inBuf, BinaryMessage msg);

    /**
     * 메시지의 헤더 크기를 리턴
     * @return int
     */
    abstract protected short getHeaderSize();
}
