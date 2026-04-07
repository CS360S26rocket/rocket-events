package com.example.seprojectpart3;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OfflineTicketDao {

    /** Bulk-insert tickets when pre-fetching for an event. REPLACE handles re-fetches. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OfflineTicket> tickets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(OfflineTicket ticket);

    /** Main lookup during QR scan validation. */
    @Query("SELECT * FROM offline_tickets WHERE ticket_id = :ticketId AND event_id = :eventId LIMIT 1")
    OfflineTicket findTicket(String ticketId, String eventId);

    /** Mark a ticket as used offline, queue for sync. */
    @Query("UPDATE offline_tickets SET status = 'used', scanned_at_ms = :scannedAtMs, pending_sync = 1 " +
            "WHERE ticket_id = :ticketId")
    void markUsedOffline(String ticketId, long scannedAtMs);

    /** Mark a ticket as synced after Firestore update succeeds. */
    @Query("UPDATE offline_tickets SET pending_sync = 0 WHERE ticket_id = :ticketId")
    void markSynced(String ticketId);

    /** Get all tickets that were scanned offline and haven't been synced yet. */
    @Query("SELECT * FROM offline_tickets WHERE pending_sync = 1")
    List<OfflineTicket> getPendingSync();

    /** Called when the organizer switches to a different event — clear stale cache. */
    @Query("DELETE FROM offline_tickets WHERE event_id != :eventId")
    void clearOtherEvents(String eventId);

    @Query("DELETE FROM offline_tickets WHERE event_id = :eventId")
    void clearEvent(String eventId);
}
