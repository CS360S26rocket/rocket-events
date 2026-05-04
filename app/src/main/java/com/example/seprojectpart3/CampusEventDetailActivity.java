/*
 * This file defines CampusEventDetailActivity, an Android activity used by the Scene app.
 * It contains the campus event details, ticket quantity, tier selection, and mock payment flow.
 * Its functions include onCreate, bindViews, bindActions, loadEvent to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class CampusEventDetailActivity extends AppCompatActivity {

    private final EventRepository eventRepo = new EventRepository();
    private final TicketRepository ticketRepo = new TicketRepository();
    private final MockPaymentRepository mockPaymentRepo = new MockPaymentRepository();

    private TextView tvTitle, tvHeroDate, tvMeta, tvDescription, tvCapacity, tvMessage, tvQuantity;
    private ImageView ivDetailBanner;
    private LinearLayout listTicketTypes, rowQuantity, sectionMockPayment;
    private EditText etTransactionId;
    private Button btnRegister, btnVerifyTransaction;

    private String eventId, uid, email = "", name = "Campus User";
    private boolean selectedEventIsFree = true;
    private boolean selectedEventIsExternal = false;
    private String externalTicketLink = "";
    private String selectedTicketTypeId = null;
    private int selectedQuantity = 1;
    private double selectedUnitPrice = 0;
    private String selectedTicketName = "Ticket";
    private final List<Map<String, Object>> paidTicketTypes = new ArrayList<>();
    private final List<View> paidTicketViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_event_detail);

        eventId = getIntent().getStringExtra("eventId");
        uid = getIntent().getStringExtra("uid");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if ((uid == null || uid.isEmpty()) && user != null) uid = user.getUid();
        if (user != null) {
            email = user.getEmail() == null ? "" : user.getEmail();
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                name = user.getDisplayName();
            }
        }

        bindViews();
        bindActions();
        loadEvent();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvHeroDate = findViewById(R.id.tvHeroDate);
        ivDetailBanner = findViewById(R.id.ivDetailBanner);
        tvMeta = findViewById(R.id.tvDetailMeta);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvCapacity = findViewById(R.id.tvDetailCapacity);
        tvMessage = findViewById(R.id.tvDetailMessage);
        tvQuantity = findViewById(R.id.tvQuantity);
        listTicketTypes = findViewById(R.id.listTicketTypes);
        rowQuantity = findViewById(R.id.rowQuantity);
        sectionMockPayment = findViewById(R.id.sectionMockPayment);
        etTransactionId = findViewById(R.id.etTransactionId);
        btnRegister = findViewById(R.id.btnRegister);
        btnVerifyTransaction = findViewById(R.id.btnVerifyTransaction);
    }

    private void bindActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDecreaseQty).setOnClickListener(v -> changeQuantity(-1));
        findViewById(R.id.btnIncreaseQty).setOnClickListener(v -> changeQuantity(1));
        btnRegister.setOnClickListener(v -> registerForEvent());
        btnVerifyTransaction.setOnClickListener(v -> verifyMockTransaction());
    }

    private void loadEvent() {
        if (eventId == null || eventId.isEmpty()) {
            showMessage("Event not found.");
            return;
        }

        eventRepo.getEventDetail(eventId, new EventRepository.EventDetailCallback() {
            @Override public void onSuccess(Map<String, Object> event) {
                selectedEventIsFree = Boolean.TRUE.equals(event.get("isFree"));
                selectedEventIsExternal = "external_link".equals(value(event, "ticketMode"))
                        || Boolean.TRUE.equals(event.get("externalLinkEnabled"));
                externalTicketLink = valueOrDefault(event, "externalTicketLink", "");
                renderEvent(event);
                renderTicketTypes(event);
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void renderEvent(Map<String, Object> event) {
        tvTitle.setText(value(event, "title"));
        tvHeroDate.setText(compactDate(value(event, "date")));
        tvMeta.setText(value(event, "date") + "  |  " + value(event, "venue"));
        tvDescription.setText(valueOrDefault(event, "description", "No description added yet."));
        loadBanner(value(event, "bannerImageUrl"));
        String paymentLine = selectedEventIsExternal ? "Registration: External link" :
                "Payment QR: " + (selectedEventIsFree ? "Not required"
                : valueOrDefault(event, "paymentQrUrl", "Organizer has not uploaded it yet"));
        tvCapacity.setText("Capacity: " + valueOrDefault(event, "capacity", "No limit")
                + "\nFee: " + valueOrDefault(event, "priceSummary",
                selectedEventIsExternal ? "External registration" : (selectedEventIsFree ? "Free RSVP" : "At checkout"))
                + "\n" + paymentLine);
    }

    @SuppressWarnings("unchecked")
    private void renderTicketTypes(Map<String, Object> event) {
        listTicketTypes.removeAllViews();
        paidTicketTypes.clear();
        paidTicketViews.clear();
        if (selectedEventIsExternal) {
            selectedTicketTypeId = "external_registration";
            selectedUnitPrice = 0;
            selectedTicketName = "External registration";
            rowQuantity.setVisibility(View.GONE);
            sectionMockPayment.setVisibility(View.GONE);
            btnRegister.setText("External Registration");
            listTicketTypes.addView(info("This event uses external registration. The app will track interest, but the outside link flow is not enabled yet."));
            return;
        }
        if (selectedEventIsFree) {
            selectedTicketTypeId = "free_rsvp";
            selectedUnitPrice = 0;
            selectedTicketName = "Free RSVP";
            rowQuantity.setVisibility(View.GONE);
            btnRegister.setText("Register Free");
            sectionMockPayment.setVisibility(View.GONE);
            return;
        }

        btnRegister.setText("Reserve Ticket");
        rowQuantity.setVisibility(View.VISIBLE);
        sectionMockPayment.setVisibility(View.VISIBLE);

        Object raw = event.get("ticketTypes");
        List<Map<String, Object>> ticketTypes = raw instanceof List
                ? (List<Map<String, Object>>) raw : null;

        if (ticketTypes == null || ticketTypes.isEmpty()) {
            listTicketTypes.addView(info("No ticket tiers configured yet."));
            return;
        }

        for (Map<String, Object> tier : ticketTypes) {
            View card = ticketTierCard(tier);
            paidTicketTypes.add(tier);
            paidTicketViews.add(card);
            listTicketTypes.addView(card);
            if (selectedTicketTypeId == null) selectTicketTier(tier, card);
        }
        updateOrderSummary();
    }

    private View ticketTierCard(Map<String, Object> tier) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_ticket_item);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        col.addView(label(valueOrDefault(tier, "name", value(tier, "typeId")),
                15, R.color.text_primary, true));
        col.addView(label("PKR " + money(doubleValue(tier.get("price")))
                        + "  |  " + availableCount(tier) + " available",
                12, R.color.text_secondary, false));
        card.addView(col);

        TextView select = label("Select", 11, R.color.accent_lime, true);
        select.setTag("selectLabel");
        select.setGravity(android.view.Gravity.CENTER);
        select.setBackgroundResource(R.drawable.bg_chip_success);
        select.setPadding(dp(10), dp(6), dp(10), dp(6));
        card.addView(select);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        card.setOnClickListener(v -> selectTicketTier(tier, card));
        return card;
    }

    private void selectTicketTier(Map<String, Object> tier, View selectedCard) {
        selectedTicketTypeId = value(tier, "typeId");
        selectedTicketName = valueOrDefault(tier, "name", selectedTicketTypeId);
        selectedUnitPrice = doubleValue(tier.get("price"));
        for (int i = 0; i < listTicketTypes.getChildCount(); i++) {
            View child = listTicketTypes.getChildAt(i);
            child.setBackgroundResource(R.drawable.bg_ticket_item);
            TextView label = child.findViewWithTag("selectLabel");
            if (label != null) label.setText("Select");
        }
        selectedCard.setBackgroundResource(R.drawable.bg_card_dark);
        TextView label = selectedCard.findViewWithTag("selectLabel");
        if (label != null) label.setText("Selected");
        updateOrderSummary();
    }

    private void registerForEvent() {
        if (!hasUser()) return;
        if (selectedEventIsExternal) {
            eventRepo.incrementExternalLinkClick(eventId, new EventRepository.EventCallback() {
                @Override public void onSuccess(String id) {
                    showMessage(externalTicketLink.isEmpty()
                            ? "External registration is marked for this event. The link will be added later."
                            : "External registration interest recorded. Link opening will be enabled later.");
                }
                @Override public void onFailure(String error) {
                    showMessage("External registration will be available soon.");
                }
            });
            return;
        }
        if (!selectedEventIsFree && selectedTicketTypeId == null) {
            showMessage("Select a ticket tier first.");
            return;
        }
        if (!selectedEventIsFree) {
            showMessage("Use payment verification to complete this ticket.");
            return;
        }

        completeRegistration("Registration confirmed",
                "Your RSVP has been confirmed successfully.");
    }

    private void completeRegistration(String successTitle, String successMessage) {
        String tierId = selectedTicketTypeId == null ? "free_rsvp" : selectedTicketTypeId;
        ticketRepo.registerTicketSelection(eventId, uid, name, email,
                tierId, selectedQuantity, selectedEventIsFree,
                new TicketRepository.RegistrationCallback() {
                    @Override public void onConfirmed(String registrationId) {
                        openSuccess(successTitle, successMessage);
                    }

                    @Override public void onWaitlisted(String waitlistId) {
                        openSuccess("Added to waitlist",
                                "The event is full, so you have been added to the waitlist.");
                    }

                    @Override public void onFailure(String error) {
                        showMessage(error);
                    }
                });
    }

    private void verifyMockTransaction() {
        if (!hasUser()) return;
        if (selectedEventIsFree) {
            showMessage("This event is free. No payment verification needed.");
            return;
        }
        if (selectedTicketTypeId == null) {
            showMessage("Select a ticket tier first.");
            return;
        }

        String inputProblem = transactionInputProblem();
        if (!inputProblem.isEmpty()) {
            showMessage(inputProblem);
            return;
        }

        double total = selectedQuantity * selectedUnitPrice;
        mockPaymentRepo.verifyTransaction(etTransactionId.getText().toString(), eventId, uid, name, email, total,
                new MockPaymentRepository.VerificationCallback() {
                    @Override public void onVerified(String transactionId) {
                        completeRegistration("Payment verified",
                                "Your ticket has been reserved successfully.");
                    }

                    @Override public void onFailure(String error) {
                        showMessage(error);
                    }
                });
    }

    private String transactionInputProblem() {
        String raw = etTransactionId.getText().toString();
        String normalized = raw == null ? "" : raw.trim().toUpperCase(java.util.Locale.US).replace(" ", "");
        long expected = Math.round(selectedQuantity * selectedUnitPrice);
        if (normalized.isEmpty()) {
            return "Enter the payment code from your Paymo receipt.";
        }

        String[] parts = normalized.split("-");
        if (parts.length != 3) {
            return "This payment code format is not accepted.";
        }
        if (!"PAYMO".equals(parts[0]) && !"STRIPE".equals(parts[0]) && !"MOCK".equals(parts[0])) {
            return "This payment provider is not supported.";
        }
        if (!"OK".equals(parts[2])) {
            return "This payment has not been marked successful by the provider.";
        }
        long enteredAmount;
        try {
            enteredAmount = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return "The payment amount inside this code is invalid.";
        }
        if (enteredAmount != expected) {
            return "Amount mismatch.\nSelected tier: " + selectedTicketName
                    + "\nExpected amount: PKR " + expected
                    + "\nCode amount: PKR " + enteredAmount;
        }
        return "";
    }

    private void changeQuantity(int delta) {
        selectedQuantity += delta;
        if (selectedQuantity < 1) selectedQuantity = 1;
        if (selectedQuantity > 10) selectedQuantity = 10;
        tvQuantity.setText(String.valueOf(selectedQuantity));
        updateOrderSummary();
    }

    private void updateOrderSummary() {
        if (selectedEventIsFree) return;
        showMessage("Selected tier: " + selectedTicketName
                + "\nOrder: " + selectedQuantity + " ticket(s) | PKR "
                + money(selectedQuantity * selectedUnitPrice)
                + "\nEnter the payment code from your Paymo receipt to confirm.");
    }

    private void openSuccess(String title, String message) {
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("uid", uid);
        startActivity(intent);
        finish();
    }

    private boolean hasUser() {
        if (uid == null || uid.isEmpty()) {
            showMessage("Please log in again.");
            return false;
        }
        return true;
    }

    private TextView info(String text) {
        TextView view = label(text, 13, R.color.text_primary, false);
        view.setBackgroundResource(R.drawable.bg_card_dark);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        return view;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return view;
    }

    private void loadBanner(String bannerUrl) {
        if (bannerUrl == null || bannerUrl.trim().isEmpty()
                || "-".equals(bannerUrl) || "null".equals(bannerUrl)) {
            ivDetailBanner.setVisibility(View.GONE);
            return;
        }
        ivDetailBanner.setVisibility(View.VISIBLE);
        Glide.with(this).load(bannerUrl).centerCrop().into(ivDetailBanner);
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

    private String compactDate(String rawDate) {
        if (rawDate == null || rawDate.equals("-") || rawDate.length() < 10) return rawDate;
        return rawDate.substring(5, 10).replace("-", "/");
    }

    private String availableCount(Map<String, Object> tier) {
        long quantity = longValue(tier.get("quantity"));
        long sold = longValue(tier.get("sold"));
        if (quantity <= 0) return "No limit";
        return String.valueOf(Math.max(0, quantity - sold));
    }

    private long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? 0 : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double doubleValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value == null ? 0 : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String money(double amount) {
        if (amount == Math.floor(amount)) return String.valueOf((long) amount);
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showMessage(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }
}
