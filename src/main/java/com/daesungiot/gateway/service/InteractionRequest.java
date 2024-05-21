package com.daesungiot.gateway.service;

import com.daesungiot.gateway.controller.RemoteController;
import com.daesungiot.gateway.dao.M2mAe;
import com.daesungiot.gateway.dao.M2mCin;
import com.daesungiot.gateway.dao.M2mCnt;
import com.daesungiot.gateway.dao.M2mSub;
import com.daesungiot.gateway.util.Common;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Component(value = "mobiusRequest")
public class InteractionRequest {

    @Autowired
    Common common;

    private static int requestIndex = 0;
    private static int registIndex = 0;
    @Value("${mobius-request.inCSEAddress}")
    private String inCSEAddress;
    @Value("${mobius-request.appServerAddress}")
    private String appServerAddress;
    @Value("${mobius-request.passCode}")
    private String passCode;
    @Value("${mobius-request.from}")
    private String from;
    @Value("${mobius-request.pointOfAccess}")
    private String pointOfAccess;
    private boolean DEBUGPRINT = true;

    private static PoolingHttpClientConnectionManager connectionManager = null;
    private static CloseableHttpClient httpClient;

    ObjectMapper objectMapper = new ObjectMapper();

    LocalDateTime timeStamp = Common.getTimeAsiaSeoulNow();

    public CloseableHttpClient getHttpClient() {
        System.out.println("InteractionRequest -> getHttpClient CALLED");
        if (connectionManager == null) {
            connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(500);
            connectionManager.setDefaultMaxPerRoute(50);
            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(500)
                    .setConnectTimeout(10)
                    .setSocketTimeout(2000)
                    .setExpectContinueEnabled(true).build();

            httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .setConnectionManager(connectionManager)
                    .build();
        }
        return httpClient;
    }

    private MccResponse pickupResponse(URI uri, String jsonBody, HttpResponse response) throws org.apache.http.ParseException, IOException {
        System.out.println("InteractionRequest -> pickupResponse CALLED");

        MccResponse mccResp = new MccResponse();

        int responseCode = response.getStatusLine().getStatusCode();
        mccResp.setResponseCode(responseCode);


        Header[] responseHeader = response.getHeaders("Content-Location");
        String dKey = "";
        if (responseHeader.length > 0) {
            String location = responseHeader[0].getValue();
            dKey = location.substring(7, 38);
        }

        mccResp.setDKey(dKey);

        HttpEntity responseEntity = response.getEntity();
        String responseString = EntityUtils.toString(responseEntity);
        mccResp.setResponseContents(responseString);
        System.out.println("responseString : " + responseString);

        System.out.println("====HTTP Request URI===============================================================================");
        System.out.println("HTTP Request URI : " + uri.toString());
        System.out.println("====HTTP Request Body==============================================================================");
        System.out.println("HTTP Request Body : " + jsonBody);
        System.out.println("====HTTP Response Code, dKey=======================================================================");
        System.out.println("HTTP Response Code, dKey : " + responseCode + "," + dKey);
        System.out.println("====HTTP Response String===========================================================================");
        System.out.println("HTTP Response String : " + responseString);

        return mccResp;
    }

    // Remote ae생성 Method
    public MccResponse createAE(String cseid, String srNo, String modelCode, String rKey, String tKey) throws UnsupportedEncodingException, ClientProtocolException,
            URISyntaxException, IOException {

        M2mAe aeObject = new M2mAe();
        M2mAe.Ae ae = new M2mAe.Ae();

        int endMcseid = cseid.lastIndexOf('.');
        String aeRn = cseid.substring(endMcseid + 1);

        ae.setRn(aeRn);
        ae.setApi(modelCode + "." + rKey + "." + srNo + ":GW");
        ae.setLbl(Arrays.asList(rKey, tKey, cseid));
        ae.setRr(true);
        ae.setPoa(Arrays.asList(pointOfAccess));
        aeObject.setDefaultValue(ae);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aeObject);
        System.out.println("========================================================");
        System.out.println(requestBody);
        System.out.println("========================================================");

