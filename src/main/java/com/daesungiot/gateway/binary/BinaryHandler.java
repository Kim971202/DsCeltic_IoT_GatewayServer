package com.daesungiot.gateway.binary;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public abstract class BinaryHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("BinaryHandler -> channelRead CALLED");
        BinaryMessage hMsg = null;

        LOGGER.debug("#Binary Handler R/C Data recieved:"+msg);
        log.info("#Binary Handler R/C Data recieved: "+msg);

        if (msg instanceof BinaryMessage) {
            hMsg = (BinaryMessage) msg;
            LOGGER.debug("recieved message => " + hMsg);
            System.out.println("recieved message => " + hMsg);
        }

        BinaryMessage rMsg = messageReceived(ctx, hMsg);
        if(rMsg != null) {
            final String rMsgStr = rMsg.toString();
            ChannelFuture future = ctx.writeAndFlush(rMsg);
            future.addListener(new ChannelFutureListener(){
                public void operationComplete(ChannelFuture future){
                    if(future.isSuccess()) {
                        LOGGER.debug("[Binary channelRead]written message => " + rMsgStr);
                    }else {
                        Throwable cause = future.cause();
                        LOGGER.error(cause.getMessage(), cause);
                    }
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        System.out.println("BinaryHandler -> exceptionCaught CALLED");
        LOGGER.error(cause.getMessage(), cause);
        ctx.close();
    }

    abstract protected BinaryMessage messageReceived(ChannelHandlerContext ctx, BinaryMessage msg);

}
