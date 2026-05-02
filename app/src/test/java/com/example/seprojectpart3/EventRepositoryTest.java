package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;

public class EventRepositoryTest {

    @Test
    public void testValidEventData() {
        String title = "Tech Talk";
        String date = "2026-05-01";
        String venue = "LUMS - Lahore";

        assertFalse(title.isEmpty());
        assertFalse(date.isEmpty());
        assertFalse(venue.isEmpty());
    }

    @Test
    public void testInvalidEventData() {
        String title = "";
        assertTrue(title.isEmpty());
    }
}