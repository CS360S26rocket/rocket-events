// ─── OfflineTicket.java ───────────────────────────────────────────────────────
// Room entity that caches ticket records locally for offline QR validation.
// Pre-fetched when the organizer opens the scanner; synced back when connectivity returns.

package com.example.seprojectpart3;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_tickets")
public class OfflineTicket {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ticket_id")
    public String ticketId;

    @ColumnInfo(name = "event_id")
    public String eventId;

    @ColumnInfo(name = "attendee_name")
    public String attendeeName;

    @ColumnInfo(name = "tier")
    public String tier;

    /**
     * Mirrored from Firestore: "valid" | "used" | "cancelled"
     * Kept in sync — when we scan offline, we set this to "used" locally
     * and then sync to Firestore once connectivity returns.
     */
    @ColumnInfo(name = "status")
    public String status;

    /**
     * Epoch millis when this ticket was scanned (used locally).
     * 0 if not yet scanned.
     */
    @ColumnInfo(name = "scanned_at_ms")
    public long scannedAtMs;

    /**
     * True if this scan happened while offline and hasn't been synced to Firestore yet.
     */
    @ColumnInfo(name = "pending_sync")
    public boolean pendingSync;
}
