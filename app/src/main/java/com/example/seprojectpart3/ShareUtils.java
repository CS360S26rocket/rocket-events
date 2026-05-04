/*
 * This file defines ShareUtils, a supporting Java class used by the Scene app.
 * It contains Android share-intent helpers for event and ticket sharing.
 * Its functions include shareEvent, buildEventLink, buildEventUri to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;













public class ShareUtils {

    
    private static final String DEEP_LINK_BASE = "https://campusevents.app/event/";

    








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

    



    public static String buildEventLink(String eventId) {
        return DEEP_LINK_BASE + eventId;
    }

    



    public static Uri buildEventUri(String eventId) {
        return Uri.parse(DEEP_LINK_BASE + eventId);
    }
}
