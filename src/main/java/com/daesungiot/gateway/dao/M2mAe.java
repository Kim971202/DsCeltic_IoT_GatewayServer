package com.daesungiot.gateway.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class M2mAe {

    // m2m:ae 고정
    @JsonProperty("m2m:ae")
    private Ae defaultValue;

    @Getter
    @Setter
    public static class Ae{
        private String rn;
        private String api;
        private List<String> lbl;
        private boolean rr;
        private List<String> poa;
    }
}
