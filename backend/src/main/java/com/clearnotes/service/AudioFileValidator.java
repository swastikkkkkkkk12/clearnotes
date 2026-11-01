package com.clearnotes.service;

import com.clearnotes.exception.InvalidAudioFileException;
import com.clearnotes.exception.UploadTooLargeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AudioFileValidator {
    private static final Map<String, String> TYPES = Map.of(
            ".mp3", "audio/mpeg", ".wav", "audio/wav", ".m4a", "audio/mp4",
            ".ogg", "audio/ogg", ".webm", "audio/webm", ".flac", "audio/flac");
    private final long maxBytes;

    public AudioFileValidator(@Value("${clearnotes.upload.max-bytes}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public ValidatedAudio validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidAudioFileException("Choose a non-empty audio file.");
        if (file.getSize() > maxBytes) throw new UploadTooLargeException();
        String original = safeName(file.getOriginalFilename());
        String lower = original.toLowerCase(Locale.ROOT);
        String extension = TYPES.keySet().stream().filter(lower::endsWith).findFirst()
                .orElseThrow(() -> new InvalidAudioFileException("Supported formats are MP3, WAV, M4A, OGG, WebM, and FLAC."));
        String declared = file.getContentType();
        if (declared != null && !declared.isBlank() && !acceptedMime(extension, declared))
            throw new InvalidAudioFileException("The file content type does not match a supported audio format.");
        try (var input = file.getInputStream()) {
            if (!hasValidSignature(extension, input.readNBytes(12)))
                throw new InvalidAudioFileException("The file contents do not match its audio file extension.");
        } catch (IOException exception) {
            throw new InvalidAudioFileException("Could not read the uploaded audio file.");
        }
        return new ValidatedAudio(original, extension, TYPES.get(extension), file.getSize());
    }

    private static String safeName(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidAudioFileException("The uploaded file needs a filename.");
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.isBlank() || name.length() > 255 || name.chars().anyMatch(Character::isISOControl))
            throw new InvalidAudioFileException("The filename is invalid or too long.");
        return name;
    }

    private static boolean acceptedMime(String extension, String rawType) {
        String type = rawType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (type.equals("application/octet-stream")) return true;
        return switch (extension) {
            case ".mp3" -> Set.of("audio/mpeg", "audio/mp3").contains(type);
            case ".wav" -> Set.of("audio/wav", "audio/x-wav", "audio/wave").contains(type);
            case ".m4a" -> Set.of("audio/mp4", "audio/x-m4a", "application/mp4", "video/mp4").contains(type);
            case ".ogg" -> Set.of("audio/ogg", "application/ogg", "audio/vorbis").contains(type);
            case ".webm" -> Set.of("audio/webm", "video/webm").contains(type);
            case ".flac" -> Set.of("audio/flac", "audio/x-flac").contains(type);
            default -> false;
        };
    }

    private static boolean hasValidSignature(String extension, byte[] bytes) {
        return switch (extension) {
            case ".mp3" -> bytes.length >= 3 && ((bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
                    || (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xe0) == 0xe0));
            case ".wav" -> starts(bytes, "RIFF") && bytes.length >= 12 && matches(bytes, 8, "WAVE");
            case ".m4a" -> bytes.length >= 8 && matches(bytes, 4, "ftyp");
            case ".ogg" -> starts(bytes, "OggS");
            case ".webm" -> bytes.length >= 4 && (bytes[0] & 0xff) == 0x1a && (bytes[1] & 0xff) == 0x45
                    && (bytes[2] & 0xff) == 0xdf && (bytes[3] & 0xff) == 0xa3;
            case ".flac" -> starts(bytes, "fLaC");
            default -> false;
        };
    }

    private static boolean starts(byte[] bytes, String value) { return matches(bytes, 0, value); }
    private static boolean matches(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++) if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        return true;
    }

    public record ValidatedAudio(String originalFilename, String extension, String contentType, long size) { }
}
