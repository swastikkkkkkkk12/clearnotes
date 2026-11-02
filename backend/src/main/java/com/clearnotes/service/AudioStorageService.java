package com.clearnotes.service;

import com.clearnotes.exception.AudioStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class AudioStorageService {
    private static final Logger log = LoggerFactory.getLogger(AudioStorageService.class);
    private final Path uploadDirectory;

    public AudioStorageService(@Value("${clearnotes.storage.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String extension) {
        String storedName = UUID.randomUUID() + extension;
        Path target = resolve(storedName);
        try {
            Files.createDirectories(uploadDirectory);
            try (var input = file.getInputStream()) { Files.copy(input, target); }
            return storedName;
        } catch (IOException exception) {
            delete(storedName);
            throw new AudioStorageException("Could not store the uploaded audio file.", exception);
        }
    }

    public boolean delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
            return true;
        } catch (IOException exception) {
            log.warn("Could not clean up stored upload {}", storedName, exception);
            return false;
        }
    }

    public Path pathFor(String storedName) { return resolve(storedName); }

    private Path resolve(String storedName) {
        Path target = uploadDirectory.resolve(storedName).normalize();
        if (!target.getParent().equals(uploadDirectory)) throw new IllegalArgumentException("Invalid storage key.");
        return target;
    }
}
