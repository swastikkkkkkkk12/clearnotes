package com.clearnotes.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class TranscriptionClient {
    private final ObjectMapper mapper;
    private final URI endpoint;

    public TranscriptionClient(ObjectMapper mapper,
                               @Value("${clearnotes.transcription.base-url}") String baseUrl) {
        this.mapper = mapper;
        this.endpoint = URI.create(baseUrl.replaceAll("/$", "") + "/transcribe");
    }

    public TranscriptionResponse transcribe(Path audioPath) {
        String boundary = "----ClearNotes" + UUID.randomUUID();
        String filename = audioPath.getFileName().toString();
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        try {
            HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofMinutes(30).toMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setFixedLengthStreamingMode((long) prefix.length + Files.size(audioPath) + suffix.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(prefix);
                Files.copy(audioPath, output);
                output.write(suffix);
            }

            int status = connection.getResponseCode();
            InputStream responseStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (responseStream == null)
                throw new IllegalStateException("Transcription service returned HTTP " + status);
            try (responseStream) {
                if (status < 200 || status >= 300)
                    throw new IllegalStateException("Transcription service returned HTTP " + status);
                TranscriptionResponse result = mapper.readValue(responseStream, TranscriptionResponse.class);
                if (result.segments() == null)
                    throw new IllegalStateException("Transcription service returned an empty response.");
                return result;
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not send audio to the transcription service.", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TranscriptionResponse(List<TranscriptPart> segments) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TranscriptPart(double start, double end, String text) { }
}
