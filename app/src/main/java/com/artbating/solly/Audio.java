package com.artbating.solly;

import java.util.Map;

/**
 * Created by chunghoen on 2017-03-19.
 */

public class Audio {
    Map<String,Object> audio;
    public Audio(){}
    public Audio(Map<String,Object> audio) {
        this.audio = audio;
    }

    public Map<String,Object> getAudio() {
        return audio;
    }
}
