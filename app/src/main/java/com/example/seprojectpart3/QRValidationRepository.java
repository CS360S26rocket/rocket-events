package com.example.seprojectpart3;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.seprojectpart3.AppDatabase;
import com.example.seprojectpart3.OfflineTicket;
import com.example.seprojectpart3.OfflineTicketDao;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Story #38 — Scan attendee QR codes at gate for entry.
 *
 * Flow:
 * 1. Organizer opens scanner → call prefetchTicketsForEvent() to cache all
 *    valid tickets in Room DB (handles offline fallback).
 * 2. ML Kit reads a QR code → call validateTicket() with the scanned string.
 * 3. If online:  validate against Firestore (authoritative), mark as "used" atomically.
 *    If offline: validate against local Room cache, queue a pending sync.
 * 4. When connectivity returns → call syncPendingScans() to push offline scans to Firestore.
 *
 * QR code format (agree with M3 who generates tickets in #11/#12):
 *   The QR code encodes ONLY the ticketId (Firestore document ID string).
 *   Example: "tkt_abc123xyz"
 *
 * COORDINATE WITH M3:
 *   - Collection name: "tickets" (confirm with M3)
 *   - Status field values: "valid" | "used" | "cancelled" (confirm with M3)
 */
public class QRValidationRepository {

    private static final String TAG = "QRValidation";

    // ── Confirm these with M3 ─────────────────────────────────────────────────
    private static final String TICKETS_COLLECTION = "tickets";
    private static final String FIELD_STATUS        = "status";
    private static final String FIELD_EVENT_ID      = "eventId";
    private static final String FIELD_ATTENDEE_NAME = "attendeeName";
    private static final String FIELD_TIER          = "tier";
    private static final String STATUS_VALID        = "valid";
    private static final String STATUS_USED         = "used";
    private static final String STATUS_CANCELLED    = "cancelled";
    // ──────────────────────────────────────────────────────────────────────────

    public enum ScanResult {
        ADMITTED,          // ticket valid and just marked used
        ALREADY_USED,      // ticket exists but was already scanned
        CANCELLED,         // ticket was cancelled
        NOT_FOUND,         // ticketId not in Firestore OR wrong event
        OFFLINE_ADMITTED,  // admitted from local cache (offline mode)
        OFFLINE_UNKNOWN,   // not in cache and offline — cannot verify
        ERROR              // Firestore error
    }

    public interface ScanCallback {
        void onResult(ScanResult result, String attendeeName, String tier);
    }

    private final FirebaseFirestore db;
    private final OfflineTicketDao  dao;
    private final Context           context;
    private final ExecutorService   executor = Executors.newSingleThreadExecutor();

    public QRValidationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db  = FirebaseFirestore.getInstance();
        this.dao = AppDatabase.getInstance(context).offlineTicketDao();
    }

    // ── Step 1: Pre-fetch all valid tickets into Room ─────────────────────────

    public interface PrefetchCallback {
        void onComplete(int ticketCount);
        void onError(Exception e);
    }

    /**
     * Downloads all "valid" tickets for this event into the local Room cache.
     * Call this when the organizer opens the scanner screen so offline fallback
     * has data to work with even before connectivity drops.
     *
     * Runs Firestore query on calling thread's context; Room writes on background thread.
     */
    public void prefetchTicketsForEvent(@NonNull String eventId,
                                        @NonNull PrefetchCallback callback) {
        db.collection(TICKETS_COLLECTION)
                .whereEqualTo(FIELD_EVENT_ID, eventId)
                .whereEqualTo(FIELD_STATUS, STATUS_VALID)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    executor.execute(() -> {
                        List<OfflineTicket> cached = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            OfflineTicket t = new OfflineTicket();
                            t.ticketId    = doc.getId();
                            t.eventId     = eventId;
                            t.attendeeName = doc.getString(FIELD_ATTENDEE_NAME);
                            t.tier        = doc.getString(FIELD_TIER);
                            t.status      = STATUS_VALID;
                            t.scannedAtMs = 0;
                            t.pendingSync = false;
                            cached.add(t);
                        }
                        dao.clearOtherEvents(eventId); // remove stale cache from other events
                        dao.insertAll(cached);
                        Log.d(TAG, "Pre-fetched " + cached.size() + " tickets for event " + eventId);
                        callback.onComplete(cached.size());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Prefetch failed", e);
                    callback.onError(e);
                });
    }

    // ── Step 2: Validate a scanned QR code ───────────────────────────────────

    /**
     * Main entry point after ML Kit reads a QR code.
     *
     * @param ticketId  The raw string scanned from the QR code (should be a Firestore doc ID)
     * @param eventId   The event this scanner session is for (set when organizer opens scanner)
     * @param callback  Delivers result — post to main thread before updating UI
     */
    public void validateTicket(@NonNull String ticketId,
                               @NonNull String eventId,
                               @NonNull ScanCallback callback) {
        if (isOnline()) {
            validateOnline(ticketId, eventId, callback);
        } else {
            validateOffline(ticketId, eventId, callback);
        }
    }

    // ── Online path ───────────────────────────────────────────────────────────

    private void validateOnline(String ticketId, String eventId, ScanCallback callback) {
        DocumentReference ticketRef = db.collection(TICKETS_COLLECTION).document(ticketId);

        db.runTransaction((Transaction.Function<DocumentSnapshot>) transaction -> {
            DocumentSnapshot doc = transaction.get(ticketRef);

            if (!doc.exists()) throw new RuntimeException(ScanResult.NOT_FOUND.name());

            // Make sure this ticket belongs to the correct event
            String docEventId = doc.getString(FIELD_EVENT_ID);
            if (!eventId.equals(docEventId)) throw new RuntimeException(ScanResult.NOT_FOUND.name());

            String status = doc.getString(FIELD_STATUS);
            if (STATUS_USED.equals(status))      throw new RuntimeException(ScanResult.ALREADY_USED.name());
            if (STATUS_CANCELLED.equals(status)) throw new RuntimeException(ScanResult.CANCELLED.name());

            // Valid — mark as used atomically
            transaction.update(ticketRef,
                    FIELD_STATUS, STATUS_USED,
                    "usedAt", FieldValue.serverTimestamp()
            );

            return doc; // return snapshot so we can read name/tier after transaction

        }).addOnSuccessListener(doc -> {
            String name = doc.getString(FIELD_ATTENDEE_NAME);
            String tier = doc.getString(FIELD_TIER);

            // Update local Room cache to stay consistent
            executor.execute(() -> {
                dao.markUsedOffline(ticketId, System.currentTimeMillis());
                dao.markSynced(ticketId); // already synced — was online
            });

            callback.onResult(ScanResult.ADMITTED, name, tier);

        }).addOnFailureListener(e -> {
            String msg = e.getMessage();
            if (ScanResult.ALREADY_USED.name().equals(msg)) {
                // Still try to get attendee name for display
                ticketRef.get().addOnSuccessListener(doc -> {
                    callback.onResult(ScanResult.ALREADY_USED,
                            doc.getString(FIELD_ATTENDEE_NAME),
                            doc.getString(FIELD_TIER));
                }).addOnFailureListener(e2 -> callback.onResult(ScanResult.ALREADY_USED, null, null));
            } else if (ScanResult.CANCELLED.name().equals(msg)) {
                callback.onResult(ScanResult.CANCELLED, null, null);
            } else if (ScanResult.NOT_FOUND.name().equals(msg)) {
                callback.onResult(ScanResult.NOT_FOUND, null, null);
            } else {
                Log.e(TAG, "Transaction failed", e);
                callback.onResult(ScanResult.ERROR, null, null);
            }
        });
    }

    // ── Offline path ──────────────────────────────────────────────────────────

    private void validateOffline(String ticketId, String eventId, ScanCallback callback) {
        executor.execute(() -> {
            OfflineTicket cached = dao.findTicket(ticketId, eventId);

            if (cached == null) {
                // Not in local cache — cannot safely admit without connectivity
                callback.onResult(ScanResult.OFFLINE_UNKNOWN, null, null);
                return;
            }

            if (STATUS_USED.equals(cached.status)) {
                callback.onResult(ScanResult.ALREADY_USED, cached.attendeeName, cached.tier);
                return;
            }

            if (STATUS_CANCELLED.equals(cached.status)) {
                callback.onResult(ScanResult.CANCELLED, cached.attendeeName, cached.tier);
                return;
            }

            // Valid offline — mark locally, queue for sync
            dao.markUsedOffline(ticketId, System.currentTimeMillis());
            Log.d(TAG, "Offline admit for ticket " + ticketId + " — queued for sync");
            callback.onResult(ScanResult.OFFLINE_ADMITTED, cached.attendeeName, cached.tier);
        });
    }

    // ── Step 3: Sync offline scans when connectivity returns ──────────────────

    public interface SyncCallback {
        void onSyncComplete(int synced, int failed);
    }

    /**
     * Pushes offline-scanned tickets to Firestore.
     * Call this when ConnectivityManager reports connectivity restored,
     * or when the organizer manually taps "Sync".
     */
    public void syncPendingScans(@NonNull SyncCallback callback) {
        executor.execute(() -> {
            List<OfflineTicket> pending = dao.getPendingSync();
            if (pending.isEmpty()) {
                callback.onSyncComplete(0, 0);
                return;
            }

            int[] synced = {0};
            int[] failed = {0};
            int total = pending.size();

            for (OfflineTicket ticket : pending) {
                db.collection(TICKETS_COLLECTION).document(ticket.ticketId)
                        .update(FIELD_STATUS, STATUS_USED,
                                "usedAt", FieldValue.serverTimestamp(),
                                "usedOffline", true)
                        .addOnSuccessListener(unused -> {
                            dao.markSynced(ticket.ticketId);
                            synced[0]++;
                            if (synced[0] + failed[0] == total) {
                                callback.onSyncComplete(synced[0], failed[0]);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Sync failed for ticket " + ticket.ticketId, e);
                            failed[0]++;
                            if (synced[0] + failed[0] == total) {
                                callback.onSyncComplete(synced[0], failed[0]);
                            }
                        });
            }
        });
    }

    // ── Connectivity check ────────────────────────────────────────────────────

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }
}