        StringEntity entity = new StringEntity(requestBody);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius")
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=2");
        post.setHeader("X-M2M-Origin", "S" + rKey);
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            System.out.println("response : " + response);
            mccResp = pickupResponse(uri, requestBody, response);
//            containerCreate();
        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }

    // 각방 AE 생성
    public MccResponse eachRcCreateAE(String cseid, String srNo, String modelCode, String rKey, String tKey, String prId) throws UnsupportedEncodingException, ClientProtocolException,
            URISyntaxException, IOException {

        M2mAe aeObject = new M2mAe();
        M2mAe.Ae ae = new M2mAe.Ae();

        int endMcseid = cseid.lastIndexOf('.');
        String aeRn = cseid.substring(endMcseid + 1);


//        ae.setRn("0.2.481.1.1." + modelCode + "." + timeStamp.toString());
        ae.setRn(aeRn);
        ae.setApi(modelCode + "." + rKey + "." + srNo + ":GW");
        ae.setLbl(Arrays.asList(rKey, tKey, cseid));
        ae.setRr(true);
        ae.setPoa(Arrays.asList(pointOfAccess));
        aeObject.setDefaultValue(ae);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aeObject);
        System.out.println("========================================================");
        System.out.println(requestBody);
        System.out.println("========================================================");

        StringEntity entity = new StringEntity(requestBody);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius")
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=2");
        post.setHeader("X-M2M-Origin", prId);
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }


    public MccResponse createContainer(String srNo, String[] labels, String aeName) throws URISyntaxException, IOException {
        M2mCnt cntObject = new M2mCnt();
        M2mCnt.Cnt cnt = new M2mCnt.Cnt();

        cnt.setRn(srNo);
        cnt.setMbs(16384);
        cnt.setLbl(List.of(labels));
        cntObject.setDefaultValue(cnt);
        System.out.println(cntObject);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cntObject);
        StringEntity entity = new StringEntity(requestBody);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius" + "/" + aeName)
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=3");
        post.setHeader("X-M2M-Origin", "S");
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }


    public MccResponse createSubscription(String aeName, String srNo, String rKey) throws URISyntaxException, IOException {
        M2mSub subObject = new M2mSub();
        M2mSub.Sub sub = new M2mSub.Sub();
        M2mSub.Sub.Enc enc = new M2mSub.Sub.Enc();

        enc.setNet(List.of(3));

        sub.setRn(rKey);
        sub.setEnc(enc);
        sub.setNu(List.of(appServerAddress));
        sub.setExc(10);

        subObject.setDefaultValue(sub);
        System.out.println(subObject);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(subObject);
        StringEntity entity = new StringEntity(requestBody);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius" + "/" + aeName + "/" + srNo)
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=23");
        post.setHeader("X-M2M-Origin", "S");
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }

    public MccResponse createContentInstance(String aeName, String srNo, String jsonBody, String cseid) throws Exception {
        M2mCin cinObject = new M2mCin();
        M2mCin.Cin cin = new M2mCin.Cin();

        List<String> key = new ArrayList<>() {
            {
                add("uuId");
                add("srNo");
                add("deviceId");
            }
        };

        List<String> value = new ArrayList<>() {
            {
                add(common.getTransactionId());
                add(srNo);
                add(cseid);
            }
        };

        String newBody = common.addCon(jsonBody, key, value);

        cin.setCon(newBody);
        cinObject.setDefaultValue(cin);

        String serialNo = common.hexaToText(srNo);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cinObject);
        StringEntity entity = new StringEntity(requestBody);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius" + "/" + aeName + "/" + serialNo)
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=4");
        post.setHeader("X-M2M-Origin", "S");
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }

    public MccResponse createContentInstance(String aeName, String srNo, String jsonBody, String cmdCode, String cseid) throws Exception {
        M2mCin cinObject = new M2mCin();
        M2mCin.Cin cin = new M2mCin.Cin();

        System.out.println("srNo: " + srNo);

        String uuId = RemoteController.uuIdMap.get(cseid);
        System.out.println("Interaction uuId: " + uuId);
        List<String> key = new ArrayList<>() {
            {
                add("functionId");
                add("uuId");
                add("srNo");
                add("deviceId");
            }
        };
        String finalCmdCode = cmdCode;
        List<String> value = new ArrayList<>() {
            {
                add(finalCmdCode);
//                add(common.getTransactionId());
                add(uuId);
                add(srNo);
                add(cseid);
            }
        };

        System.out.println("key: " + key);
        System.out.println("value: " + value);

        String newBody = common.addCon(jsonBody, key, value);


        cin.setCon(newBody);
        cinObject.setDefaultValue(cin);

        if (cmdCode.equals("rqSt")) {
            Map<String, Object> rqStContent = new HashMap<String, Object>();
            rqStContent.put("requestId", "rqSt");
            List<String> valueKeys = new ArrayList<String>();
            valueKeys.add("rsCf");
            rqStContent.put("requestValueKeys", valueKeys);
            cmdCode = new Gson().toJson(rqStContent);
        }

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cinObject);
        StringEntity entity = new StringEntity(requestBody);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius" + "/" + aeName + "/" + srNo)
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=4");
        post.setHeader("X-M2M-Origin", "S" + cmdCode);
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }


    public MccResponse ContentInstanceResultPut(String cseid, String dKey, String mgmtCmd, String uuId, String resourceId, String rKey, String srNo, int errorCode) throws Exception {
        M2mCin cinObject = new M2mCin();
        M2mCin.Cin cin = new M2mCin.Cin();

        List<String> key = new ArrayList<>() {
            {
                add("deviceId");
                add("functionId");
                add("uuId");
                add("rKey");
                add("errorCode");
            }
        };

        List<String> value = new ArrayList<>() {
            {
                add(cseid);
                add(mgmtCmd);
                add(uuId);
                add(rKey);
                add(String.valueOf(errorCode));
            }
        };

        String newBody = common.createCon(key, value);
        String serialNo = common.hexaToText2(srNo);
        cin.setCon(newBody);
        cinObject.setDefaultValue(cin);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cinObject);
        StringEntity entity = new StringEntity(requestBody);

        int endIdx = cseid.lastIndexOf('.');
        String aeName = cseid.substring(endIdx + 1);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius" + "/" + aeName + "/" + serialNo)
