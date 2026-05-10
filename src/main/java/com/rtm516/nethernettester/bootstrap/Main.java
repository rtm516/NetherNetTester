package com.rtm516.nethernettester.bootstrap;

import com.rtm516.nethernettester.NetherNetTester;
import org.slf4j.LoggerFactory;

/**
 * This is just for internal testing of the app
 */
public class Main {

    public static void main(String[] args) {
        Logger logger = new Logger(LoggerFactory.getLogger(Main.class));
        logger.setDebug(true);

//        HttpClient httpClient = Methanol.newBuilder()
//            .version(HttpClient.Version.HTTP_1_1)
//            .followRedirects(HttpClient.Redirect.NORMAL)
//            .requestTimeout(Duration.ofMillis(Constants.TIMEOUT_MS))
//            .build();
//
//        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));

        NetherNetTester tester = new NetherNetTester()
            .logger(logger)
            .targetGamertag("CrimpyLace85127")
            .statusCallback(status -> logger.info("Status: " + status));
//            .httpClient(httpClient)
//            .scheduledExecutorService(scheduledThreadPool);

        tester.start().thenAccept(success -> {
            logger.info("Test completed successfully!");
        }).exceptionally(ex -> {
            logger.error("An error occurred during the test: " + ex.getCause().getMessage());
            return null;
        });
    }
}