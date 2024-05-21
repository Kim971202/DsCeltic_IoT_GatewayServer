package com.daesungiot.gateway.binary;

import io.netty.channel.ChannelHandlerContext;

public interface ResponseHandler {
    public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg);

}
