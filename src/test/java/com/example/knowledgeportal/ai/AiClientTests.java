package com.example.knowledgeportal.ai;

import com.example.knowledgeportal.exception.AiProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class AiClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Verify Gemini configuration getters and configured state")
    void testGeminiConfigurationGetters() {
        AiClient client = new AiClient("GEMINI", "valid-key-xyz", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", 8000);

        assertTrue(client.isConfigured(), "Client must report configured when non-empty key is provided.");
        assertEquals("GEMINI", client.getProvider());
        assertEquals("gemini-2.5-flash", client.getModelName());
        assertEquals(8000, client.getTimeoutMs());
    }

    @Test
    @DisplayName("Verify missing or blank API key is unconfigured and throws IllegalStateException")
    void testMissingApiKeyHandling() {
        AiClient unconfiguredClient = new AiClient("GEMINI", "", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", 5000);

        assertFalse(unconfiguredClient.isConfigured(), "Client must be unconfigured when key is empty.");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                unconfiguredClient.generateAnswer("What is the leave policy?", Collections.emptyList())
        );
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    @DisplayName("Verify Gemini response parsing extracts candidates and parts correctly")
    void testGeminiSuccessfulResponseParsing() throws IOException, InterruptedException {
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);

        String sampleGeminiResponse = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    { "text": "Employees receive 25 days of paid annual leave." }
                                ],
                                "role": "model"
                            },
                            "finishReason": "STOP",
                            "index": 0
                        }
                    ]
                }
                """;

        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        Mockito.when(mockResponse.body()).thenReturn(sampleGeminiResponse);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        AiClient client = new AiClient("GEMINI", "mock-gemini-key", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", 5000, mockHttpClient, objectMapper);

        EvidenceChunk chunk = new EvidenceChunk(1L, "Leave Policy", 1, 1L, 0, "Section 2", 1, "Regular full-time employees receive 25 days.", 1.0);
        String answer = client.generateAnswer("How many days of leave?", List.of(chunk));

        assertNotNull(answer);
        assertEquals("Employees receive 25 days of paid annual leave.", answer);
    }

    @Test
    @DisplayName("Verify Gemini error response sanitizes and redacts API keys")
    void testGeminiErrorResponseSanitizesApiKey() throws IOException, InterruptedException {
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);

        String rawSecretKey = "secret-super-sensitive-api-key-999";
        String sampleErrorResponse = """
                {
                    "error": {
                        "code": 400,
                        "message": "API key secret-super-sensitive-api-key-999 is invalid or expired.",
                        "status": "INVALID_ARGUMENT"
                    }
                }
                """;

        Mockito.when(mockResponse.statusCode()).thenReturn(400);
        Mockito.when(mockResponse.body()).thenReturn(sampleErrorResponse);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        AiClient client = new AiClient("GEMINI", rawSecretKey, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", 5000, mockHttpClient, objectMapper);

        AiProcessingException ex = assertThrows(AiProcessingException.class, () ->
                client.generateAnswer("What is the leave policy?", Collections.emptyList())
        );

        assertFalse(ex.getMessage().contains(rawSecretKey), "Raw API key must NEVER be leaked in exception message.");
        assertTrue(ex.getMessage().contains("[REDACTED_API_KEY]"), "Error message must redact secret key.");
    }

    @Test
    @DisplayName("Verify Gemini timeout throws AiProcessingException")
    void testGeminiTimeoutHandling() throws IOException, InterruptedException {
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(
                new HttpTimeoutException("Connection timed out after 5000ms")
        );

        AiClient client = new AiClient("GEMINI", "mock-key", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", 5000, mockHttpClient, objectMapper);

        AiProcessingException ex = assertThrows(AiProcessingException.class, () ->
                client.generateAnswer("What is the leave policy?", Collections.emptyList())
        );

        assertTrue(ex.getMessage().toLowerCase().contains("timed out"), "Exception must report timeout condition.");
    }
}
