package com.example.seprojectpart3;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Story #33 — Apply discount / referral code at checkout
 * M3 · Sprint 4
 *
 * Server-side validation of discount and referral codes.
 * Discount codes are stored in a "discount_codes" Firestore collection.
 *
 * Document structure:
 * {
 *   code:          "EARLYBIRD20"
 *   discountType:  "percentage" | "fixed"
 *   discountValue: 20          (% or flat amount)
 *   maxUses:       100
 *   currentUses:   42
 *   validFrom:     Timestamp
 *   validUntil:    Timestamp
 *   eventId:       "abc123" | null  (null = global code)
 *   isActive:      true
 *   minTicketPrice: 0.0       (minimum price to apply)
 * }
 */
public class DiscountRepository {

    private static final String COLLECTION_DISCOUNTS = "discount_codes";
    private final FirebaseFirestore db;

    public DiscountRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public DiscountRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // ── Result wrapper ──────────────────────────────────────────
    public static class DiscountResult {
        public final boolean valid;
        public final String message;
        public final String discountType;    // "percentage" or "fixed"
        public final double discountValue;
        public final double finalPrice;
        public final String discountId;

        private DiscountResult(boolean valid, String message, String discountType,
                               double discountValue, double finalPrice, String discountId) {
            this.valid = valid;
            this.message = message;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.finalPrice = finalPrice;
            this.discountId = discountId;
        }

        public static DiscountResult success(String type, double value,
                                             double finalPrice, String id) {
            return new DiscountResult(true, "Discount applied", type, value, finalPrice, id);
        }

        public static DiscountResult failure(String message) {
            return new DiscountResult(false, message, null, 0, 0, null);
        }
    }

    // ── Callback ────────────────────────────────────────────────
    public interface OnDiscountValidatedListener {
        void onResult(DiscountResult result);
    }

    /**
     * Validate a discount/referral code against an event's ticket price.
     *
     * @param code          The code entered by the user (case-insensitive)
     * @param eventId       The event being purchased
     * @param originalPrice The original ticket price before discount
     * @param listener      Callback with validation result
     */
    public void validateAndApply(String code, String eventId,
                                 double originalPrice,
                                 OnDiscountValidatedListener listener) {
        if (code == null || code.trim().isEmpty()) {
            listener.onResult(DiscountResult.failure("Please enter a discount code"));
            return;
        }

        String normalizedCode = code.trim().toUpperCase();

        db.collection(COLLECTION_DISCOUNTS)
                .whereEqualTo("code", normalizedCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onResult(DiscountResult.failure("Invalid discount code"));
                        return;
                    }

                    var doc = querySnapshot.getDocuments().get(0);
                    String discountId = doc.getId();

                    // ── Check active ────────────────────────
                    Boolean isActive = doc.getBoolean("isActive");
                    if (isActive == null || !isActive) {
                        listener.onResult(
                                DiscountResult.failure("This code is no longer active"));
                        return;
                    }

                    // ── Check event scope ───────────────────
                    String codeEventId = doc.getString("eventId");
                    if (codeEventId != null && !codeEventId.equals(eventId)) {
                        listener.onResult(
                                DiscountResult.failure("This code is not valid for this event"));
                        return;
                    }

                    // ── Check date validity ─────────────────
                    Timestamp validFrom = doc.getTimestamp("validFrom");
                    Timestamp validUntil = doc.getTimestamp("validUntil");
                    Date now = new Date();

                    if (validFrom != null && now.before(validFrom.toDate())) {
                        listener.onResult(
                                DiscountResult.failure("This code is not yet active"));
                        return;
                    }
                    if (validUntil != null && now.after(validUntil.toDate())) {
                        listener.onResult(
                                DiscountResult.failure("This code has expired"));
                        return;
                    }

                    // ── Check usage limit ───────────────────
                    Long maxUses = doc.getLong("maxUses");
                    Long currentUses = doc.getLong("currentUses");
                    if (maxUses != null && currentUses != null && currentUses >= maxUses) {
                        listener.onResult(
                                DiscountResult.failure("This code has reached its usage limit"));
                        return;
                    }

                    // ── Check minimum price ─────────────────
                    Double minPrice = doc.getDouble("minTicketPrice");
                    if (minPrice != null && originalPrice < minPrice) {
                        listener.onResult(DiscountResult.failure(
                                "Minimum ticket price for this code is " + minPrice));
                        return;
                    }

                    // ── Calculate discount ──────────────────
                    String discountType = doc.getString("discountType");
                    Double discountValue = doc.getDouble("discountValue");

                    if (discountType == null || discountValue == null) {
                        listener.onResult(
                                DiscountResult.failure("Invalid discount configuration"));
                        return;
                    }

                    double finalPrice;
                    if ("percentage".equals(discountType)) {
                        double discount = originalPrice * (discountValue / 100.0);
                        finalPrice = Math.max(0, originalPrice - discount);
                    } else if ("fixed".equals(discountType)) {
                        finalPrice = Math.max(0, originalPrice - discountValue);
                    } else {
                        listener.onResult(
                                DiscountResult.failure("Unknown discount type"));
                        return;
                    }

                    // Round to 2 decimal places
                    finalPrice = Math.round(finalPrice * 100.0) / 100.0;

                    listener.onResult(DiscountResult.success(
                            discountType, discountValue, finalPrice, discountId));
                })
                .addOnFailureListener(e ->
                        listener.onResult(DiscountResult.failure(
                                "Failed to validate code: " + e.getMessage())));
    }

    /**
     * Increment the usage counter for a discount code after successful
     * ticket generation. Called by TKT-A upon approval.
     */
    public void incrementUsage(String discountId) {
        if (discountId == null) return;

        db.collection(COLLECTION_DISCOUNTS)
                .document(discountId)
                .update("currentUses", FieldValue.increment(1));
    }

    /**
     * Create a new discount code (organizer action).
     */
    public void createDiscountCode(String code, String discountType,
                                   double discountValue, int maxUses,
                                   String eventId, Date validFrom,
                                   Date validUntil,
                                   OnCodeCreatedListener listener) {
        Map<String, Object> discountDoc = new HashMap<>();
        discountDoc.put("code", code.trim().toUpperCase());
        discountDoc.put("discountType", discountType);
        discountDoc.put("discountValue", discountValue);
        discountDoc.put("maxUses", maxUses);
        discountDoc.put("currentUses", 0);
        discountDoc.put("isActive", true);
        discountDoc.put("minTicketPrice", 0.0);
        discountDoc.put("createdAt", FieldValue.serverTimestamp());

        if (eventId != null) {
            discountDoc.put("eventId", eventId);
        }
        if (validFrom != null) {
            discountDoc.put("validFrom", new Timestamp(validFrom));
        }
        if (validUntil != null) {
            discountDoc.put("validUntil", new Timestamp(validUntil));
        }

        db.collection(COLLECTION_DISCOUNTS)
                .add(discountDoc)
                .addOnSuccessListener(docRef -> listener.onSuccess(docRef.getId()))
                .addOnFailureListener(e ->
                        listener.onFailure("Failed to create code: " + e.getMessage()));
    }

    public interface OnCodeCreatedListener {
        void onSuccess(String discountId);
        void onFailure(String errorMessage);
    }
}
