package com.daesungiot.gateway.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class M2mCin {

    // m2m:cin 고정
    @JsonProperty("m2m:cin")
    private Cin defaultValue;

    @Getter
    @Setter
    public static class Cin{
        private String con;
    }
}
