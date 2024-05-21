package com.daesungiot.gateway.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class M2mSub {

    // m2m:sub 고정
    @JsonProperty("m2m:sub")
    private Sub defaultValue;

    @Getter
    @Setter
    public static class Sub {
        private String rn;
        private Enc enc;
        private List<String> nu;
        private int exc;

        @Getter
        @Setter
        public static class Enc {
            private List<Integer> net;
        }
        }


}
