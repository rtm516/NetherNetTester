package com.rtm516.nethernettester;

import com.github.mizosoft.methanol.Methanol;
import com.rtm516.nethernettester.models.AddFriendResponse;
import com.rtm516.nethernettester.models.FollowerResponse;
import com.rtm516.nethernettester.models.SessionHandlesResponse;
import com.rtm516.nethernettester.nethernet.initializer.NetherNetBedrockChannelInitializer;
import com.rtm516.nethernettester.utils.EmptyLogger;
import com.rtm516.nethernettester.utils.FutureUtils;
import com.rtm516.nethernettester.utils.NamedThreadFactory;
import com.rtm516.nethernettester.utils.PingUtil;
import dev.kastle.netty.channel.nethernet.NetherNetChannelFactory;
import dev.kastle.netty.channel.nethernet.config.NetherChannelOption;
import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetClientSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetXboxSignaling;
import dev.kastle.webrtc.PeerConnectionFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NetherNetTester {
    private Logger logger;

    private String target;
    private String targetGamertag;
    private String targetXuid;

    private Consumer<String> statusCallback;

    private HttpClient httpClient;
    private ScheduledExecutorService scheduledExecutorService;

    private AuthManager authManager;

    private boolean usingOwnHttp;
    private boolean usingOwnExecutor;
    private boolean addedFriend;

    public NetherNetTester logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public NetherNetTester targetGamertag(String targetGamertag) {
        if (this.target != null) {
            throw new IllegalStateException("Target already set to " + this.target);
        }
        this.target = "gt(" + URLEncoder.encode(targetGamertag, StandardCharsets.UTF_8).replaceAll("\\+", "%20") + ")";
        this.targetGamertag = targetGamertag;
        return this;
    }

    public NetherNetTester targetXuid(String targetXuid) {
        if (this.target != null) {
            throw new IllegalStateException("Target already set to " + this.target);
        }
        this.target = "xuid(" + targetXuid + ")";
        this.targetXuid = targetXuid;
        return this;
    }

    public NetherNetTester statusCallback(Consumer<String> statusCallback) {
        this.statusCallback = statusCallback;
        return this;
    }

    public NetherNetTester httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public NetherNetTester scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        return this;
    }

    public CompletableFuture<Void> start() {
        if (httpClient == null) {
            usingOwnHttp = true;
            httpClient = Methanol.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .requestTimeout(Duration.ofMillis(Constants.TIMEOUT_MS))
                .build();
        }

        if (scheduledExecutorService == null) {
            usingOwnExecutor = true;
            scheduledExecutorService = Executors.newScheduledThreadPool(5, new NamedThreadFactory("NetherNet Tester Thread"));
        }

        if (logger == null) {
            this.logger = new EmptyLogger();
        }

        if ((targetGamertag == null || targetGamertag.isBlank()) && (targetXuid == null || targetXuid.isBlank())) {
            throw new IllegalStateException("Target must be set");
        }

        return CompletableFuture.runAsync(() -> {}, scheduledExecutorService)
            .thenRun(() -> {
                logger.info("Starting test against " + target);
                statusCallback.accept("Starting test");
            })
            .thenRun(this::setupAuth)
            .thenCompose(v -> resolveFriend())
            .thenApply(friend -> findNethernetSession(friend.xuid))
            .thenCompose(this::connectToSession)
            .thenCompose(this::pingServer)
            .whenComplete((result, ex) -> cleanup());
    }

    private CompletableFuture<Void> pingServer(InetSocketAddress address) {
        return PingUtil.ping(address, Constants.TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .thenAccept(pong -> {
                logger.info("MOTD: " + pong.motd());
                logger.info("Players: " + pong.playerCount() + "/" + pong.maximumPlayerCount());
                statusCallback.accept("Server pinged: " + pong.motd() + " (" + pong.playerCount() + "/" + pong.maximumPlayerCount() + ")");
            });
    }

    private CompletableFuture<FollowerResponse.Person> resolveFriend() {
        return checkFriendsList()
            .thenCompose(friend -> {
                if (friend != null) {
                    logger.info("Target is already a friend");
                    statusCallback.accept("Target is already a friend");
                    return CompletableFuture.completedFuture(friend);
                }

                logger.error("Friend not found");
                statusCallback.accept("User not found in friends list, sending friend request and waiting for acceptance...");
                return sendFriendRequest()
                    .thenCompose(v -> pollForFriendAcceptance())
                    .thenApply(accepted -> {
                        if (accepted == null) {
                            throw new RuntimeException("Friend request not accepted in time");
                        }

                        addedFriend = true;

                        logger.info("Friend request accepted");
                        statusCallback.accept("Friend request accepted");
                        return accepted;
                    });
            });
    }

    private SessionHandlesResponse.Connection findNethernetSession(String xuid) {
        SessionHandlesResponse.Connection nethernetConnection = findNethernetConnection(xuid);
        if (nethernetConnection == null) {
            throw new RuntimeException("Unable to find session or its not a NetherNet connection");
        }

        var typeName = nethernetConnection.ConnectionType() == Constants.ConnectionTypeWebRTC ? "WebRTC" : "JsonRPC";
        logger.info("Found NetherNet session " + nethernetConnection.NetherNetId() + " using " + typeName);
        statusCallback.accept("Located NetherNet connection with ID " + nethernetConnection.NetherNetId() + " using " + typeName);

        return nethernetConnection;
    }

    private CompletableFuture<InetSocketAddress> connectToSession(SessionHandlesResponse.Connection connection) {
        // Setup either WebRTC or JsonRPC signaling depending on the connection type
        NetherNetClientSignaling signaling;
        NetherNetAddress socketAddress;
        if (connection.ConnectionType() == Constants.ConnectionTypeWebRTC) {
            signaling = new NetherNetXboxSignaling(getMCTokenHeader());
            socketAddress = new NetherNetAddress(String.valueOf(connection.NetherNetId()));
        } else if (connection.ConnectionType() == Constants.ConnectionTypeJsonRpc) {
            signaling = new NetherNetXboxRpcSignaling(getMCTokenHeader());
            socketAddress = new NetherNetAddress(String.valueOf(connection.PmsgId()));
        } else {
            return CompletableFuture.failedFuture(
                new RuntimeException("Unsupported connection type: " + connection.ConnectionType()));
        }

        CompletableFuture<InetSocketAddress> future = new CompletableFuture<>();
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

        // Always shutdown the netty event loop
        future.whenComplete((r, ex) -> eventLoopGroup.shutdownGracefully());

        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup)
            .channelFactory(NetherNetChannelFactory.client(new PeerConnectionFactory(), signaling))
            .option(NetherChannelOption.NETHER_CLIENT_HANDSHAKE_TIMEOUT_MS, Constants.TIMEOUT_MS)
            .handler(new NetherNetBedrockChannelInitializer<LoggingBedrockClientSession>() {
                @Override
                protected LoggingBedrockClientSession createSession0(BedrockPeer peer, int subClientId) {
                    return new LoggingBedrockClientSession(peer, subClientId, logger);
                }

                @Override
                protected void initSession(LoggingBedrockClientSession session) {
                    statusCallback.accept("Connected to NetherNet session");
                    logger.debug("Session initialized: " + session);
                    session.setCodec(Constants.BEDROCK_CODEC);
//                    session.setLogging(true);
                    session.setPacketHandler(new SessionPacketHandler(session, logger, authManager, socketAddress, statusCallback, future, scheduledExecutorService));
                }
            });

        // Connect without blocking
        b.connect(socketAddress).addListener((ChannelFuture channelFuture) -> {
            if (!channelFuture.isSuccess()) {
                future.completeExceptionally(new RuntimeException("Failed to connect: " + channelFuture.cause().getMessage(), channelFuture.cause()));
            }
        });

        return future;
    }

    private void cleanup() {
        if (addedFriend) {
            logger.info("Removing target as friend");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://social.xboxlive.com/users/me/people/friends/v2/" + target))
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "7")
                .header("accept-language", "en-GB")
                .DELETE()
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if  (response.statusCode() != 200) {
                        logger.warn("Failed to delete target as friend: " + response);
                    }
                });
        }

        if (usingOwnHttp) {
            httpClient.close();
        }

        if (usingOwnExecutor) {
            scheduledExecutorService.shutdownNow();
        }
    }

    private SessionHandlesResponse.Connection findNethernetConnection(String xboxFriendXuid) {
        logger.info("Getting sessions...");
        HttpRequest sessionHandlesRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://sessiondirectory.xboxlive.com/handles/query?include=relatedInfo,customProperties"))
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "107")
            .header("accept-language", "en-GB")
            .POST(HttpRequest.BodyPublishers.ofString("{\"owners\":{\"people\":{\"moniker\":\"people\",\"monikerXuid\":\"" + authManager.getXuid() + "\"}},\"scid\":\"" + Constants.SERVICE_CONFIG_ID + "\",\"type\":\"activity\"}"))
            .build();

        SessionHandlesResponse.SessionHandleResponse foundSession = null;
        try {
            String rawResponse = httpClient.send(sessionHandlesRequest, HttpResponse.BodyHandlers.ofString()).body();

            SessionHandlesResponse sessionHandlesResponse = Constants.GSON.fromJson(rawResponse, SessionHandlesResponse.class);

            foundSession = sessionHandlesResponse.results().stream().filter(session -> session.ownerXuid().equalsIgnoreCase(xboxFriendXuid)).findFirst().orElse(null);

            if (foundSession == null) {
                logger.error("Session not found");
                return null;
            }

            logger.info("Session found");
        } catch (Exception e) {
            logger.error("Failed to get friends", e);
            return null;
        }

        SessionHandlesResponse.Connection nethernetConnection = foundSession.customProperties().SupportedConnections().stream().filter(connection -> connection.ConnectionType() == Constants.ConnectionTypeWebRTC || connection.ConnectionType() == Constants.ConnectionTypeJsonRpc).findFirst().orElse(null);
        if (nethernetConnection == null) {
            logger.error("No nethernet connection found");
            return null;
        }

        return nethernetConnection;
    }

    private CompletableFuture<FollowerResponse.Person> checkFriendsList() {
        logger.info("Getting friends...");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(Constants.FRIENDS)
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "7")
            .header("accept-language", "en-GB")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                FollowerResponse friendsResponse = Constants.GSON.fromJson(response.body(), FollowerResponse.class);
                return friendsResponse.people.stream()
                    .filter(friend -> friend.gamertag.equalsIgnoreCase(targetGamertag) || friend.xuid.equalsIgnoreCase(targetXuid))
                    .findFirst()
                    .orElse(null);
            });
    }

    private CompletableFuture<Void> sendFriendRequest() {
        logger.info("Sending friend request");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://social.xboxlive.com/users/me/people/friends/v2/" + target))
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "7")
            .header("accept-language", "en-GB")
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                AddFriendResponse addFriendResponse = Constants.GSON.fromJson(response.body(), AddFriendResponse.class);
                if (!addFriendResponse.friendRequestSent()) {
                    throw new RuntimeException("Unable to send friend request");
                }
            });
    }

    private CompletableFuture<FollowerResponse.Person> pollForFriendAcceptance() {
        CompletableFuture<FollowerResponse.Person> future = new CompletableFuture<>();

        ScheduledFuture<?> poll = scheduledExecutorService.scheduleAtFixedRate(() -> {
            checkFriendsList().thenAccept(friend -> {
                if (friend != null) {
                    future.complete(friend);
                }
            }).exceptionally(ex -> {
                logger.error("Error polling for friend acceptance", ex);
                return null;
            });
        }, 10, 10, TimeUnit.SECONDS);

        future.orTimeout(120, TimeUnit.SECONDS);
        future.whenComplete((r, ex) -> poll.cancel(false));

        return FutureUtils.withTimeoutMessage(future, "Friend request not accepted within 120s");
    }

    private CompletableFuture<Void> setupAuth() {
        try {
            logger.debug("Doing auth...");

            authManager = new AuthManager(logger);
            authManager.getManager().getXboxLiveXstsToken().getUpToDate();
            authManager.getManager().getMinecraftSession().getUpToDate();
            authManager.getManager().getMinecraftMultiplayerToken().getUpToDate();

            logger.info("Successfully authenticated as " + authManager.getGamertag() + " (" + authManager.getXuid() + ")");

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String getTokenHeader() {
        try {
            return authManager.getManager().getXboxLiveXstsToken().getUpToDate().getAuthorizationHeader();
        } catch (Exception e) {
            logger.error("Failed to get auth header", e);
            return "";
        }
    }

    private String getMCTokenHeader() {
        try {
            return authManager.getManager().getMinecraftSession().getUpToDate().getAuthorizationHeader();
        } catch (Exception e) {
            logger.error("Failed to get auth header", e);
            return "";
        }
    }
}
