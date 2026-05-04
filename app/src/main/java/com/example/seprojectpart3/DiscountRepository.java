/*
 * This file defines DiscountRepository, a data repository used by the Scene app.
 * It contains discount and promotion lookup behavior for event ticketing.
 * Its functions include success, failure, validateAndApply, incrementUsage to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;






















public class DiscountRepository {

    private static final String COLLECTION_DISCOUNTS = "discount_codes";
    private final FirebaseFirestore db;

    public DiscountRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public DiscountRepository(FirebaseFirestore db) {
        this.db = db;
    }

    
    public static class DiscountResult {
        public final boolean valid;
        public final String message;
        public final String discountType;    
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

    
    public interface OnDiscountValidatedListener {
        void onResult(DiscountResult result);
    }

    







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

                    
                    Boolean isActive = doc.getBoolean("isActive");
                    if (isActive == null || !isActive) {
                        listener.onResult(
                                DiscountResult.failure("This code is no longer active"));
                        return;
                    }

                    
                    String codeEventId = doc.getString("eventId");
                    if (codeEventId != null && !codeEventId.equals(eventId)) {
                        listener.onResult(
                                DiscountResult.failure("This code is not valid for this event"));
                        return;
                    }

                    
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

                    
                    Long maxUses = doc.getLong("maxUses");
                    Long currentUses = doc.getLong("currentUses");
                    if (maxUses != null && currentUses != null && currentUses >= maxUses) {
                        listener.onResult(
                                DiscountResult.failure("This code has reached its usage limit"));
                        return;
                    }

                    
                    Double minPrice = doc.getDouble("minTicketPrice");
                    if (minPrice != null && originalPrice < minPrice) {
                        listener.onResult(DiscountResult.failure(
                                "Minimum ticket price for this code is " + minPrice));
                        return;
                    }

                    
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

                    
                    finalPrice = Math.round(finalPrice * 100.0) / 100.0;

                    listener.onResult(DiscountResult.success(
                            discountType, discountValue, finalPrice, discountId));
                })
                .addOnFailureListener(e ->
                        listener.onResult(DiscountResult.failure(
                                "Failed to validate code: " + e.getMessage())));
    }

    



    public void incrementUsage(String discountId) {
        if (discountId == null) return;

        db.collection(COLLECTION_DISCOUNTS)
                .document(discountId)
                .update("currentUses", FieldValue.increment(1));
    }

    


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
