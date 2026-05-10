package com.rtm516.nethernettester;

import com.rtm516.nethernettester.utils.ForgeryUtils;
import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import org.cloudburstmc.protocol.bedrock.BedrockClientSession;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SessionPacketHandler implements BedrockPacketHandler {
    private final BedrockClientSession session;
    private final Logger logger;
    private final AuthManager authManager;
    private final NetherNetAddress netherNetAddress;
    private final Consumer<String> statusCallback;
    private final CompletableFuture<InetSocketAddress> future;
    private final ScheduledFuture<?> timeoutFuture;

    public SessionPacketHandler(BedrockClientSession session, Logger logger, AuthManager authManager, NetherNetAddress netherNetAddress, Consumer<String> statusCallback, CompletableFuture<InetSocketAddress> future, ScheduledExecutorService scheduledExecutorService) {
        this.session = session;
        this.logger = logger;
        this.authManager = authManager;
        this.netherNetAddress = netherNetAddress;
        this.statusCallback = statusCallback;
        this.future = future;

        this.timeoutFuture = scheduledExecutorService.schedule(() -> {
            session.disconnect();
            future.completeExceptionally(new RuntimeException("No transfer received within " + Constants.TIMEOUT_MS + "ms"));
        }, Constants.TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Kick off the connection
        RequestNetworkSettingsPacket requestNetworkSettingsPacket = new RequestNetworkSettingsPacket();
        requestNetworkSettingsPacket.setProtocolVersion(Constants.BEDROCK_CODEC.getProtocolVersion());
        session.sendPacket(requestNetworkSettingsPacket);
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        BedrockPacketHandler.super.handlePacket(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public void onDisconnect(CharSequence reason) {
        logger.info("Disconnected: " + reason);

        if (!future.isDone()) {
            // Only log the reason if we have not got a transfer packet
            statusCallback.accept("Disconnected from server: " + reason);

            timeoutFuture.cancel(true);
            future.completeExceptionally(new RuntimeException("Disconnected from server before receiving transfer packet"));
        }
    }

    @Override
    public PacketSignal handle(DisconnectPacket packet) {
        onDisconnect(packet.getReason().toString());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(NetworkSettingsPacket packet) {
        // Set the compression from the session
        if (packet.getCompressionThreshold() > 0) {
            session.setCompression(packet.getCompressionAlgorithm());
        } else {
            session.setCompression(PacketCompressionAlgorithm.NONE);
        }

        try {
            LoginPacket loginPacket = new LoginPacket();
            loginPacket.setProtocolVersion(Constants.BEDROCK_CODEC.getProtocolVersion());
            loginPacket.setClientJwt(ForgeryUtils.forgeOnlineSkinData(authManager.getManager(), new JSONObject(), netherNetAddress));
            loginPacket.setAuthPayload(new TokenPayload(authManager.getManager().getMinecraftMultiplayerToken().getCached().getToken(), AuthType.FULL));
            session.sendPacket(loginPacket);

            return PacketSignal.HANDLED;
        } catch (Exception e) {
            logger.error("Failed to send login packet", e);
            session.close("Failed to authenticate");
            return PacketSignal.HANDLED;
        }
    }

    @Override
    public PacketSignal handle(ServerToClientHandshakePacket packet) {
        session.sendPacket(new ClientToServerHandshakePacket());

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePacksInfoPacket packet) {
        // ResourcePackClientResponsePacket(packIds=[], status=HAVE_ALL_PACKS)
        ResourcePackClientResponsePacket resourcePackClientResponsePacket = new ResourcePackClientResponsePacket();
        resourcePackClientResponsePacket.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        session.sendPacket(resourcePackClientResponsePacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePackStackPacket packet) {
        ResourcePackClientResponsePacket resourcePackClientResponsePacket = new ResourcePackClientResponsePacket();
        resourcePackClientResponsePacket.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        session.sendPacket(resourcePackClientResponsePacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(TransferPacket packet) {
        logger.info("Received transfer packet to " + packet.getAddress() + ":" + packet.getPort());
        session.disconnect();

        statusCallback.accept("Received transfer packet to " + packet.getAddress() + ":" + packet.getPort());

        timeoutFuture.cancel(true);
        future.complete(new InetSocketAddress(packet.getAddress(), packet.getPort()));

        return PacketSignal.HANDLED;
    }
}
