package com.hotel.system.util;

import java.nio.file.Path;

public record StoredMedia(String publicPath, String fileName, Path absolutePath) {
}
