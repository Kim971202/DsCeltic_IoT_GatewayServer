package com.daesungiot.gateway.daesung;

import com.google.gson.Gson;
import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.net.URI;
import java.text.DecimalFormat;
@Getter
@Setter
@Component(value = "cmd0212")
public class Cmd0212Handler implements ResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(Cmd0212Handler.class);
	
	private String opWeatherAppId;
	
	public void setOpWeatherAppId(String opWeatherAppId) {
		this.opWeatherAppId = opWeatherAppId;
	}

	@Override
	public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg) {
		RemoteMessage req = (RemoteMessage) msg;
		if(dKey == null) {
			int endIdx = cseid.lastIndexOf('.');
			dKey = cseid.substring(endIdx + 1);

			if (dKey.isEmpty()) {
				return RemoteHandler.makeResponse(req, "{\"rtCd\":\"401\"}");
			}
		}
		
		try { 
			JSONObject jsonObject = new Gson().fromJson (new StringReader(new String(req.getBody())), JSONObject.class);
			
			String lat = null;
			String lon = null;
			try {
				lat = (String) jsonObject.get("rcLt");
				lat = lat.replace("+", "");
				lon = (String) jsonObject.get("rcLg");
				lon = lon.replace("+", "");
			} catch (Exception e) {}
			
			//37.574515 126.976930 광화문광장
			if(lat == null || lat.length() < 1 || lon == null || lon.length() < 1) {
				lat = "37.574515";
				lon = "126.976930";
			}
				
			URI uri = new URIBuilder()
					.setScheme("http")
					.setHost("api.openweathermap.org")
					.setPath("/data/2.5/weather")
					.setParameter("lat", lat).setParameter("lon", lon).setParameter("appid", opWeatherAppId).build();
	
			HttpGet get = new HttpGet(uri);
			HttpClient httpclient = new DefaultHttpClient();
			String tempStr;
			try {
				tempStr = "+00.0";

				HttpResponse response = httpclient.execute(get);
				if(response.getStatusLine().getStatusCode() > 199 && response.getStatusLine().getStatusCode() < 300) {
					HttpEntity responseEntity = response.getEntity();
					String responseString = EntityUtils.toString(responseEntity);
					
					JSONParser parser = new JSONParser();
					Object obj1 = parser.parse(new StringReader(responseString));
					JSONObject jsonObject1 = (JSONObject) obj1;
					JSONObject main = (JSONObject)jsonObject1.get("main");
					String tempVal = String.valueOf(main.get("temp"));
					Double temp = Double.parseDouble(tempVal);
					temp = temp - 273.15;
					
					DecimalFormat df = new DecimalFormat("00.0");
					tempStr = df.format(temp);
					if(temp > 0) 
						tempStr = "+" + tempStr;
				}
			} finally {
				httpclient.getConnectionManager().shutdown();
			}
			return RemoteHandler.makeResponse(req, "{\"rtCd\":\"200\",\"nwTp\":\""+tempStr+"\"}");

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return RemoteHandler.makeResponse(req, "{\"rtCd\":\"500\"}");
		}
	}
}
