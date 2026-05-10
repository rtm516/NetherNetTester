package com.rtm516.nethernettester.utils;

import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.lang.JoseException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Base64;
import java.util.HashMap;

// From https://github.com/Kas-tle/ProxyPass/blob/b7b472bca0c19308a776874438227fc20c5391c1/src/main/java/org/cloudburstmc/proxypass/network/bedrock/util/ForgeryUtils.java
public class ForgeryUtils {
    @SuppressWarnings("unchecked")
    public static String forgeOnlineSkinData(BedrockAuthManager authManager, JSONObject skinData, SocketAddress serverAddress) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(authManager.getSessionKeyPair().getPublic().getEncoded());

        HashMap<String,Object> overrideData = new HashMap<>();
        overrideData.put("DeviceId", authManager.getDeviceId().toString().replace("-", ""));
        overrideData.put("ThirdPartyName", authManager.getMinecraftMultiplayerToken().getCached().getDisplayName());

        switch (serverAddress) {
            case InetSocketAddress a -> {
                overrideData.put("ServerAddress", a.getHostString() + ":" + a.getPort());
            }
            case NetherNetAddress a -> {
                overrideData.put("ServerAddress", a.getNetworkId());
            }
            default -> {
                throw new IllegalArgumentException("Unsupported serverAddress type: " + serverAddress.getClass().getName());
            }
        }

        skinData.putAll(overrideData);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmHeaderValue("ES384");
        jws.setHeader(HeaderParameterNames.X509_URL, publicKeyBase64);
        jws.setPayload(skinData.toJSONString());
        jws.setKey(authManager.getSessionKeyPair().getPrivate());

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new RuntimeException(e);
        }
    }
}
