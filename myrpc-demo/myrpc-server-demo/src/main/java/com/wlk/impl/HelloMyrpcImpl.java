package com.wlk.impl;

import com.wlk.HelloMyrpc;
import com.wlk.annotation.MyrpcApi;

@MyrpcApi(group = "g1")
public class HelloMyrpcImpl implements HelloMyrpc {
    @Override
    public String sayHi(String msg) {
        return "Hi! client"+msg;
    }
}
