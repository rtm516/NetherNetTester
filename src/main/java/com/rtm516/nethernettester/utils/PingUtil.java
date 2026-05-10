package com.rtm516.nethernettester.utils;

import com.rtm516.nethernettester.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakPing;
import org.cloudburstmc.netty.channel.raknet.RakPong;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PingUtil {

    public static CompletableFuture<BedrockPong> ping(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        CompletableFuture<BedrockPong> future = new CompletableFuture<>();
        future.orTimeout(timeout, timeUnit);

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        future.whenComplete((r, ex) -> eventLoopGroup.shutdownGracefully());

        new Bootstrap()
            .channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
            .group(eventLoopGroup)
            .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong())
            .handler(new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (!(msg instanceof RakPong rakPong)) {
                        super.channelRead(ctx, msg);
                        return;
                    }

                    try {
                        future.complete(BedrockPong.fromRakNet(rakPong.getPongData()));
                    } finally {
                        ReferenceCountUtil.release(rakPong);
                    }

                    ctx.channel().close();
                }
            })
            .bind(0)
            .addListener((ChannelFuture channelFuture) -> {
                if (channelFuture.cause() != null) {
                    future.completeExceptionally(channelFuture.cause());
                    channelFuture.channel().close();
                }

                RakPing ping = new RakPing(System.currentTimeMillis(), server);
                channelFuture.channel().writeAndFlush(ping).addListener(writeFuture -> {
                    if (writeFuture.cause() != null) {
                        future.completeExceptionally(writeFuture.cause());
                        channelFuture.channel().close();
                    }
                });
            });

        return FutureUtils.withTimeoutMessage(future, "Server ping timed out after " + Constants.TIMEOUT_MS + "ms");
    }
}
