/*
 * This file defines CampusAiAssistantActivity, an Android activity used by the Scene app.
 * It contains the AI event assistant chat screen and recommendation flow.
 * Its functions include onCreate, loadEvents, onSuccess, onFailure to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CampusAiAssistantActivity extends AppCompatActivity {

    private final EventRepository eventRepo = new EventRepository();
    private final GeminiAssistantRepository assistantRepo = new GeminiAssistantRepository();

    private LinearLayout listMessages;
    private EditText etQuestion;
    private TextView tvStatus;
    private final List<Map<String, Object>> events = new ArrayList<>();
    private boolean eventsLoaded = false;
    private int matchmakerStep = 0;
    private String matchPriceRange = "";
    private String matchActivities = "";
    private String matchUniversity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_ai_assistant);

        listMessages = findViewById(R.id.listMessages);
        etQuestion = findViewById(R.id.etQuestion);
        tvStatus = findViewById(R.id.tvAssistantStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> sendQuestion());
        findViewById(R.id.chipSuggest).setOnClickListener(v ->
                askQuickQuestion("Suggest the best event for me under PKR 500."));
        findViewById(R.id.chipMatchmaker).setOnClickListener(v ->
                startMatchmaker());
        findViewById(R.id.chipWeekend).setOnClickListener(v ->
                askQuickQuestion("Which events are happening soonest?"));
        findViewById(R.id.chipSummarize).setOnClickListener(v ->
                askQuickQuestion("Summarize the available events by category."));

        etQuestion.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendQuestion();
                return true;
            }
            return false;
        });

        addAssistantMessage("Would you like event recommendations picked for you? Reply yes, or tap Find my match. I will ask for your price range, activities you enjoy, and university.");
        loadEvents();
    }

    private void loadEvents() {
        tvStatus.setText("Loading event knowledge...");
        eventRepo.getUpcomingEvents(new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> loadedEvents) {
                events.clear();
                if (loadedEvents != null) events.addAll(loadedEvents);
                eventsLoaded = true;
                tvStatus.setText(events.isEmpty()
                        ? "No events found yet."
                        : "Ready with " + events.size() + " events.");
            }

            @Override public void onFailure(String error) {
                eventsLoaded = true;
                tvStatus.setText("Could not load events: " + error);
            }
        });
    }

    private void askQuickQuestion(String question) {
        matchmakerStep = 0;
        etQuestion.setText(question);
        sendQuestion();
    }

    private void startMatchmaker() {
        matchmakerStep = 1;
        matchPriceRange = "";
        matchActivities = "";
        matchUniversity = "";
        addAssistantMessage("Great. What price range works for you? Example: free, under PKR 500, or PKR 300 to 1000.");
        etQuestion.setHint("Enter price range...");
    }

    private void sendQuestion() {
        String question = etQuestion.getText().toString().trim();
        if (question.isEmpty()) return;

        addUserMessage(question);
        etQuestion.setText("");

        if (handleMatchmakerAnswer(question)) {
            return;
        }

        if (!eventsLoaded) {
            addAssistantMessage("I am still loading the event list. Try again in a moment.");
            return;
        }

        tvStatus.setText("Thinking...");
        assistantRepo.ask(question, buildEventContext(), new GeminiAssistantRepository.AssistantCallback() {
            @Override public void onSuccess(String answer) {
                runOnUiThread(() -> {
                    addAssistantMessage(answer);
                    tvStatus.setText("Ready");
                });
            }

            @Override public void onFailure(String error) {
                runOnUiThread(() -> {
                    addAssistantMessage(error);
                    tvStatus.setText(error.startsWith("Gemini API key") ? "Needs setup" : "Could not answer");
                });
            }
        });
    }

    private boolean handleMatchmakerAnswer(String answer) {
        String normalized = answer.trim().toLowerCase(java.util.Locale.US);
        if (matchmakerStep == 0 && (normalized.equals("yes") || normalized.equals("yeah")
                || normalized.equals("y") || normalized.contains("recommend"))) {
            startMatchmaker();
            return true;
        }

        if (matchmakerStep == 1) {
            matchPriceRange = answer.trim();
            matchmakerStep = 2;
            addAssistantMessage("What activities do you enjoy? Example: music, sports, seminars, workshops, performances.");
            etQuestion.setHint("Enter activities...");
            return true;
        }

        if (matchmakerStep == 2) {
            matchActivities = answer.trim();
            matchmakerStep = 3;
            addAssistantMessage("Which university are you from? Example: LUMS, FAST, NUST, IBA.");
            etQuestion.setHint("Enter university...");
            return true;
        }

        if (matchmakerStep == 3) {
            matchUniversity = answer.trim();
            matchmakerStep = 0;
            etQuestion.setHint("Ask about events...");
            askForRecommendation();
            return true;
        }

        return false;
    }

    private void askForRecommendation() {
        if (!eventsLoaded) {
            addAssistantMessage("I am still loading the event list. Try again in a moment.");
            return;
        }

        String question = "Recommend the best events for this student. "
                + "Price range: " + matchPriceRange + ". "
                + "Activities enjoyed: " + matchActivities + ". "
                + "University: " + matchUniversity + ". "
                + "Prioritize matching budget, activity type, and university/institution/venue relevance.";

        tvStatus.setText("Matching events...");
        assistantRepo.ask(question, buildEventContext(), new GeminiAssistantRepository.AssistantCallback() {
            @Override public void onSuccess(String answer) {
                runOnUiThread(() -> {
                    addAssistantMessage(answer);
                    tvStatus.setText("Ready");
                });
            }

            @Override public void onFailure(String error) {
                runOnUiThread(() -> {
                    addAssistantMessage(error);
                    tvStatus.setText(error.startsWith("Gemini API key") ? "Needs setup" : "Could not answer");
                });
            }
        });
    }

    private String buildEventContext() {
        if (events.isEmpty()) return "No upcoming events are available in Firestore right now.";

        StringBuilder builder = new StringBuilder();
        int count = Math.min(events.size(), 30);
        for (int i = 0; i < count; i++) {
            Map<String, Object> event = events.get(i);
            boolean isFree = Boolean.TRUE.equals(event.get("isFree"));
            builder.append(i + 1).append(". ")
                    .append(value(event, "title")).append(" | ")
                    .append("Date: ").append(value(event, "date")).append(" | ")
                    .append("Time: ").append(value(event, "time")).append(" | ")
                    .append("Venue: ").append(value(event, "venue")).append(" | ")
                    .append("University: ").append(valueOrDefault(event, "institutionName",
                            valueOrDefault(event, "institution", "-"))).append(" | ")
                    .append("Category: ").append(normalizedCategory(value(event, "category"))).append(" | ")
                    .append("Price: ").append(valueOrDefault(event, "priceSummary", isFree ? "Free" : "Paid")).append(" | ")
                    .append("Capacity: ").append(value(event, "capacity")).append(" | ")
                    .append("Description: ").append(value(event, "description")).append("\n");
        }
        return builder.toString();
    }

    private void addUserMessage(String text) {
        TextView bubble = messageBubble("You\n" + text, true);
        listMessages.addView(bubble);
    }

    private void addAssistantMessage(String text) {
        TextView bubble = messageBubble("Assistant\n" + text, false);
        listMessages.addView(bubble);
    }

    private TextView messageBubble(String text, boolean user) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(getColor(user ? R.color.button_text_dark : R.color.text_primary));
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackgroundResource(user ? R.drawable.bg_button_primary : R.drawable.bg_event_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        view.setLayoutParams(params);
        return view;
    }

    private String normalizedCategory(String category) {
        if ("campus_events".equals(category)) return "entertainment";
        if ("talks".equals(category)) return "seminars";
        return category;
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private String valueOrDefault(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) return fallback;
        String asString = String.valueOf(value);
        return asString.trim().isEmpty() || "null".equals(asString) ? fallback : asString;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
