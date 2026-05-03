package com.example.seprojectpart3;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiAssistantRepository {

    public interface AssistantCallback {
        void onSuccess(String answer);
        void onFailure(String error);
    }

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void ask(String userQuestion, String eventContext, AssistantCallback callback) {
        String apiKey = BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
        if (apiKey.isEmpty()) {
            callback.onFailure("Gemini API key is missing. Add GEMINI_API_KEY to local.properties, then rebuild the app.");
            return;
        }

        executor.execute(() -> {
            try {
                String prompt = buildPrompt(userQuestion, eventContext);
                byte[] body = buildRequest(prompt).toString().getBytes(StandardCharsets.UTF_8);
                String lastError = "";

                for (String model : fallbackModels()) {
                    GeminiResponse response = requestModel(model, apiKey, body);
                    if (response.success) {
                        String answer = extractAnswer(response.body);
                        callback.onSuccess(answer.isEmpty()
                                ? "I could not generate an answer from the current event data."
                                : answer);
                        return;
                    }

                    lastError = parseError(response.status, response.body);
                    if (response.status != 503 && response.status != 429 && response.status != 404) {
                        callback.onFailure(lastError);
                        return;
                    }
                }

                callback.onFailure(lastError.isEmpty()
                        ? "Gemini is busy right now. Please try again in a minute."
                        : "Gemini is busy right now. Please try again in a minute.");
            } catch (Exception e) {
                callback.onFailure(e.getMessage() == null
                        ? "Unable to reach Gemini right now."
                        : e.getMessage());
            }
        });
    }

    private GeminiResponse requestModel(String model, String apiKey, byte[] body) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(String.format(ENDPOINT, model));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(body);
            }

            int status = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream(),
                    StandardCharsets.UTF_8));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return new GeminiResponse(status >= 200 && status < 300, status, response.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private List<String> fallbackModels() {
        List<String> models = new ArrayList<>();
        String configured = BuildConfig.GEMINI_MODEL == null || BuildConfig.GEMINI_MODEL.trim().isEmpty()
                ? "gemini-3.1-flash-lite-preview"
                : BuildConfig.GEMINI_MODEL.trim();
        addUnique(models, configured);
        addUnique(models, "gemini-3.1-flash-lite-preview");
        addUnique(models, "gemini-flash-lite-latest");
        addUnique(models, "gemini-2.5-flash-lite");
        addUnique(models, "gemini-2.0-flash");
        return models;
    }

    private void addUnique(List<String> models, String model) {
        if (model != null && !model.trim().isEmpty() && !models.contains(model.trim())) {
            models.add(model.trim());
        }
    }

    private JSONObject buildRequest(String prompt) throws Exception {
        JSONObject root = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        content.put("role", "user");
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        content.put("parts", parts);
        contents.put(content);
        root.put("contents", contents);
        root.put("generationConfig", new JSONObject()
                .put("temperature", 0.4)
                .put("maxOutputTokens", 900));
        return root;
    }

    private String buildPrompt(String userQuestion, String eventContext) {
        return "You are Scene Assistant and AI Event Matchmaker inside a campus events app. "
                + "Use only the event data provided below. "
                + "Help students choose events by their interests, date, category, venue, availability, and budget. "
                + "When asked for a match, rank the best 2 to 3 events and briefly explain why each fits the user's price range, enjoyed activities, and university. "
                + "If the data does not contain an answer, say what is missing and suggest what the user can check in the app. "
                + "Keep answers concise: 2 to 4 plain-text bullet points, no markdown bold formatting, no long paragraphs. "
                + "Always finish the final sentence.\n\n"
                + "EVENT DATA:\n" + eventContext + "\n\n"
                + "USER QUESTION:\n" + userQuestion;
    }

    private String extractAnswer(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return "";
        JSONObject candidate = candidates.optJSONObject(0);
        String finishReason = candidate.optString("finishReason", "");
        JSONObject content = candidate.optJSONObject("content");
        if (content == null) return "";
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) return "";
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part != null) answer.append(part.optString("text", ""));
        }
        String text = answer.toString().trim();
        if ("MAX_TOKENS".equals(finishReason) && !text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            return text + "\n\nThe answer was cut short. Ask again with a narrower date, category, or budget.";
        }
        return text;
    }

    private String parseError(int status, String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject error = root.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "Gemini request failed.");
                String code = error.optString("status", "");
                return code.isEmpty()
                        ? "Gemini error " + status + ": " + message
                        : "Gemini error " + status + " (" + code + "): " + message;
            }
        } catch (Exception ignored) {
        }
        return "Gemini error " + status + ". Please check your internet connection and try again.";
    }

    private static class GeminiResponse {
        final boolean success;
        final int status;
        final String body;

        GeminiResponse(boolean success, int status, String body) {
            this.success = success;
            this.status = status;
            this.body = body;
        }
    }
}
