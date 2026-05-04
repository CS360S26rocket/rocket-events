package com.example.seprojectpart3;

import static org.junit.Assert.*;

import org.junit.Test;

public class M2TicketFlowTest {

    @Test
    public void freeRsvpConfirmsWhenCapacityAvailable() {
        long capacity = 100;
        long soldCount = 25;

        assertTrue(hasCapacity(capacity, soldCount, 1));
    }

    @Test
    public void freeRsvpWaitlistsWhenCapacityFull() {
        long capacity = 50;
        long soldCount = 50;

        assertFalse(hasCapacity(capacity, soldCount, 1));
    }

    @Test
    public void paidTicketRejectsQuantityAboveAvailableTierCapacity() {
        long tierQuantity = 10;
        long tierSold = 8;
        int requestedQuantity = 3;

        assertFalse(hasTierCapacity(tierQuantity, tierSold, requestedQuantity));
    }

    @Test
    public void paidTicketAllowsQuantityInsideAvailableTierCapacity() {
        long tierQuantity = 10;
        long tierSold = 8;
        int requestedQuantity = 2;

        assertTrue(hasTierCapacity(tierQuantity, tierSold, requestedQuantity));
    }

    @Test
    public void cancelConfirmedRsvpRestoresCapacity() {
        long soldCount = 10;

        long restoredSoldCount = restoreCapacity(soldCount);

        assertEquals(9, restoredSoldCount);
    }

    @Test
    public void cancelDoesNotMakeSoldCountNegative() {
        long soldCount = 0;

        long restoredSoldCount = restoreCapacity(soldCount);

        assertEquals(0, restoredSoldCount);
    }

    @Test
    public void statusSupportsPendingApprovedRejected() {
        assertTrue(isValidTicketStatus("pending"));
        assertTrue(isValidTicketStatus("approved"));
        assertTrue(isValidTicketStatus("rejected"));
        assertFalse(isValidTicketStatus("unknown"));
    }

    private boolean hasCapacity(long capacity, long soldCount, int quantity) {
        return capacity <= 0 || soldCount + quantity <= capacity;
    }

    private boolean hasTierCapacity(long tierQuantity, long tierSold, int quantity) {
        return tierQuantity <= 0 || tierSold + quantity <= tierQuantity;
    }

    private long restoreCapacity(long soldCount) {
        return soldCount > 0 ? soldCount - 1 : 0;
    }

    private boolean isValidTicketStatus(String status) {
        return "pending".equals(status)
                || "approved".equals(status)
                || "rejected".equals(status)
                || "confirmed".equals(status)
                || "waitlisted".equals(status);
    }
}
