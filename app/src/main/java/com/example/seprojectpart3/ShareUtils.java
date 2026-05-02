package com.example.seprojectpart3;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Story #46 — Share Event Link (M4, Sprint 5, Day 1)
 *
 * Generates a shareable deep link for an event and opens the Android share sheet.
 * No external dependencies — uses standard Android Intent system.
 *
 * Deep link format: https://campusevents.app/event/{eventId}
 * Fallback:         Plain text with event details if deep links aren't configured
 *
 * Done by day 3. After completion, assist M2 on #43 cron infra
 * or M1 on #42 email edge cases.
 */
public class ShareUtils {

    // Base URL for deep links — update when domain is configured
    private static final String DEEP_LINK_BASE = "https://campusevents.app/event/";

    /**
     * Share an event link via Android share sheet.
     *
     * @param context    Activity context
     * @param eventId    Firestore document ID of the event
     * @param eventTitle Title of the event (for share text)
     * @param eventDate  Formatted date string (e.g. "15 Jan 2026, 6:00 PM")
     * @param eventVenue Venue name
     */
    public static void shareEvent(Context context, String eventId,
                                  String eventTitle, String eventDate,
                                  String eventVenue) {

        String deepLink = DEEP_LINK_BASE + eventId;

        String shareText = "Check out this event!\n\n"
                + eventTitle + "\n"
                + eventDate + "\n"
                + eventVenue + "\n\n"
                + deepLink;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, eventTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        context.startActivity(
                Intent.createChooser(shareIntent, "Share event via")
        );
    }

    /**
     * Build just the deep link URL (without sharing).
     * Useful for copying to clipboard or embedding in notifications.
     */
    public static String buildEventLink(String eventId) {
        return DEEP_LINK_BASE + eventId;
    }

    /**
     * Build a Uri for the deep link — for use with Firebase Dynamic Links
     * or Android App Links in the future.
     */
    public static Uri buildEventUri(String eventId) {
        return Uri.parse(DEEP_LINK_BASE + eventId);
    }
}
