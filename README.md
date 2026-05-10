# NetherNet Tester
[![License: GPL-3.0](https://img.shields.io/github/license/MCXboxBroadcast/Broadcaster)](LICENSE)
[![Discord](https://img.shields.io/discord/1139621390908133396?label=discord&color=5865F2)](https://discord.gg/Tp3tA2kdCN)

A library for testing NetherNet connectivity to Bedrock servers by authenticating via Xbox Live, resolving a friend's session, and attempting a full NetherNet connection.

The library expects the target to be a server that sends a transfer packet upon connection and will fail if not.

Supports both WebRTC and JsonRPC connection types.

## Tester steps
1. Authenticate with Xbox Live
2. Add the target as a friend if not already
3. Resolve the target's session to get the NetherNet connection details
4. Attempt to connect to the target's session
5. Wait for a transfer packet
6. Ping the transfer packet target address

## Usage
If you want to use this library in your project, add the following to your build.gradle.kts:
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.rtm516:NetherNetTester:latest")
}
```

### Example
```java
NetherNetTester tester = new NetherNetTester()
    .targetGamertag("ExampleGamertag")
    .statusCallback(status -> logger.info("Status: " + status));

tester.start().thenAccept(v -> {
    logger.info("Test completed successfully!");
}).exceptionally(ex -> {
    logger.error("Test failed!", ex);
    return null;
});
```

### Methods
#### Builder
| Method                                                        | Description                                                          |
|---------------------------------------------------------------|----------------------------------------------------------------------|
| `logger(Logger logger)`                                       | Sets the logger instance. Defaults to a no-op logger if not provided |
| `targetGamertag(String gamertag)`                             | Sets the target by Xbox Live gamertag                                |
| `targetXuid(String xuid)`                                     | Sets the target by Xbox Live XUID                                    |
| `statusCallback(Consumer<String> callback)`                   | Receives human-readable status updates throughout the test           |
| `httpClient(HttpClient client)`                               | Sets a custom HTTP client. A default is created if not provided      |
| `scheduledExecutorService(ScheduledExecutorService executor)` | Sets a custom executor. A default is created if not provided         |

Either `targetGamertag` or `targetXuid` must be set before calling `start()`.

#### Lifecycle

| Method    | Returns                   | Description                                                                                                                              |
|-----------|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `start()` | `CompletableFuture<Void>` | Runs the full test. Completes on success, completes exceptionally on failure |
