package com.taskmaster.attachment;

public record StoredFile(String filename, String contentType, String relativePath, long sizeBytes) {
}
