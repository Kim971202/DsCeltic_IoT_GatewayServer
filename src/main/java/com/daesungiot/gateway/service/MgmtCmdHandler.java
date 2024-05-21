package com.daesungiot.gateway.service;

import com.daesungiot.gateway.daesung.RemoteMessage;
import com.google.gson.Gson;
import com.daesungiot.gateway.binary.BinaryMessage;
import com.daesungiot.gateway.binary.ResponseHandler;
import io.netty.channel.ChannelHandlerContext;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.HashMap;

@Component(value = "mgmtcmd")
public class MgmtCmdHandler implements ResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MgmtCmdHandler.class);
	private static HashMap<Short, String> cmdCode2MgmtCmd = new HashMap<Short, String>();
	
	static {
		cmdCode2MgmtCmd.put((short)0x201, "powr");
		cmdCode2MgmtCmd.put((short)0x203, "opMd");
		cmdCode2MgmtCmd.put((short)0x205, "htTp");
		cmdCode2MgmtCmd.put((short)0x207, "wtTp");
		cmdCode2MgmtCmd.put((short)0x209, "hwTp");
		cmdCode2MgmtCmd.put((short)0x211, "ftMd");
		cmdCode2MgmtCmd.put((short)0x215, "24h" );
		cmdCode2MgmtCmd.put((short)0x217, "12h" );
		cmdCode2MgmtCmd.put((short)0x219, "7wk" );
		cmdCode2MgmtCmd.put((short)0x221, "fwh" );
		cmdCode2MgmtCmd.put((short)0x227, "sfck");
		cmdCode2MgmtCmd.put((short)0x229, "reSt");
		cmdCode2MgmtCmd.put((short)0x231, "mfAr");
		cmdCode2MgmtCmd.put((short)0x233, "fcnt");
		cmdCode2MgmtCmd.put((short)0x235, "blCf");
		cmdCode2MgmtCmd.put((short)0x301, "rsPw");
		cmdCode2MgmtCmd.put((short)0x303, "sdCd");
		cmdCode2MgmtCmd.put((short)0x305, "fcLc");
		cmdCode2MgmtCmd.put((short)0x307, "slSt");
		cmdCode2MgmtCmd.put((short)0x311, "rqSt");
		cmdCode2MgmtCmd.put((short)0x313, "rsSl");

	}

	@Autowired
	private InteractionRequest mccRequest;
	
//	public void setMccRequest(InteractionRequest mccRequest) {
//		this.mccRequest = mccRequest;
//	}

	@Override
	public BinaryMessage handle(ChannelHandlerContext ctx, String cseid, String dKey, BinaryMessage msg) {
//		JsonPowr jsonPowr = new JsonPowr();

		RemoteMessage req = (RemoteMessage) msg;
		String mgmtCmd = cmdCode2MgmtCmd.get(req.getCmdCode());

		try {
			JSONObject jsonObject = new Gson().fromJson (new StringReader(new String(req.getBody())), JSONObject.class);
			String rtCd = (String) jsonObject.get("rtCd");
			String resourceId = (String) jsonObject.get("ri");

			if(dKey == null) {
				int endMcseid = cseid.lastIndexOf('.');
				String newDkey = cseid.substring(endMcseid + 1);

			    dKey = newDkey;
				System.out.println("NEWNEWNEWNEWNEWNEWNEWNEWN" + dKey);
			}
			
			if(rtCd != null && rtCd.indexOf("200") < 0) {
//				MccResponse resp = mccRequest.execInstanceResultPut(cseid, dKey, mgmtCmd, resourceId, 4);
				MccResponse resp = mccRequest.ContentInstanceResultPut(cseid, dKey, mgmtCmd, resourceId, 4);
			} else {
				String chCd = (String) jsonObject.get("deviceType");
				int ichCd = 0;
				try {
					ichCd = Integer.parseInt(chCd);
				} catch (Exception e) {

				}
//				MccResponse resp = mccRequest.execInstanceResultPut(cseid, dKey, mgmtCmd, resourceId, ichCd);
				System.out.println("================================================");
				//System.out.println(cseid + dKey + mgmtCmd + resourceId + ichCd);
				System.out.println("================================================");
				MccResponse resp = mccRequest.ContentInstanceResultPut(cseid, dKey, mgmtCmd, resourceId, ichCd);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return null;
	}
}
