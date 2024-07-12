package com.daesungiot.gateway.daesung;

import com.google.gson.Gson;
import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import com.daesungiot.gateway.service.InteractionRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

@Getter
@Setter
@Component(value = "cmd0104")
@Slf4j
public class Cmd0104Handler implements ResponseHandler {

    @Autowired
    private InteractionRequest mccRequest;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    @Override
    public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg) {
        System.out.println("Cmd0104Handler -> handle CALLED");
        RemoteMessage req = (RemoteMessage) msg;
        Calendar cal = Calendar.getInstance();
        String wk = String.valueOf(cal.get(Calendar.DAY_OF_WEEK)-1);
        String dateString = sdf.format(cal.getTime());

        try {
            JSONObject jsonObject = new Gson().fromJson (new StringReader(new String(req.getBody())), JSONObject.class);
            String utcT = (String) jsonObject.get("utcT");
            System.out.println("utcT: " + utcT);

            // 한국 시간대 설정
            TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");

            // 원하는 형식으로 날짜 포맷 지정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            sdf.setTimeZone(tz);

            // 현재 한국 시간 출력
            String koreanTime = sdf.format(cal.getTime());
            System.out.println("Current Korean Time: " + koreanTime);

            cal = Calendar.getInstance(tz);
            wk = String.valueOf(cal.get(Calendar.DAY_OF_WEEK)-1);
            dateString = sdf.format(cal.getTime());

            System.out.println("wk: " + wk);
            System.out.println("koreanTime: " + koreanTime);
            System.out.println("dateString: " + dateString);

        } catch (Exception e1) {
            log.error("", e1);
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"400\",\"syDt\":\""+dateString+"\",\"wk\":\""+wk+"\"}");
        }

        return RemoteHandler.makeResponse(req, "{\"rtCd\":\"200\",\"syDt\":\""+dateString+"\",\"wk\":\""+wk+"\"}");
    }
}