//                .setPath("/Mobius" + "/" + "boiler_final" + "/" + "request_last")
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=4");
        post.setHeader("X-M2M-Origin", "S" + from);
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }

    public MccResponse ContentInstanceResultPut(String cseid, String dKey, String mgmtCmd, String resourceId, int errorCode) throws Exception {
        M2mCin cinObject = new M2mCin();
        M2mCin.Cin cin = new M2mCin.Cin();

        int endMcseid = cseid.lastIndexOf('.');
        String srNo = cseid.substring(endMcseid + 1);

        String uuId = RemoteController.uuIdMap.get(cseid);
        System.out.println("Interaction uuId: " + uuId);

        List<String> key = new ArrayList<>() {
            {
                add("deviceId");
                add("functionId");
                add("erCd");
                add("uuId");

            }
        };

        List<String> value = new ArrayList<>() {
            {
                add(cseid);
                add(mgmtCmd);
                add(String.valueOf(errorCode));
                add(uuId);
            }
        };

        String newBody = common.createCon(key, value);
        String serialNo = common.hexaToText2(srNo);
        cin.setCon(newBody);
        cinObject.setDefaultValue(cin);

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cinObject);
        StringEntity entity = new StringEntity(requestBody);

        int endIdx = cseid.lastIndexOf('.');
        String aeName = cseid.substring(endIdx + 1);

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius" + "/" + aeName + "/" + serialNo)
                .build();

        HttpPost post = new HttpPost(uri);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/vnd.onem2m-res+json;ty=4");
        post.setHeader("X-M2M-Origin", "S" + from);
        post.setHeader("locale", "ko");
        post.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        post.setEntity(entity);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;

        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(post);
            mccResp = pickupResponse(uri, requestBody, response);

        } catch (Exception e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }

    /**
     * RemoteCSE 정보 삭제[각방제어기의 온도조절기 삭제]

    public MccResponse remoteCSEDelete(String cseid, String dKey, String content) throws UnsupportedEncodingException, ClientProtocolException, ParseException, URISyntaxException, IOException {
        System.out.println("InteractionRequest -> remoteCSEDelete(String cseid, String dKey, String content) CALLED");

        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<m2m:exin\n" +
                "xmlns:m2m=\"http://www.onem2m.org/xml/protocols\"\n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(inCSEAddress)
                .setPath("/Mobius/remoteCSE-" + cseid).build();

        HttpDelete delete = new HttpDelete(uri);
        delete.setHeader("Accept", "application/xml");
        delete.setHeader("locale", "ko");
        delete.setHeader("X-M2M-Origin", from);
        delete.setHeader("X-M2M-RI", Integer.toString(requestIndex));
        delete.setHeader("dKey", dKey);
        requestIndex++;

        MccResponse mccResp = null;
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(delete);
            mccResp = pickupResponse(uri, requestBody, response);
        } catch (ClientProtocolException e) {
            System.out.println("send to oneM2M Error : " + e);
        } finally {
            response.close();
        }
        return mccResp;
    }
     */
}
