package com.example.seprojectpart3;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Story #16 — Separate schema tracking for referral/discount codes.
 *
 * Stored in Firestore collection: discountCodes/{codeId}
 *
 * type = "discount" → applies discountPercent or discountAmount to ticket price
 * type = "referral"  → tracks who referred whom; may also apply a discount
 */
public class DiscountCode {

    @DocumentId
    private String codeId;

    private String code;            // Human-readable code, e.g. "SAVE20" or "REF_ali123"
    private String type;            // "discount" or "referral"
    private double discountPercent; // 0–100. Use this OR discountAmount, not both.
    private double discountAmount;  // Flat PKR amount off. Use this OR discountPercent.
    private int maxUses;            // -1 = unlimited
    private int currentUses;
    private String eventId;         // null = applies to all events by this organizer
    private String createdByOrgId;  // organizer who created it
    private String referrerId;      // only for type="referral": the user who shared the code

    @ServerTimestamp
    private Date createdAt;

    private Date expiresAt;         // null = never expires
    private boolean active;

    // Firestore requires a public no-arg constructor
    public DiscountCode() {}

    // --- Builder ---

    public static class Builder {
        private final DiscountCode obj = new DiscountCode();

        public Builder code(String code)                 { obj.code = code; return this; }
        public Builder type(String type)                 { obj.type = type; return this; }
        public Builder discountPercent(double pct)       { obj.discountPercent = pct; return this; }
        public Builder discountAmount(double amt)        { obj.discountAmount = amt; return this; }
        public Builder maxUses(int max)                  { obj.maxUses = max; return this; }
        public Builder eventId(String eventId)           { obj.eventId = eventId; return this; }
        public Builder createdByOrgId(String orgId)      { obj.createdByOrgId = orgId; return this; }
        public Builder referrerId(String referrerId)     { obj.referrerId = referrerId; return this; }
        public Builder expiresAt(Date date)              { obj.expiresAt = date; return this; }

        public DiscountCode build() {
            obj.currentUses = 0;
            obj.active = true;
            return obj;
        }
    }

    // --- Getters & Setters ---

    public String getCodeId() { return codeId; }
    public void setCodeId(String codeId) { this.codeId = codeId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }

    public int getCurrentUses() { return currentUses; }
    public void setCurrentUses(int currentUses) { this.currentUses = currentUses; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCreatedByOrgId() { return createdByOrgId; }
    public void setCreatedByOrgId(String createdByOrgId) { this.createdByOrgId = createdByOrgId; }

    public String getReferrerId() { return referrerId; }
    public void setReferrerId(String referrerId) { this.referrerId = referrerId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
