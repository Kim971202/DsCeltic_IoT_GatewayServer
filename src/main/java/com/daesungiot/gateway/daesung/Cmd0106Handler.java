package com.daesungiot.gateway.daesung;

import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import com.daesungiot.gateway.service.InteractionRequest;
import com.daesungiot.gateway.service.MccResponse;
import com.daesungiot.gateway.util.Common;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Getter
@Setter
@Component(value = "cmd0106")
public class Cmd0106Handler implements ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cmd0106Handler.class);

    @Autowired
    private InteractionRequest mccRequest;

    @Autowired
    private Cmd0102Handler rcHandler;

    @Autowired
    Common common;

    @Override
    public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg) {
        System.out.println("Cmd0106Handler -> handle CALLED");
        RemoteMessage req = (RemoteMessage) msg;
        System.out.println("reqreqreq : " + req);
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(req.toString());
        System.out.println(Arrays.toString(req.getBody()));
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(cseid);

        System.out.println("msgmsgmsgmsgmsgmsgmsgmsgmsgmsgmsg : " + msg);

        String srNo;
        try {
            srNo = common.hexaToText(cseid);
            System.out.println("srNo: " + srNo);
        } catch (Exception e) {
            System.out.println("CMD106 NOT valid json to map" + e);
            LOGGER.error(e.getMessage(), e);
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"400\"}");
        }

        try {
            if (dKey == null) {
                int endIdx = cseid.lastIndexOf('.');
                dKey = cseid.substring(endIdx + 1);

                if (dKey.isEmpty()) {
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"401\"}");
                }
            }
            String containerName = null;
            switch (req.getCmdCode()) {
                case 0x0106: //주기상태보고
                    containerName = "rtSt";
                    break;
                case 0x0108: //변경실시간상태
                    containerName = "mfSt";
                    break;
                case 0x0222: //에러
                    containerName = "erAl";
                    break;
                case 0x0224: // 가동시간
                    containerName = "opIf";
                    break;
                case 0x0308: // 상태 정보 요청 전달
                    containerName = "rqSt";
                    break;
            }
            LOGGER.debug("################## get Report Code ( containerName ): " + containerName);

            if (containerName != null) {
                int endIdx = cseid.lastIndexOf('.');
                String aeName = cseid.substring(endIdx + 1);
                MccResponse resp = mccRequest.createContentInstance(aeName, srNo, new String(req.getBody()), containerName, cseid);
                if (resp.getResponseCode() < 300 && resp.getResponseCode() > 199)
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"200\"}");
                else
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"402\"}");
            } else {
                return RemoteHandler.makeResponse(req, "{\"rtCd\":\"400\"}");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\"}");
        }
    }

}
