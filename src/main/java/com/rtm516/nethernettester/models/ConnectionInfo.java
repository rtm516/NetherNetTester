package com.rtm516.nethernettester.models;

public record ConnectionInfo(SessionHandlesResponse.Connection connection, int protocolVersion) {
}
