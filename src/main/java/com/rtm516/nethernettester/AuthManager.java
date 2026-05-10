package com.rtm516.nethernettester;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtm516.nethernettester.exceptions.AgeVerificationException;
import com.rtm516.nethernettester.models.CachedProfileInfo;
import com.rtm516.nethernettester.models.XblUsersMeProfileRequest;
import com.rtm516.nethernettester.utils.FileUtils;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.util.holder.Holder;
import net.raphimc.minecraftauth.util.http.exception.InformativeHttpRequestException;

import java.io.IOException;
import java.util.function.Consumer;

public class AuthManager {
    private final Logger logger;

    private BedrockAuthManager authManager;

    private final Holder<CachedProfileInfo> profileInfo;

    /**
     * Create an instance of AuthManager
     *
     * @param logger              The logger to use for outputting messages
     */
    public AuthManager(Logger logger) {
        this.logger = logger.prefixed("Auth");

        this.authManager = null;
        this.profileInfo = new Holder<>(() -> {
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            XblUsersMeProfileRequest.Response response = httpClient.executeAndHandle(new XblUsersMeProfileRequest(authManager.getXboxLiveXstsToken().getUpToDate()));
            XblUsersMeProfileRequest.Response.ProfileUser profileUser = response.profileUsers().get(0);
            return new CachedProfileInfo(profileUser.settings().get("Gamertag"), profileUser.id());
        });
    }

    /**
     * Follow the auth flow to get the Xbox token and store it
     */
    private void initialise() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();

        // Try to load xboxToken from cache.json if is not already loaded
        if (authManager == null) {
            try {
                String cacheData = FileUtils.read("cache.json");
                if (!cacheData.isBlank()) {
                    JsonObject json = JsonParser.parseString(cacheData).getAsJsonObject();
                    if (json != null) {
                        authManager = BedrockAuthManager.fromJson(httpClient, Constants.BEDROCK_CODEC.getMinecraftVersion(), json);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load cache.json", e);
            }
        }

        try {
            // Login if not already loaded
            if (authManager == null) {
                // Explicitly define the callback to assist type inference for the generic T
                Consumer<MsaDeviceCode> deviceCodeCallback = msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + "?otc=" + msaDeviceCode.getUserCode() + " to authenticate.");
                };

                authManager = BedrockAuthManager.create(httpClient, Constants.BEDROCK_CODEC.getMinecraftVersion())
                        .login(DeviceCodeMsaAuthService::new, deviceCodeCallback);
            }

            // Ensure tokens are fresh
            refreshTokens();

            // Set up listener for saving
            // Explicitly cast to BasicChangeListener to resolve ambiguity with Runnable
            authManager.getChangeListeners().add(this::saveToCache);
            saveToCache();

        } catch (Exception e) {
            // Dont log age verification errors as they are handled elsewhere
            if (e instanceof AgeVerificationException) {
                return;
            }

            logger.error("Failed to get/refresh auth token", e);
        }
    }

    private void refreshTokens() throws IOException, AgeVerificationException {
        try {
            // Requesting up-to-date tokens will automatically refresh them if expired
            authManager.getXboxLiveXstsToken().getUpToDate();
            authManager.getPlayFabToken().getUpToDate();
            profileInfo.getUpToDate();
        } catch (InformativeHttpRequestException e) {
            if (e.getMessage().contains("agecheck")) {
                throw new AgeVerificationException("Authentication failed due to age verification requirement", e);
            } else {
                throw e; // Rethrow if it's a different error
            }
        }
    }

    private void saveToCache() {
        try {
            FileUtils.write("cache.json", BedrockAuthManager.toJson(authManager).toString());
        } catch (Exception e) {
            logger.error("Failed to save auth cache", e);
        }
    }

    /**
     * Get the authenticated BedrockAuthManager.
     * Initializes the manager if it hasn't been already.
     *
     * @return The BedrockAuthManager
     */
    public BedrockAuthManager getManager() {
        if (authManager == null) {
            initialise();
        }

        try {
            // Ensure we have fresh tokens
            refreshTokens();
        } catch (IOException e) {
            logger.error("Failed to refresh tokens", e);
            // Try to re-initialize (force login if refresh failed fatally)
            initialise();
        }
        return authManager;
    }

    /**
     * Get the Gamertag of the current user
     *
     * @return The Gamertag of the current user
     */
    public String getGamertag() {
        return profileInfo.getCached().gamertag();
    }

    /**
     * Get the XUID of the current user
     *
     * @return The XUID of the current user
     */
    public String getXuid() {
        return profileInfo.getCached().xuid();
    }
}