package com.daesungiot.gateway.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MccResponse {

    private int responseCode;
    private String dKey;
    private String responseContents;
    private String deviceId;

}
