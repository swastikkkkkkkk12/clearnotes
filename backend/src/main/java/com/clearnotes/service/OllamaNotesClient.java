package com.clearnotes.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OllamaNotesClient {
    private final ObjectMapper mapper;
    private final URI endpoint;
    private final String model;

    public OllamaNotesClient(ObjectMapper mapper,
                             @Value("${clearnotes.ollama.base-url}") String baseUrl,
                             @Value("${clearnotes.ollama.model}") String model) {
        this.mapper = mapper;
        this.endpoint = URI.create(baseUrl.replaceAll("/$", "") + "/api/chat");
        this.model = model;
    }

    public String extractJson(String prompt, String correction, List<Integer> validSegmentIndexes) {
        try {
            String userPrompt = correction == null ? prompt : prompt + "\n\n" + correction;
            byte[] body = mapper.writeValueAsBytes(Map.of(
                    "model", model,
                    "stream", false,
                    "format", NotesSchema.jsonSchema(validSegmentIndexes),
                    "options", Map.of("temperature", 0),
                    "messages", List.of(
                            Map.of("role", "system", "content", NotesSchema.SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt))));
            HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofMinutes(5).toMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setFixedLengthStreamingMode(body.length);
            try (var output = connection.getOutputStream()) { output.write(body); }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                connection.disconnect();
                throw new IllegalStateException("Ollama returned HTTP " + status + ".");
            }
            try (var input = connection.getInputStream()) {
                ChatResponse response = mapper.readValue(input, ChatResponse.class);
                if (response.message() == null || response.message().content() == null)
                    throw new IllegalStateException("Ollama returned an empty response.");
                return response.message().content();
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not get notes from Ollama.", exception);
        }
    }

    private static class NotesSchema {
        private static final String SYSTEM_PROMPT = "You extract notes only from the supplied transcript. "
                + "Never invent facts or use outside knowledge. Return decisions and actionable tasks only; do not return meeting notes. "
                + "For an unstated owner or due date use JSON null. Use a due date only when the transcript "
                + "states an unambiguous calendar date; otherwise use null. Each note must cite the exact "
                + "segment_index that supports it. Use the exact segment_index from the transcript input. "
                + "Use null for an unstated owner or due date, and for any relative or incomplete date. If there are no notes, return an empty array.";
        private static Map<String, Object> jsonSchema(List<Integer> validSegmentIndexes) {
            return Map.of(
                "type", "object", "additionalProperties", false, "required", List.of("notes"),
                "properties", Map.of("notes", Map.of(
                        "type", "array", "items", Map.of(
                                "type", "object", "additionalProperties", false,
                                "required", List.of("type", "content", "source_segment_index", "owner", "due_date"),
                                "properties", Map.of(
                                        "type", Map.of("type", "string", "enum", List.of("DECISION", "TASK")),
                                        "content", Map.of("type", "string"),
                                        "source_segment_index", Map.of("type", "integer", "enum", validSegmentIndexes),
                                        "owner", Map.of("type", List.of("string", "null")),
                                        "due_date", Map.of("type", List.of("string", "null")))))));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(ChatMessage message) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatMessage(String content) { }
}
