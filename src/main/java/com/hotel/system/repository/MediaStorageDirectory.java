package com.hotel.system.repository;

public enum MediaStorageDirectory {
    AVATARS("avatars"),
    SERVICES("services"),
    ROOMS("rooms");

    private final String folderName;

    MediaStorageDirectory(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
