package com.daesungiot.gateway.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
public class Common {

    private static HashMap<String, String> srNoMap = new HashMap<>();

    public static String getSrNoMap(String key) {
        return srNoMap.get(key);
    }

    public static void setSrNoMap(String serialKey, String srNo) {
        srNoMap.put(serialKey, srNo);
    }

    private static HashMap<String, String> modelCodeMap = new HashMap<>();

    public static String getModelCodeMap(String key) {
        return modelCodeMap.get(key);
    }

    public static void setModelCodeMap(String modelCodeKey, String modelCode) {
        modelCodeMap.put(modelCodeKey, modelCode);
    }

    public static String getValueFromMsg(Object msg, String inputKey) {

        if (msg == null) return null;
        String[] parts = msg.toString().split("\\{", 2); // { 까지 제거

        String jsonString = "{" + parts[1]; // { 문자가 분리되어 제외되었으므로 다시 추가
        System.out.println(jsonString);
        JSONObject jsonObject = new JSONObject(jsonString);

        return jsonObject.getString(inputKey);
    }

    /**
     * 트랜잭션 ID
     *
     * @return
     */
    public String getTransactionId() {
        return getTransactionIdBaseUUID();
    }

    /**
     * UUID 리턴
     *
     * @return a432e21a-54df-4e43-8ef9-99cd274dced8
     */
    private String getTransactionIdBaseUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * LocalTime 리턴
     *
     * @return 2024-01-16T11:04:38.047567300
     */
    public static LocalDateTime getTimeAsiaSeoulNow() {
        return getTimeNow("Asia/Seoul");
    }

    public static LocalDateTime getTimeNow(String zoneId) {
        return LocalDateTime.now(ZoneId.of(zoneId));
    }

    public String readCon(String jsonString, String value) throws Exception {
        String result;
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        JsonNode fullNode = jsonNode.path("m2m:sgn");
        JsonNode baseNode = jsonNode.path("m2m:sgn").path("nev").path("rep").path("m2m:cin");
        JsonNode conNode = baseNode.path("con");
        JsonNode hoursNode = baseNode.path("con").path("hours");
        JsonNode weekNode = baseNode.path("con").path("weekList");
        JsonNode awakeNode = baseNode.path("con").path("awakeList");

        if (value.equals("sur")) {
            return fullNode.path("sur").asText();
        } else if (value.equals("nev")) {
            return fullNode.path("nev").asText();
        }

        switch (value) {
            case "con":
                return objectMapper.writeValueAsString(conNode);

            case "functionId":
                return baseNode.path("con").path("functionId").asText();

            case "uuId":
                return baseNode.path("con").path("uuId").asText();

            case "rKey":
                return baseNode.path("con").path("rKey").asText();

            case "deviceId":
                return baseNode.path("con").path("deviceId").asText();

            case "ri":
                return baseNode.path("ri").asText();

            case "powerStatus":
                return baseNode.path("con").path("powerStatus").asText();

            case "modeCode":
                return baseNode.path("con").path("modeCode").asText();

            case "sleepCode":
                return baseNode.path("con").path("sleepCode").asText();

            case "temperature":
                return baseNode.path("con").path("temperature").asText();

            case "lockSet":
                return baseNode.path("con").path("lockSet").asText();

            case "type24h":
                return baseNode.path("con").path("type24h").asText();

            case "hours":
                return objectMapper.writeValueAsString(hoursNode);

            case "workPeriod":
                return baseNode.path("con").path("workPeriod").asText();

            case "workTime":
                return baseNode.path("con").path("workTime").asText();

            case "weekList":
                return objectMapper.writeValueAsString(weekNode);

            case "awakeList":
                return objectMapper.writeValueAsString(awakeNode);

            case "brightnessLevel":
                return baseNode.path("con").path("brightnessLevel").asText();

        }

            return "Nothing to read";
        }


//        public String createDkey (String jsonString, String value) throws Exception {
//            String result;
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode jsonNode = objectMapper.readTree(jsonString);
//
//            JsonNode fullNode = jsonNode.path("m2m:ae");
//
//            if (value.equals("rn")) {
//                return fullNode.path("rn").asText();
//            }
//            return "Create dkey failed!!";
//        }

        public String addCon (String body, List < String > key, List < String > value) throws Exception {

            String modifiedJson = null;
            System.out.println("String Body : " + body);
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(body);

                // Add a new item under the "con" key
                for (int i = 0; i < key.size(); i++) {
                    ((ObjectNode) jsonNode).put(key.get(i), value.get(i));
                }
                // Convert the modified JSON object back to a string
                modifiedJson = objectMapper.writeValueAsString(jsonNode);

                System.out.println("modifiedJson");
                System.out.println(modifiedJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return modifiedJson;
        }

        public String createCon (List < String > key, List < String > value) throws Exception {

            HashMap<String, String> myMap = new HashMap<>();

            for (int i = 0; i < key.size(); i++) {
                myMap.put(key.get(i), value.get(i));
            }
            JSONObject json = new JSONObject(myMap);
            System.out.println("jsonjsonjsonjsonjsonjsonjsonjsonjsonjson");
            System.out.println("jsonjsonjsonjsonjsonjsonjsonjsonjsonjson:" + json);

            return json.toString();
        }

    public String hexaToText (String deviceId) throws Exception {

        // "." 이후의 문자열을 추출
        int startIndex = deviceId.lastIndexOf('.') + 1; // 마지막 점 이후부터 시작
        if (startIndex < 0 || startIndex + 32 > deviceId.length()) {
            throw new Exception("Input string is too short to extract the required substring.");
        }

        String hex = deviceId.substring(startIndex, startIndex + 32);

        // Hex string을 텍스트로 변환
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            stringBuilder.append((char) Integer.parseInt(str, 16));
        }
        return stringBuilder.toString().replaceAll(" ", "");
    }

//        public String hexaToText (String deviceId) throws Exception {
//
////            String HexString = deviceId.substring(33, 57);
//
//                String HexString = deviceId.substring(41, 65);
//
//                String OutputString = new String();
//                char[] Temp_Char = HexString.toCharArray();
//                for (int x = 0; x < Temp_Char.length; x = x + 2) {
//                    String Temp_String = "" + Temp_Char[x] + "" + Temp_Char[x + 1];
//                    char character = (char) Integer.parseInt(Temp_String, 16);
//                    OutputString = OutputString + character;
//                }
//
//                return OutputString;
//
//        }

    public String hexaToText2 (String srNo) throws Exception {

//            String HexString = deviceId.substring(33, 57);

        String HexString = srNo.substring(8);

        String OutputString = new String();
        char[] Temp_Char = HexString.toCharArray();
        for (int x = 0; x < Temp_Char.length; x = x + 2) {
            String Temp_String = "" + Temp_Char[x] + "" + Temp_Char[x + 1];
            char character = (char) Integer.parseInt(Temp_String, 16);
            OutputString = OutputString + character;
        }

        return OutputString;

    }



    }

