package com.rtm516.nethernettester.models;

import java.util.Date;

public record AddFriendResponse(
    String xuid,
    Date addedDateTimeUtc,
    boolean friendRequestSent
) {
}
