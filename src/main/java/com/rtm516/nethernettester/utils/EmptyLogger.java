package com.rtm516.nethernettester.utils;

import com.rtm516.nethernettester.Logger;

public class EmptyLogger implements Logger {
    @Override
    public void info(String message) {

    }

    @Override
    public void warn(String message) {

    }

    @Override
    public void error(String message) {

    }

    @Override
    public void error(String message, Throwable ex) {

    }

    @Override
    public void debug(String message) {

    }

    @Override
    public Logger prefixed(String prefixString) {
        return this;
    }
}
