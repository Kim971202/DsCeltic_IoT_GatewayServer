package com.daesungiot.gateway.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class M2mCnt {

    // m2m:cnt 고정
    @JsonProperty("m2m:cnt")
    private Cnt defaultValue;

    @Getter
    @Setter
    public static class Cnt{
        private String rn;
        private int mbs;
        private List<String> lbl;
    }
}
