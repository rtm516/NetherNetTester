package com.rtm516.nethernettester;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rtm516.nethernettester.json.DateConverter;
import com.rtm516.nethernettester.json.InstantConverter;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944;
import org.cloudburstmc.protocol.bedrock.codec.v975.Bedrock_v975;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

public class Constants {
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantConverter())
        .registerTypeAdapter(Date.class, new DateConverter())
        .disableHtmlEscaping()
        .create();

    public static final String SERVICE_CONFIG_ID = "4fc10100-5f7a-4470-899b-280835760c07"; // The service config ID for Minecraft

    public static final URI FRIENDS = URI.create("https://peoplehub.xboxlive.com/users/me/people/friends");

    public static final String STORAGE_FOLDER = "./";

    public static final int TIMEOUT_MS = 5000;

    /**
     * Gathered from scraped web requests, seems to use the below enum
     * https://github.com/LiteLDev/LeviLamina/blob/main/src/mc/network/ConnectionType.h
     */
    public static final int ConnectionTypeWebRTC = 3;
    public static final int ConnectionTypeJsonRpc = 7;

    /**
     * Used for the micro nethernet server that transfers the client to the real server
     */
    public static final BedrockCodec BEDROCK_CODEC = Bedrock_v975.CODEC;
}
