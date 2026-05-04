/*
 * This file defines CampusHomeActivity, an Android activity used by the Scene app.
 * It contains the campus event discovery home screen, filters, cards, navigation, and assistant entry.
 * Its functions include onCreate, onResume, bindViews, bindActions to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CampusHomeActivity extends AppCompatActivity {

    private final EventRepository eventRepo = new EventRepository();
    private final TicketRepository ticketRepo = new TicketRepository();
    private final NotificationRepository notificationRepo = new NotificationRepository();

    private ScrollView sectionHome, sectionMyEvents, sectionNotifications, sectionProfile;
    private LinearLayout listEvents, listMyEvents, listNotifications;
    private TextView tvCampusTitle, tvCampusSubtitle, tvMessage;
    private TextView navHome, navMyEvents, navNotifications, navProfile;
    private TextView tvProfileInitials, tvProfileName, tvProfileEmail, tvProfileInfo;
    private TextView chipAll, chipEntertainment, chipSeminars, chipSports, chipWorkshops;
    private TextView chipPriceAll, chipFree, chipPaid;
    private EditText etSearch;
    private EditText etProfileName, etProfilePassword;
    private final List<Map<String, Object>> allEvents = new ArrayList<>();
    private String selectedCategory = "all";
    private String selectedPriceFilter = "all";

    private String uid;
    private String email = "";
    private String name = "Campus User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_home);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = getIntent().getStringExtra("uid");
        if ((uid == null || uid.isEmpty()) && user != null) uid = user.getUid();
        if (user != null) {
            email = user.getEmail() == null ? "" : user.getEmail();
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                name = user.getDisplayName();
            }
        }

        bindViews();
        bindActions();
        renderProfile();
        showSection("home");
        loadAllEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sectionMyEvents != null && sectionMyEvents.getVisibility() == View.VISIBLE) {
            loadMyEvents();
        }
        if (sectionNotifications != null && sectionNotifications.getVisibility() == View.VISIBLE) {
            loadNotifications();
        }
    }

    private void bindViews() {
        sectionHome = findViewById(R.id.sectionHome);
        sectionMyEvents = findViewById(R.id.sectionMyEvents);
        sectionNotifications = findViewById(R.id.sectionNotifications);
        sectionProfile = findViewById(R.id.sectionProfile);

        listEvents = findViewById(R.id.listEvents);
        listMyEvents = findViewById(R.id.listMyEvents);
        listNotifications = findViewById(R.id.listNotifications);

        tvCampusTitle = findViewById(R.id.tvCampusTitle);
        tvCampusSubtitle = findViewById(R.id.tvCampusSubtitle);
        tvMessage = findViewById(R.id.tvMessage);

        navHome = findViewById(R.id.navHome);
        navMyEvents = findViewById(R.id.navMyEvents);
        navNotifications = findViewById(R.id.navNotifications);
        navProfile = findViewById(R.id.navProfile);

        tvProfileInitials = findViewById(R.id.tvProfileInitials);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileInfo = findViewById(R.id.tvProfileInfo);
        etProfileName = findViewById(R.id.etProfileName);
        etProfilePassword = findViewById(R.id.etProfilePassword);

        etSearch = findViewById(R.id.etSearch);
        chipAll = findViewById(R.id.chipAll);
        chipEntertainment = findViewById(R.id.chipEntertainment);
        chipSeminars = findViewById(R.id.chipSeminars);
        chipSports = findViewById(R.id.chipSports);
        chipWorkshops = findViewById(R.id.chipWorkshops);
        chipPriceAll = findViewById(R.id.chipPriceAll);
        chipFree = findViewById(R.id.chipFree);
        chipPaid = findViewById(R.id.chipPaid);
    }

    private void bindActions() {
        navHome.setOnClickListener(v -> showSection("home"));
        navMyEvents.setOnClickListener(v -> {
            showSection("my");
            loadMyEvents();
        });
        navNotifications.setOnClickListener(v -> {
            showSection("notifications");
            loadNotifications();
        });
        navProfile.setOnClickListener(v -> showSection("profile"));

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchEvents();
                return true;
            }
            return false;
        });
        chipAll.setOnClickListener(v -> selectCategory("all"));
        chipEntertainment.setOnClickListener(v -> selectCategory("entertainment"));
        chipSeminars.setOnClickListener(v -> selectCategory("seminars"));
        chipSports.setOnClickListener(v -> selectCategory("sports"));
        chipWorkshops.setOnClickListener(v -> selectCategory("workshops"));
        chipPriceAll.setOnClickListener(v -> selectPriceFilter("all"));
        chipFree.setOnClickListener(v -> selectPriceFilter("free"));
        chipPaid.setOnClickListener(v -> selectPriceFilter("paid"));
        findViewById(R.id.btnSortEarliest).setOnClickListener(v -> sortEarliest());

        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnAiAssistant).setOnClickListener(v ->
                startActivity(new Intent(this, CampusAiAssistantActivity.class)));
    }

    private void showSection(String section) {
        sectionHome.setVisibility("home".equals(section) ? View.VISIBLE : View.GONE);
        sectionMyEvents.setVisibility("my".equals(section) ? View.VISIBLE : View.GONE);
        sectionNotifications.setVisibility("notifications".equals(section) ? View.VISIBLE : View.GONE);
        sectionProfile.setVisibility("profile".equals(section) ? View.VISIBLE : View.GONE);

        if ("my".equals(section)) {
            tvCampusTitle.setText("My Events");
            tvCampusSubtitle.setText("Your RSVPs and tickets");
            etSearch.setVisibility(View.GONE);
        } else if ("notifications".equals(section)) {
            tvCampusTitle.setText("Notifications");
            tvCampusSubtitle.setText("Updates from events you registered for");
            etSearch.setVisibility(View.GONE);
        } else if ("profile".equals(section)) {
            tvCampusTitle.setText("My profile");
            tvCampusSubtitle.setText("Account and privacy");
            etSearch.setVisibility(View.GONE);
        } else {
            tvCampusTitle.setText("Events");
            tvCampusSubtitle.setText("Here you'll find all active Scene events");
            etSearch.setVisibility(View.VISIBLE);
        }

        setNavActive(navHome, "home".equals(section));
        setNavActive(navMyEvents, "my".equals(section));
        setNavActive(navNotifications, "notifications".equals(section));
        setNavActive(navProfile, "profile".equals(section));
    }

    private void setNavActive(TextView nav, boolean active) {
        int iconColor = getColor(active ? R.color.text_primary : R.color.text_secondary);
        nav.setTextColor(iconColor);
        nav.setCompoundDrawableTintList(ColorStateList.valueOf(iconColor));
        nav.setBackgroundResource(active ? R.drawable.bg_nav_item_active : 0);
    }

    private void loadAllEvents() {
        eventRepo.getUpcomingEvents(new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                allEvents.clear();
                if (events != null) allEvents.addAll(events);
                applyHomeFilters();
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void searchEvents() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            loadAllEvents();
            return;
        }

        eventRepo.searchEvents(keyword, new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                allEvents.clear();
                if (events != null) allEvents.addAll(events);
                applyHomeFilters();
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        setCategoryActive(chipAll, "all".equals(category));
        setCategoryActive(chipEntertainment, "entertainment".equals(category));
        setCategoryActive(chipSeminars, "seminars".equals(category));
        setCategoryActive(chipSports, "sports".equals(category));
        setCategoryActive(chipWorkshops, "workshops".equals(category));
        applyHomeFilters();
    }

    private void setCategoryActive(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_button_primary : R.drawable.bg_button_ghost);
        chip.setTextColor(getColor(active ? R.color.button_text_dark : R.color.text_primary));
    }

    private void selectPriceFilter(String filter) {
        selectedPriceFilter = filter;
        setCategoryActive(chipPriceAll, "all".equals(filter));
        setCategoryActive(chipFree, "free".equals(filter));
        setCategoryActive(chipPaid, "paid".equals(filter));
        applyHomeFilters();
    }

    private void sortEarliest() {
        Collections.sort(allEvents, (a, b) -> value(a, "date").compareTo(value(b, "date")));
        applyHomeFilters();
    }

    private void applyHomeFilters() {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> event : allEvents) {
            if (matchesCategory(event) && matchesPrice(event)) {
                filtered.add(event);
            }
        }
        renderEvents(filtered);
    }

    private boolean matchesCategory(Map<String, Object> event) {
        if ("all".equals(selectedCategory)) return true;
        String category = value(event, "category");
        if ("entertainment".equals(selectedCategory)) {
            return "entertainment".equals(category) || "campus_events".equals(category);
        }
        if ("seminars".equals(selectedCategory)) {
            return "seminars".equals(category) || "talks".equals(category);
        }
        return selectedCategory.equals(category);
    }

    private boolean matchesPrice(Map<String, Object> event) {
        if ("all".equals(selectedPriceFilter)) return true;
        boolean isFree = Boolean.TRUE.equals(event.get("isFree"));
        return "free".equals(selectedPriceFilter) ? isFree : !isFree;
    }

    private void loadMyEvents() {
        if (!hasUser()) return;

        ticketRepo.getUserEvents(uid, new TicketRepository.UserEventsCallback() {
            @Override public void onSuccess(List<Map<String, Object>> events) {
                listMyEvents.removeAllViews();
                if (events == null || events.isEmpty()) {
                    listMyEvents.addView(makeEmptyCard("Your tickets will show up here",
                            "Explore events and secure your first one."));
                    return;
                }

                for (Map<String, Object> event : events) {
                    listMyEvents.addView(makeMyEventCard(event));
                }
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void loadNotifications() {
        if (!hasUser()) return;

        notificationRepo.getUserNotifications(uid, new NotificationRepository.NotificationListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> notifications) {
                listNotifications.removeAllViews();
                if (notifications == null || notifications.isEmpty()) {
                    listNotifications.addView(makeEmptyCard("No notifications yet",
                            "Event updates will appear here."));
                    return;
                }

                for (Map<String, Object> notification : notifications) {
                    listNotifications.addView(makeNotificationCard(notification));
                }
            }

            @Override public void onFailure(String error) {
                showMessage(error);
            }
        });
    }

    private void renderEvents(List<Map<String, Object>> events) {
        listEvents.removeAllViews();
        if (events == null || events.isEmpty()) {
            listEvents.addView(makeEmptyCard("No matching events found", "Try another search or filter."));
            return;
        }

        for (Map<String, Object> event : events) {
            listEvents.addView(makeEventCard(event));
        }
    }

    private View makeEventCard(Map<String, Object> event) {
        LinearLayout card = baseCard();
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        card.addView(makeBannerHero(value(event, "bannerImageUrl"),
                compactDate(value(event, "date")), dp(130)));

        TextView title = label(value(event, "title"), 17, R.color.text_primary, true);
        title.setPadding(0, dp(12), 0, 0);
        card.addView(title);
        card.addView(label(value(event, "venue"), 13, R.color.text_secondary, false));

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(0, dp(10), 0, 0);
        boolean isFree = Boolean.TRUE.equals(event.get("isFree"));
        chipRow.addView(makeChip(valueOrDefault(event, "category", "Campus Event"),
                R.drawable.bg_icon_button, R.color.text_primary));
        chipRow.addView(makeChip(valueOrDefault(event, "priceSummary", isFree ? "Free RSVP" : "Paid ticket"),
                isFree ? R.drawable.bg_chip_success : R.drawable.bg_chip_warning,
                isFree ? R.color.accent_lime : R.color.warning_gold));
        card.addView(chipRow);

        card.setOnClickListener(v -> openDetail(value(event, "eventId")));
        return card;
    }

    private View makeMyEventCard(Map<String, Object> event) {
        LinearLayout card = baseCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        card.addView(makeBannerThumb(value(event, "bannerImageUrl"),
                initial(value(event, "eventTitle"))));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        col.addView(label(value(event, "eventTitle"), 16, R.color.text_primary, true));
        col.addView(label(value(event, "eventDate") + "  |  " + value(event, "eventVenue"),
                12, R.color.text_secondary, false));
        col.addView(label("Status: " + value(event, "status"),
                12, R.color.accent_lime, false));
        card.addView(col);

        card.setOnClickListener(v -> openDetail(value(event, "eventId")));
        return card;
    }

    private View makeNotificationCard(Map<String, Object> notification) {
        LinearLayout card = baseCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        String type = valueOrDefault(notification, "type", "event_update");
        TextView badge = label(notificationIcon(type), 16, R.color.text_primary, true);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setBackgroundResource("payment_verified".equals(type)
                ? R.drawable.bg_chip_success : R.drawable.bg_button_secondary);
        card.addView(badge, new LinearLayout.LayoutParams(dp(50), dp(50)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        col.addView(label(notificationTypeLabel(type),
                11, "payment_verified".equals(type) ? R.color.accent_lime : R.color.warning_gold, true));
        col.addView(label(valueOrDefault(notification, "title", "Event update"),
                16, R.color.text_primary, true));
        col.addView(label(valueOrDefault(notification, "message",
                        valueOrDefault(notification, "body", "New update available.")),
                13, R.color.text_secondary, false));
        card.addView(col);
        return card;
    }

    private View makeBannerHero(String bannerUrl, String dateChip, int height) {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundResource(R.drawable.bg_hero_media);
        hero.setClipToOutline(true);

        if (hasBanner(bannerUrl)) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setAlpha(0.92f);
            hero.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            Glide.with(this).load(bannerUrl).centerCrop().into(image);
        }

        TextView chip = makeChip(dateChip, R.drawable.bg_chip_warning, R.color.warning_gold);
        FrameLayout.LayoutParams chipParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP | android.view.Gravity.RIGHT);
        chipParams.setMargins(0, dp(12), dp(12), 0);
        hero.addView(chip, chipParams);

        hero.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height));
        return hero;
    }

    private View makeBannerThumb(String bannerUrl, String fallbackInitial) {
        FrameLayout thumb = new FrameLayout(this);
        thumb.setBackgroundResource(R.drawable.bg_icon_button);
        thumb.setClipToOutline(true);

        if (hasBanner(bannerUrl)) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            Glide.with(this).load(bannerUrl).centerCrop().into(image);
        } else {
            TextView avatar = label(fallbackInitial, 18, R.color.accent_lime, true);
            avatar.setGravity(android.view.Gravity.CENTER);
            thumb.addView(avatar, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(58), dp(58));
        params.setMargins(0, 0, dp(12), 0);
        thumb.setLayoutParams(params);
        return thumb;
    }

    private String notificationIcon(String type) {
        if ("payment_verified".equals(type)) return "OK";
        if ("event_cancelled".equals(type)) return "X";
        if ("venue_changed".equals(type)) return "V";
        if ("time_changed".equals(type)) return "T";
        if ("live_update".equals(type)) return "•";
        return "!";
    }

    private String notificationTypeLabel(String type) {
        if ("payment_verified".equals(type)) return "Payment verified";
        if ("event_cancelled".equals(type)) return "Event cancelled";
        if ("venue_changed".equals(type)) return "Venue changed";
        if ("time_changed".equals(type)) return "Time changed";
        if ("live_update".equals(type)) return "Live update";
        return "Event update";
    }

    private TextView makeEmptyCard(String title, String subtitle) {
        TextView card = label(title + "\n" + subtitle, 15, R.color.text_primary, true);
        card.setGravity(android.view.Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_card_dark);
        card.setPadding(dp(18), dp(42), dp(18), dp(42));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(16), 0, dp(12));
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout baseCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_event_card);
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        return card;
    }

    private TextView makeChip(String text, int background, int color) {
        TextView chip = label(text, 11, color, true);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setBackgroundResource(background);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return view;
    }

    private void renderProfile() {
        tvProfileInitials.setText(initials(name));
        tvProfileName.setText(name);
        tvProfileEmail.setText(email.isEmpty() ? "student@email.com" : email);
        etProfileName.setText(name);
        tvProfileInfo.setText("Profile info\n" + name
                + "\n\nAccounts & privacy settings\nCampus user account"
                + "\n\nHelp center\nContact your event organizer"
                + "\n\nAbout Scene\nCampus event discovery and ticketing");
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showMessage("Please log in again.");
            return;
        }

        String updatedName = etProfileName.getText().toString().trim();
        String updatedPassword = etProfilePassword.getText().toString().trim();

        if (updatedName.isEmpty()) {
            showMessage("Name cannot be empty.");
            return;
        }

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(updatedName)
                .build();

        user.updateProfile(request)
                .addOnSuccessListener(v -> {
                    name = updatedName;
                    tvProfileName.setText(name);
                    tvProfileInitials.setText(initials(name));
                    showMessage("Profile name updated.");
                })
                .addOnFailureListener(e -> showMessage(message(e)));

        if (!updatedPassword.isEmpty()) {
            if (updatedPassword.length() < 6) {
                showMessage("Password must be at least 6 characters.");
                return;
            }
            user.updatePassword(updatedPassword)
                    .addOnSuccessListener(v -> {
                        etProfilePassword.setText("");
                        showMessage("Profile and password updated.");
                    })
                    .addOnFailureListener(e -> showMessage("Password update failed. Please log in again and retry."));
        }
    }

    private void openDetail(String eventId) {
        if (eventId == null || eventId.equals("-") || eventId.trim().isEmpty()) {
            showMessage("Event not found.");
            return;
        }
        Intent intent = new Intent(this, CampusEventDetailActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("uid", uid);
        startActivity(intent);
    }

    private boolean hasUser() {
        if (uid == null || uid.isEmpty()) {
            showMessage("Please log in again.");
            return false;
        }
        return true;
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

    private boolean hasBanner(String url) {
        return url != null && !url.trim().isEmpty() && !"-".equals(url) && !"null".equals(url);
    }

    private String compactDate(String rawDate) {
        if (rawDate == null || rawDate.equals("-") || rawDate.length() < 10) return rawDate;
        return rawDate.substring(5, 10).replace("-", "/");
    }

    private String initial(String text) {
        if (text == null || text.trim().isEmpty() || "-".equals(text)) return "E";
        return text.trim().substring(0, 1).toUpperCase(java.util.Locale.US);
    }

    private String initials(String text) {
        if (text == null || text.trim().isEmpty()) return "CU";
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(java.util.Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(java.util.Locale.US);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String message(Exception e) {
        return e == null || e.getMessage() == null ? "Unable to update profile." : e.getMessage();
    }

    private void showMessage(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }
}
