package com.rtm516.nethernettester.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public class FutureUtils {
    /**
     * Adds a timeout message to a CompletableFuture.
     * If the future completes exceptionally with a TimeoutException, it will be wrapped in a RuntimeException with the provided message.
     *
     * @param future The CompletableFuture to add the timeout message to
     * @param message The message to use in the RuntimeException if a TimeoutException occurs
     * @return The original CompletableFuture with the timeout message added
     */
    public static <T> CompletableFuture<T> withTimeoutMessage(CompletableFuture<T> future, String message) {
        return future.exceptionallyCompose(ex -> {
            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
            if (cause instanceof TimeoutException) {
                return CompletableFuture.failedFuture(new RuntimeException(message, cause));
            }
            return CompletableFuture.failedFuture(ex);
        });
    }
}
