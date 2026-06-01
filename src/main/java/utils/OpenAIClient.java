package utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles raw HTTP POSTing to OpenAI-compatible REST APIs (like NVIDIA NIM Endpoint).
 * Uses standard java.net.HttpURLConnection.
 */
public class OpenAIClient {

    private final String baseUrl;
    private final String apiKey;

    public OpenAIClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * Send a JSON request body and return the raw JSON response string.
     */
    public String post(String requestBody) throws IOException {
        URL endpoint = new URL(baseUrl);
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(45000); // R1 models can be slow due to CoT thinking

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
