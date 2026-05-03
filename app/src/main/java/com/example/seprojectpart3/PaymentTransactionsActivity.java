package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Map;

public class PaymentTransactionsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private final PaymentVerificationRepository paymentRepo = new PaymentVerificationRepository();

    private TextView tvTitle, tvEmpty;
    private ProgressBar progressBar;
    private LinearLayout listPayments, listPaymentStats;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_transactions);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        tvTitle = findViewById(R.id.tvPaymentsTitle);
        tvEmpty = findViewById(R.id.tvEmptyPayments);
        progressBar = findViewById(R.id.progressPayments);
        listPayments = findViewById(R.id.listPayments);
        listPaymentStats = findViewById(R.id.listPaymentStats);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvTitle.setText(eventTitle == null || eventTitle.isEmpty()
                ? "Payments"
                : "Payments - " + eventTitle);

        loadPayments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPayments();
    }

    private void loadPayments() {
        if (eventId == null || eventId.isEmpty()) {
            showEmpty("Event not found.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        listPayments.removeAllViews();
        listPaymentStats.removeAllViews();
        listPaymentStats.setVisibility(View.GONE);

        paymentRepo.getPaymentsForEvent(eventId, new PaymentVerificationRepository.PaymentListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> payments) {
                progressBar.setVisibility(View.GONE);
                if (payments == null || payments.isEmpty()) {
                    showEmpty("No verified mock Paymo payments yet.");
                    return;
                }

                tvEmpty.setVisibility(View.GONE);
                renderAnalytics(payments);
                for (Map<String, Object> payment : payments) {
                    listPayments.addView(paymentCard(payment));
                }
            }

            @Override public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                showEmpty(error);
            }
        });
    }

    private void renderAnalytics(List<Map<String, Object>> payments) {
        long total = 0;
        int verified = 0;
        for (Map<String, Object> payment : payments) {
            if ("verified".equalsIgnoreCase(valueOrDefault(payment, "status", "verified"))) {
                verified++;
                total += longValue(payment.get("amount"));
            }
        }
        long average = verified == 0 ? 0 : Math.round((double) total / verified);
        listPaymentStats.setVisibility(View.VISIBLE);
        listPaymentStats.addView(statCard("Collection", "PKR " + total));
        listPaymentStats.addView(statCard("Verified", String.valueOf(verified)));
        listPaymentStats.addView(statCard("Avg ticket", "PKR " + average));
    }

    private View statCard(String title, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_dark);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.addView(label(title, 10, R.color.text_secondary, false));
        card.addView(label(value, 14, R.color.text_primary, true));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(params);
        return card;
    }

    private View paymentCard(Map<String, Object> payment) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_event_card);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        identity.addView(label(valueOrDefault(payment, "userName", "Campus user"),
                16, R.color.text_primary, true));
        identity.addView(label(valueOrDefault(payment, "userEmail", "No email"),
                12, R.color.text_secondary, false));
        top.addView(identity);

        TextView status = label(valueOrDefault(payment, "status", "verified").toUpperCase(java.util.Locale.US),
                11, R.color.accent_lime, true);
        status.setGravity(android.view.Gravity.CENTER);
        status.setBackgroundResource(R.drawable.bg_chip_success);
        status.setPadding(dp(10), dp(6), dp(10), dp(6));
        top.addView(status);
        card.addView(top);

        card.addView(label("Amount: PKR " + valueOrDefault(payment, "amount", "0")
                        + "  |  Payment verified",
                13, R.color.text_secondary, false));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        return card;
    }

    private void showEmpty(String message) {
        listPayments.removeAllViews();
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return view;
    }

    private String valueOrDefault(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) return fallback;
        String asString = String.valueOf(value);
        return asString.trim().isEmpty() || "null".equals(asString) ? fallback : asString;
    }

    private long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? 0 : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
