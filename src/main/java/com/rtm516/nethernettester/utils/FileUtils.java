package com.rtm516.nethernettester.utils;

import com.rtm516.nethernettester.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    public static String read(String file) throws IOException {
        Path filePath = Paths.get(Constants.STORAGE_FOLDER, file);
        if (!Files.exists(filePath)) {
            return "";
        }
        return Files.readString(filePath);
    }

    public static void write(String file, String data) throws IOException {
        Path filePath = Paths.get(Constants.STORAGE_FOLDER, file);
        // Cleanup the file if the data is empty
        if (data == null || data.isBlank()) {
            Files.deleteIfExists(filePath);
            return;
        }

        Files.writeString(filePath, data);
    }
}
