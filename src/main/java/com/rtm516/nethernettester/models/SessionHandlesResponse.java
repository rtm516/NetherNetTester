package com.rtm516.nethernettester.models;

import java.math.BigInteger;
import java.util.List;

public record SessionHandlesResponse(List<SessionHandleResponse> results) {
    public record SessionHandleResponse(
        String createTime,
        SessionCustomProperties customProperties,
        Object gameTypes,
        String id,
        String inviteProtocol,
        String ownerXuid,
        Object relatedInfo,
        SessionRef sessionRef,
        String titleId,
        String type,
        int version
    ) {
    }

    public record SessionCustomProperties(
        int BroadcastSetting,
        boolean CrossPlayDisabled,
        String Joinability,
        boolean LanGame,
        int MaxMemberCount,
        int MemberCount,
        boolean OnlineCrossPlatformGame,
        List<Connection> SupportedConnections,
        int TitleId,
        int TransportLayer,
        String levelId,
        String hostName,
        String ownerId,
        String rakNetGUID,
        String worldName,
        String worldType,
        int protocol,
        String version,
        boolean isEditorWorld,
        boolean isHardcore
    ) {
    }

    public record SessionRef(
        String scid,
        String templateName,
        String name
    ) {
    }

    public record Connection(
        int ConnectionType,
        String HostIpAddress,
        int HostPort,
        BigInteger NetherNetId,
        String PmsgId
    ) {
    }
}