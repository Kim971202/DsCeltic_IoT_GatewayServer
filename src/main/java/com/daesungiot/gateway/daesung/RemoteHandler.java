package com.daesungiot.gateway.daesung;

import com.daesungiot.gateway.binary.BinaryHandler;
import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import io.netty.channel.ChannelHandler.Sharable;


import java.util.HashMap;

// @Sharable handler 오류 해결 ; 어노테이션 추가 2024-04-17  참조 블로그 : https://velog.io/@joosing/take-a-look-netty
@Sharable
@Slf4j
@Configuration
@ConfigurationProperties("my-settings")
@Component(value = "handler")
public class RemoteHandler extends BinaryHandler implements ApplicationContextAware{

        private static final Logger LOGGER = LoggerFactory.getLogger(RemoteHandler.class);
        public static final  String OidPrefix = "0.2.481.1.1.";
        private final static byte protocolVersion = 0x10;
        private static HashMap<String, Channel> rcChannels = new HashMap<String, Channel>();
        private static HashMap<String, String> dKeys = new HashMap<String, String>();
        private String mCseid, ipAddr;

        private HashMap<String, String> cmdHandlers;
        private ApplicationContext context;


        @Override
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            System.out.println("RemoteHandler -> setApplicationContext CALLED");
            context = ctx;
        }

        public void setCmdHandlers(HashMap<String, String> cmdHandlers) {
            System.out.println("RemoteHandler -> setCmdHandlers CALLED");
            this.cmdHandlers = cmdHandlers;
        }

        @Override
        protected BinaryMessage messageReceived(ChannelHandlerContext ctx, BinaryMessage msg) {
            System.out.println("RemoteHandler -> messageRecieved CALLED");
            RemoteMessage req = (RemoteMessage) msg;
            System.out.println("msg :" + req);
            mCseid = OidPrefix+req.getModelCode()+"."+req.getSerialNumber();
            System.out.println("cmdHandlers: " + cmdHandlers);
            try {
                Channel legacyChannel = rcChannels.get(mCseid);
                if(legacyChannel != null){
                    if(legacyChannel != ctx.channel()){
                        LOGGER.debug("Close legacy CSEID channel : " + mCseid);
                        System.out.println("Close legacy CSEID channel : " + mCseid);
                        legacyChannel.close();
                    }
                }
            } catch (Exception e1) {
                System.out.println(e1);
            }
            rcChannels.put(mCseid, ctx.channel());
            String dKey = (String)dKeys.get(mCseid);

//            int endMcseid = mCseid.lastIndexOf('.');
//            String newDkey = mCseid.substring(endMcseid + 1);
//            String dKey = newDkey;

            String cmdCodeStr = Integer.toHexString(req.getCmdCode());

            try {
                String handlerStr = cmdHandlers.get(cmdCodeStr);
                System.out.println("handlerStr: " + handlerStr);
                if(handlerStr != null) {
                    ResponseHandler resHandler = (ResponseHandler)context.getBean(handlerStr);
                    System.out.println("msgmsg2222222222222222:" + msg);
                    return resHandler.handle(ctx, mCseid, dKey, msg);

                } else {
                    String errorCode = "{\"rtCd\":\"404\"}";
                    return makeResponse(req, errorCode);
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            String errorCode = "{\"rtCd\":\"500\"}";
            RemoteMessage resp = makeResponse(req, errorCode);

            return resp;
        }

        public String getIpAddr() {
            System.out.println("RemoteHandler -> getIpAddr CALLED");
            return ipAddr;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("RemoteHandler -> channelActive CALLED");

            ipAddr = ctx.channel().remoteAddress().toString();
            System.out.println("R/C IP addr : " + ipAddr);
            super.channelActive(ctx);
        }

        public static RemoteMessage makeResponse(RemoteMessage req, String bodyStr) {
            System.out.println("RemoteHandler -> makeResponse CALLED");
            RemoteMessage resp = new RemoteMessage();

            resp.setProtocolVersion(req.getProtocolVersion());
            resp.setCmdCode((short)(req.getCmdCode()+1));
            resp.setModelCode(req.getModelCode());
            resp.setSerialNumber(req.getSerialNumber());
            byte[] body = bodyStr.getBytes();
            resp.setBodyLength((short)body.length);
            resp.setBody(body);
            resp.setLength((short)(31+body.length));
            return resp;
        }

        public static RemoteMessage makeRequest(String cseid, short cmd, String bodyStr) {
            System.out.println("RemoteHandler -> makeRequest CALLED");
            String modelCode = "0";
            String serialNumber = "1";

            int serialSIdx = cseid.lastIndexOf('.');
            if(serialSIdx > 0) {
                int modelSIdx = cseid.lastIndexOf('.', serialSIdx-1);
                if(modelSIdx > 0) {
                    modelCode = cseid.substring(modelSIdx+1, serialSIdx);
                }
                serialNumber = cseid.substring(serialSIdx+1);
            }

            RemoteMessage req = new RemoteMessage();
            req.setProtocolVersion(protocolVersion);
            req.setCmdCode((short)(cmd));
            req.setModelCode(modelCode);
            req.setSerialNumber(serialNumber);
            byte[] body = bodyStr.getBytes();
            req.setBodyLength((short)body.length);
            req.setBody(body);
            req.setLength((short)(31+body.length));
            return req;
        }

        public void putDKey(String cseid, String dKey) {
            System.out.println("RemoteHandler -> putDKey CALLED");
            dKeys.put(cseid, dKey);
        }

        public String getDKey(String cseid) {
            System.out.println("RemoteHandler -> getDKey CALLED");
            int endMcseid = cseid.lastIndexOf('.');
            String newDkey = cseid.substring(endMcseid + 1);
            return (String)dKeys.get(newDkey);
        }

        public static void removeCSEInfo(String cseid){
            System.out.println("RemoteHandler -> removeCSEInfo CALLED");
            rcChannels.remove(cseid);
            dKeys.remove(cseid);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.out.println("RemoteHandler -> exceptionCaught CALLED");
            System.out.println("disconnect");
            rcChannels.remove(mCseid);
            dKeys.remove(mCseid);
            //System.out.println("Connection Closed [exceptionCaught] : " + mCseid);
            super.exceptionCaught(ctx, cause);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            System.out.println("RemoteHandler -> channelUnregistered CALLED");
            //rcChannels.remove(mCseid);
            //dKeys.remove(mCseid);
            //System.out.println("Connection Closed [channelUnregistered] : " + mCseid);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("RemoteHandler -> channelInactive CALLED");
            //rcChannels.remove(mCseid);
            //dKeys.remove(mCseid);
            //System.out.println("Connection Closed [channelInactive] : " + mCseid);
            super.channelInactive(ctx);
        }

        public int sendMessage(String cseid, final RemoteMessage msg) {
            System.out.println("RemoteHandler -> sendMessage CALLED");
            Channel channel = rcChannels.get(cseid);
            if(channel == null) {
                System.out.println("\"Not exist CSEID : \" + cseid");
                return -1;
            }
            //System.out.println("######### control CSE  ID: "+ cseid);
            //System.out.println("######### control msg : "+ new Gson().toJson(msg));

            ChannelFuture future = channel.writeAndFlush(msg);
            future.addListener(new ChannelFutureListener(){
                public void operationComplete(ChannelFuture future){
                    System.out.println("RemoteHandler -> sendMessage -> operationComplete CALLED");
                    if(future.isSuccess()) {
                        //System.out.println("written message => " + msg);
                    }else {
                        Throwable cause = future.cause();
                    }
                }
            });

            return 1;
        }
    }
