package com.daesungiot.gateway.controller;

import com.daesungiot.gateway.service.InteractionRequest;
import com.daesungiot.gateway.service.MccResponse;
import com.daesungiot.gateway.daesung.RemoteHandler;
import com.daesungiot.gateway.daesung.RemoteMessage;
import com.daesungiot.gateway.util.Common;
import com.daesungiot.gateway.util.JSON;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RestController
@Configuration
@ConfigurationProperties("my-settings")
public class RemoteController {

    @Autowired
    Common common;

    @Autowired
    InteractionRequest interactionReq;
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteController.class);
    private static HashMap<String, Short> mgmtCmd2CmdCode = new HashMap<String, Short>();
    private static Executor pool = Executors.newFixedThreadPool(10);

    public static HashMap<String, String> uuIdMap = new HashMap<String, String>();

    static {
        mgmtCmd2CmdCode.put("powr", (short) 0x200);
        mgmtCmd2CmdCode.put("opMd", (short) 0x202);
        mgmtCmd2CmdCode.put("htTp", (short) 0x204);
        mgmtCmd2CmdCode.put("wtTp", (short) 0x206);
        mgmtCmd2CmdCode.put("hwTp", (short) 0x208);
        mgmtCmd2CmdCode.put("ftMd", (short) 0x210);
        mgmtCmd2CmdCode.put("24h", (short) 0x214);
        mgmtCmd2CmdCode.put("12h", (short) 0x216);
        mgmtCmd2CmdCode.put("7wk", (short) 0x218);
        mgmtCmd2CmdCode.put("fwh", (short) 0x220);
        mgmtCmd2CmdCode.put("sfck", (short) 0x226);     // 자가진단
        mgmtCmd2CmdCode.put("reSt", (short) 0x228);     // 재시작(에러시)
        mgmtCmd2CmdCode.put("mfAr", (short) 0x230);     // 주소 변경
        mgmtCmd2CmdCode.put("fcnt", (short) 0x232);
        mgmtCmd2CmdCode.put("blCf", (short) 0x234);
        mgmtCmd2CmdCode.put("rsPw", (short) 0x300);     // 환기 꺼짐/켜짐 예약
        mgmtCmd2CmdCode.put("sdCd", (short) 0x302);     // 스피커 음량
        mgmtCmd2CmdCode.put("fcLc", (short) 0x304);
        mgmtCmd2CmdCode.put("slSt", (short) 0x306);     // 취침 모드 온도설정 (사용안함)
        mgmtCmd2CmdCode.put("rqSt", (short) 0x310);     // 예약 정보 전달
        mgmtCmd2CmdCode.put("rsSl", (short) 0x312);     // 환기 취침 예약
    }

    @Autowired
    private InteractionRequest mccRequest;

    @Autowired
    private RemoteHandler rcHandler;

    @PostMapping("/AppToGwServer")
    @ResponseBody
    public void controlDeviceCmd(@RequestBody String msgBody, HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("RemoteController -> controlDeviceCmd : " + msgBody);
        String conBody = common.readCon(msgBody, "con");
        System.out.println("Device Control Body Message : " + conBody);

//        mobiusService.callMobius();
        String cseid = common.readCon(msgBody, "deviceId"),
                cmd = common.readCon(msgBody, "functionId"),
                uuId = common.readCon(msgBody, "uuId"),
                ri = common.readCon(msgBody, "ri"),
                resourceId = ri.substring(2),
                rKey = common.readCon(msgBody, "rKey"),
                receiveSrNo = common.hexaToText(cseid);


        System.out.println("cseid : " + cseid + "\n" + "cmd : " + cmd + "\n" + "uuId : " + uuId + "\n" + "resourceId :" + resourceId + "\n" + "rKey :" + rKey);
        uuIdMap.put(cseid, uuId);
        System.out.println("uuIdMap: " + uuIdMap);
        try {
            short cmdCode = mgmtCmd2CmdCode.get(cmd);

            if (cmdCode < 0) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                sendResult(cseid, getDKey(cseid), cmd, uuId, resourceId, rKey, receiveSrNo, 1);
                return;
            }

            LOGGER.info("Mobius 로 부터 제어 요청 받음 : " + msgBody);


            String cmdBody = null;

            if (conBody != null) {
                if (cmdCode == (short) 0x200) {
                    String powr = common.readCon(msgBody, "powerStatus");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"powr\":\"" + powr + "\"}";
                } else if (cmdCode == (short) 0x202) {
                    String opMd = common.readCon(msgBody, "modeCode"),
                            slCd = common.readCon(msgBody, "sleepCode");
                    if (opMd.equals("06")) {
                        cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"opMd\":\"" + opMd + "\"," + "\"slCd\":\"" + slCd + "\"}";
                    } else {
                        cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"opMd\":\"" + opMd + "\"}";
                    }
                } else if (cmdCode == (short) 0x204) {
                    String htTp = common.readCon(msgBody, "temperature");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"htTp\":\"" + htTp + "\"}";
                } else if (cmdCode == (short) 0x206) {
                    String wtTp = common.readCon(msgBody, "temperature");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"wtTp\":\"" + wtTp + "\"}";
                } else if (cmdCode == (short) 0x208) {
                    String hwTp = common.readCon(msgBody, "temperature");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"hwTp\":\"" + hwTp + "\"}";
                } else if (cmdCode == (short) 0x210) {
                    String opMd = common.readCon(msgBody, "modeCode");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"opMd\":\"" + opMd + "\"}";
                } else if (cmdCode == (short) 0x214) {
                    String type24h = common.readCon(msgBody, "type24h");
                    String hours = common.readCon(msgBody, "hours");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"rsCf\": {\"24h\": {\"md\":\"" + type24h + "\"," + "\"hs\":" + hours + "}}}";
                } else if (cmdCode == (short) 0x216) {
                    String workPeriod = common.readCon(msgBody, "workPeriod");
                    String workTime = common.readCon(msgBody, "workTime");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"rsCf\": {\"12h\": {\"hr\":\"" + workPeriod + "\"," + "\"mn\":\"" + workTime + "\"}}}";
                } else if (cmdCode == (short) 0x218) {
                    String weekList = common.readCon(msgBody, "weekList");
                    cmdBody = "";
                    cmdBody += "{\"ri\":\"";
                    cmdBody += resourceId;
                    cmdBody += "\",";
                    cmdBody += "\"rsCf\": {\"7wk\":";
                    cmdBody += weekList;
                    cmdBody += "}}";
//                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"rsCf\": {\"7wk\":" + weekList + "}}";

                    System.out.println("cmdBody: " + cmdBody);
                    // 7wh 주간 예약
                } else if (cmdCode == (short) 0x220) {
                    String awakeList = common.readCon(msgBody, "awakeList");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"rsCf\": {\"fwh\":" + awakeList + "}}";

                } else if (cmdCode == (short) 0x230) {
                    // 주소 수정 요청 ( 위도 / 경도 )

                } else if (cmdCode == (short) 0x232) {
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"fcnt\":\"1\"}";
                } else if (cmdCode == (short) 0x234) {
                    String blCf = common.readCon(msgBody, "brightnessLevel");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"blCf\":\"" + blCf + "\"}";
                } else if (cmdCode == (short) 0x300) {
                    // 환기 꺼짐/켜짐 예약
                } else if (cmdCode == (short) 0x304) {
                    String fcLc = common.readCon(msgBody, "modeCode");
                    cmdBody = "{\"ri\":\"" + resourceId + "\"," + "\"fcLc\":\"" + fcLc + "\"}";
                } else if (cmdCode == (short) 0x310) {
                    // 예약 정보 전달
                } else if (cmdCode == (short) 0x312) {
                    // 환기 취침 예약 모드
                }
            } else {
                cmdBody = "{\"ri\":\"" + resourceId + "\"}";
            }

            RemoteMessage msg = RemoteHandler.makeRequest(cseid, cmdCode, cmdBody);
            LOGGER.info("r/c로 전달 제어 메세지  : " + new Gson().toJson(cmdBody));

            if (rcHandler.sendMessage(cseid, msg) < 0) {
                sendResult(cseid, getDKey(cseid), cmd, uuId, resourceId, rKey, receiveSrNo, 2);
                return;
            }
        } catch (
                Exception e) {
            sendResult(cseid, getDKey(cseid), cmd, uuId, resourceId, rKey, receiveSrNo, 2);
            LOGGER.error(e.getMessage(), e);
        }

    }

    private String getDKey(String cseid) {
        int endMcseid = cseid.lastIndexOf('.');
        String newDkey = cseid.substring(endMcseid + 1);
        String dKey = rcHandler.getDKey(newDkey);

        // TODO: dKey가 없을 때 재등록은 하지 못하므로 에러 처리 해야함
        if (dKey == null) {
            MccResponse resp;
            try {
                dKey = rcHandler.getDKey(newDkey);
                System.out.println("dKettttttttttttttttt" + dKey);
                rcHandler.putDKey(cseid, dKey);
                System.out.println("fdsafdsafdsafdsafdsafdsafdsafdsafdas" + cseid);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

        }

        return dKey;
    }

    private void sendResult(final String cseid, final String dKey, final String mgmtCmd, final String uuId, final String resourceId, final String rKey, final String receiveSrNo, final int errorCode) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // TODO: 통신 장애시 errorCode 발생시 CIN으로 결과 발송처리 해야함.
                try {
                    mccRequest.ContentInstanceResultPut(cseid, dKey, mgmtCmd, uuId, resourceId, rKey, receiveSrNo, errorCode);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        };
        pool.execute(r);
    }

}
