package com.delex.delexexpert.model;

/**
 * Created by Administrator on 2018-05-24.
 */

public class TokenModel {
    String key;

    public TokenModel(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "TokenModel{" +
                "key='" + key + '\'' +
                '}';
    }
}
