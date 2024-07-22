package com.daesungiot.gateway.daesung;

import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import com.daesungiot.gateway.service.InteractionRequest;
import com.daesungiot.gateway.service.MccResponse;
import com.daesungiot.gateway.util.Common;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;

@Component(value = "cmd0102")
public class Cmd0102Handler implements ResponseHandler {

    private static final String[] containers = {"rtSt", "mfSt", "erAl", "opIf", "dvAuth", "dvDel", "rqSt"};

//    private static final String[] mgmtCmds = {"powr", "opMd", "htTp", "wtTp", "hwTp", "ftMd", "24h", "12h", "7wk", "fwh", "sfck", "reSt", "mfAr", "fcnt", "blCf", "rsPw", "sdCd", "fcLc", "slSt", "rqSt", "rsSl"};

    private static final String[] cotrollerModelCd = {"ESCeco13S", "ESCeco20S", "DCR-91/WF", "DCR-27/WF"};

    @Autowired
    private final InteractionRequest mccRequest;
    @Autowired
    private final RemoteHandler rcHandler;

    public Cmd0102Handler(InteractionRequest mccRequest, RemoteHandler rcHandler) {
        this.mccRequest = mccRequest;
        this.rcHandler = rcHandler;
    }

    @Override
    public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg) {
        System.out.println("Cmd0102Handler -> handle CALLED");

        RemoteMessage req = (RemoteMessage) msg;
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(req.toString());
        System.out.println(Arrays.toString(req.getBody()));
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
        int min = (int) (Math.random() * 100) % 50 + 10;
        String prId = null;
        String hdModelCd = "";
        String srNo = null;
        String modelCode = null;
        String rKey = null;
        String tKey = null;
        String modelCodeKey = null;
        String serialKey = null;

        try {
            HashMap<String, String> jsonObject = null;
            try {
                jsonObject = new Gson().fromJson(new StringReader(new String(req.getBody())), HashMap.class);

                srNo = Common.getValueFromMsg(msg, "srNo");
                modelCode = Common.getValueFromMsg(msg, "biMd");
                rKey = Common.getValueFromMsg(msg, "rKey");
                tKey = Common.getValueFromMsg(msg, "tKey");

                Common.setSrNoMap(serialKey, srNo);
                Common.setModelCodeMap(modelCodeKey, modelCode);

            } catch (Exception e1) {
                System.out.println("CMD102 NOT valid json to map" + e1);
            }

            try {
                prId = jsonObject.get("prId");
            } catch (Exception e) {

            }

            String serialHexStr = req.getSerialNumber();
            byte[] bytes = new BigInteger(serialHexStr, 16).toByteArray();
            String hdSerialNo = new String(bytes);
//			if(!srNo.equals(hdSerialNo)) {
//				LOGGER.info("The Serial No mismatched ["+hdSerialNo+","+srNo+"]" );
//				return RemoteHandler.makeResponse(req, "{\"rtCd\":\"400\",\"rpTm\":\"00:"+min+"\"}");
//			}

            String modelCdHexStr = req.getModelCode();
            byte[] modelBytes = new BigInteger(modelCdHexStr, 16).toByteArray();
            hdModelCd = new String(modelBytes);
            //System.out.println("Device Model Code : "+hdModelCd);

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("{\"rtCd\":\"400\",\"rpTm\":\"00:" + min + "\"}" + "1");
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"400\",\"rpTm\":\"00:" + min + "\"}");
        }

        try {
            if (dKey == null && tKey != null && !tKey.equals("") ) {
                MccResponse resp;

                if (prId != null && !prId.equals("")) {
                    //각방온도조절기인 경우 제어기 아이디를 게이트 아이디로 셋팅
//                    System.out.println("ROOM Control ID : " + cseid);
//                    resp = mccRequest.cseRegistration(cseid, prId);
                    resp = mccRequest.eachRcCreateAE(cseid, srNo, modelCode, rKey, tKey, prId);

                } else {
                    //  resp = mccRequest.cseRegistration(cseid);
                    // TODO: 보일러, 각방, 환기 기기 기준으로 AE 생성해야함
                    resp = mccRequest.createAE(cseid, srNo, modelCode, rKey, tKey);
                }
                dKey = resp.getDKey();

                if ((resp.getResponseCode() > 299 && resp.getResponseCode() < 200) && dKey == null) {
                    if (resp.getResponseCode() == 401) {
                        System.out.println("## 401 error  || legacy Dkey : " + dKey);
                        if (prId != null && !prId.equals("")) {
                            resp = mccRequest.eachRcCreateAE(cseid, srNo, modelCode, rKey, tKey, prId);
                        } else {
                            resp = mccRequest.createAE(cseid, srNo, modelCode, rKey, tKey);
                        }
                        dKey = resp.getDKey();

                        System.out.println("## 401401401401401401401 error  || new Dkey : " + dKey);
                    }
                    System.out.println("{\"rtCd\":\"401\",\"rpTm\":\"00:" + min + "\"}" + "2");
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"401\",\"rpTm\":\"00:" + min + "\"}");
                }

                int endIdx = cseid.lastIndexOf('.');
                String aeName = cseid.substring(endIdx + 1);

                MccResponse resp1 = mccRequest.createContainer(srNo, containers, aeName);
                if (resp1.getResponseCode() > 299 && resp1.getResponseCode() != 403){
                    System.out.println("{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}" + "3");
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}");
                }


                MccResponse resp2 = mccRequest.createSubscription(aeName, srNo, rKey);
                if (resp2.getResponseCode() > 299 && resp2.getResponseCode() != 403){
                    System.out.println("{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}" + "4");
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}");
                }

                if (dKey != null)
                    rcHandler.putDKey(cseid, dKey);
            }


            String msgBody = new String(req.getBody());
            int lastBraceIndex = msgBody.lastIndexOf('}');
            if (lastBraceIndex > 0) {
                String ipAddr = "";
                try {
                    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    InetAddress inetAddress = socketAddress.getAddress();
                    ipAddr = inetAddress.getHostAddress();
                } catch (Exception e) {
                }
                String body = msgBody.substring(0, lastBraceIndex) + ",\"gwId\":\"" + mccRequest.getFrom() + "\"" + ",\"dvIp\":\"" + ipAddr + "\"}";

                int endIdx = cseid.lastIndexOf('.');
                String aeName = cseid.substring(endIdx + 1);
//                MccResponse resp = mccRequest.createContentInstance(aeName, srNo, body);
                MccResponse resp = mccRequest.createContentInstance(aeName, srNo, new String(req.getBody()), cseid);
                if (resp.getResponseCode() < 300 && resp.getResponseCode() > 199) {
                    boolean boilerRcYN = false;
                    for (String modelCd : cotrollerModelCd) {
                        if (hdModelCd.contains(modelCd)) {
                            boilerRcYN = true;
                            //System.out.println("R/C Boliler Return");
                            break;
                        }
                    }
                    if (boilerRcYN) {
                        //기존 보일러 버전인 경우와 아닌 경우 구분 해서 반환
                        System.out.println("{\"rtCd\":\"200\",\"rpTm\":\"00:" + min + "\"}" + "5");
                        return RemoteHandler.makeResponse(req, "{\"rtCd\":\"200\",\"rpTm\":\"00:" + min + "\"}");
                    } else {
                        System.out.println("{\"rtCd\":\"200\",\"rpTm\":\"00:" + min + "\",\"dvId\":\"" + cseid + "\"}" + "6");
                        return RemoteHandler.makeResponse(req, "{\"rtCd\":\"200\",\"rpTm\":\"00:" + min + "\",\"dvId\":\"" + cseid + "\"}");
                    }
                } else {
                    System.out.println("{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}" + "7");
                    return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}");
                }
            } else {
                System.out.println("{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}" + "8");
                return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}");
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("{\"rtCd\":\"500\",\"rpTm\":\"00:" + min + "\"}" + "9");
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\",\"rpTm\":\"14:" + "29" + "\"}");
        }
    }
}
