package com.wlk.impl;

import com.wlk.HelloMyrpc;
import com.wlk.annotation.MyrpcApi;

@MyrpcApi
public class HelloMyrpcImpl implements HelloMyrpc {
    @Override
    public String sayHi(String msg) {
        return "Hi! client"+msg;
    }
}
