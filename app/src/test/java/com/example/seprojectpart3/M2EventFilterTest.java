package com.example.seprojectpart3;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class M2EventFilterTest {

    @Test
    public void keywordSearchMatchesTitle() {
        Map<String, Object> event = event("AI Workshop", "Learn Firebase", "2026-05-10", true);

        assertTrue(matchesKeyword(event, "ai"));
    }

    @Test
    public void keywordSearchMatchesDescription() {
        Map<String, Object> event = event("Tech Talk", "A session about campus startups", "2026-05-10", true);

        assertTrue(matchesKeyword(event, "startup"));
    }

    @Test
    public void keywordSearchRejectsUnmatchedText() {
        Map<String, Object> event = event("Sports Gala", "Football and cricket", "2026-05-10", true);

        assertFalse(matchesKeyword(event, "music"));
    }

    @Test
    public void dateRangeAcceptsEventInsideRange() {
        Map<String, Object> event = event("Concert", "Music night", "2026-05-15", false);

        assertTrue(isInsideDateRange(event, "2026-05-01", "2026-05-31"));
    }

    @Test
    public void dateRangeRejectsEventOutsideRange() {
        Map<String, Object> event = event("Concert", "Music night", "2026-06-15", false);

        assertFalse(isInsideDateRange(event, "2026-05-01", "2026-05-31"));
    }

    @Test
    public void priceFilterSeparatesFreeAndPaidEvents() {
        Map<String, Object> freeEvent = event("Open Day", "Free campus event", "2026-05-10", true);
        Map<String, Object> paidEvent = event("Qawwali Night", "Paid ticket event", "2026-05-10", false);

        assertTrue(isFreeEvent(freeEvent));
        assertFalse(isFreeEvent(paidEvent));
    }

    private Map<String, Object> event(String title, String description, String date, boolean isFree) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("description", description);
        event.put("date", date);
        event.put("isFree", isFree);
        return event;
    }

    private boolean matchesKeyword(Map<String, Object> event, String keyword) {
        String kw = keyword.toLowerCase();
        String title = String.valueOf(event.get("title")).toLowerCase();
        String description = String.valueOf(event.get("description")).toLowerCase();
        return title.contains(kw) || description.contains(kw);
    }

    private boolean isInsideDateRange(Map<String, Object> event, String startDate, String endDate) {
        String date = String.valueOf(event.get("date"));
        return date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0;
    }

    private boolean isFreeEvent(Map<String, Object> event) {
        return Boolean.TRUE.equals(event.get("isFree"));
    }
}
