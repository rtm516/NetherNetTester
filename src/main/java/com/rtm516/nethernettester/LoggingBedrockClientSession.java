package com.rtm516.nethernettester;

import org.cloudburstmc.protocol.bedrock.BedrockClientSession;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public class LoggingBedrockClientSession extends BedrockClientSession {
    private final Logger logger;

    public LoggingBedrockClientSession(BedrockPeer peer, int subClientId, Logger logger) {
        super(peer, subClientId);
        this.logger = logger;
    }

    @Override
    protected void logOutbound(BedrockPacket packet) {
        if (this.logging) {
            logger.debug("Outbound: " + packet.toString());
        }
    }

    @Override
    protected void logInbound(BedrockPacket packet) {
        if (this.logging) {
            logger.debug("Inbound: " + packet.toString());
        }
    }
}
