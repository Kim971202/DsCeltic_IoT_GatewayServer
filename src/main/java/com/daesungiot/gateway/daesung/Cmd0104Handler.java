package com.daesungiot.gateway.daesung;

import com.google.gson.Gson;
import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import com.daesungiot.gateway.service.InteractionRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
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
public class Cmd0104Handler implements ResponseHandler {

    @Autowired
    private InteractionRequest mccRequest;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    @Override
    public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg) {
        System.out.println("Cmd0104Handler -> handle CALLED");
        RemoteMessage req = (RemoteMessage) msg;
        System.out.println("11111111111111111111111111111111111111111111111111111");
        Calendar cal = Calendar.getInstance();
        String wk = String.valueOf(cal.get(Calendar.DAY_OF_WEEK)-1);
        String dateString = sdf.format(cal.getTime());

        try {
            JSONObject jsonObject = new Gson().fromJson (new StringReader(new String(req.getBody())), JSONObject.class);
            String utcT = (String) jsonObject.get("utcT");
            System.out.println("utcT: " + utcT);
            TimeZone tz = TimeZone.getDefault();

            try {
                int sign = 1;
                if(utcT.charAt(0) == '-')
                    sign = -1;
                else if(utcT.charAt(0) == '+')
                    sign = 1;
                else
                    new Exception("invaild format");

                int offsetMillis = (sign * Integer.parseInt(utcT.substring(1, 3))*60*60*1000) + (sign * Integer.parseInt(utcT.substring(4))*60*1000);
                System.out.println("offsetMillis: " + offsetMillis);
                tz.setRawOffset(offsetMillis);
                System.out.println("tz.getRawOffset(): " + tz.getRawOffset());
            } catch (Exception e) {
                System.out.println(e);
            }
            cal = Calendar.getInstance(tz);
            wk = String.valueOf(cal.get(Calendar.DAY_OF_WEEK)-1);
            dateString = sdf.format(cal.getTime());

            System.out.println("wk: " + wk);
            System.out.println("dateString: " + dateString);

        } catch (Exception e1) {
            System.out.println(e1);
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"400\",\"syDt\":\""+dateString+"\",\"wk\":\""+wk+"\"}");
        }
        System.out.println("dKey: " + dKey);
        if(dKey == null) {
            return RemoteHandler.makeResponse(req, "{\"rtCd\":\"401\",\"syDt\":\""+dateString+"\",\"wk\":\""+wk+"\"}");
        }

        return RemoteHandler.makeResponse(req, "{\"rtCd\":\"200\",\"syDt\":\""+dateString+"\",\"wk\":\""+wk+"\"}");
    }
}

