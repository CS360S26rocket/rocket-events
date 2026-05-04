/*
 * This file defines InfoSetupActivity, an Android activity used by the Scene app.
 * It contains the final organizer event setup step for entry rules, contact, notes, and publishing.
 * Its functions include onCreate, publishEvent, onSuccess, onFailure to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class InfoSetupActivity extends AppCompatActivity {

    EditText etEntryReqs, etOrganizerContact, etAdditionalInfo;
    Switch switchAnnouncements;
    TextView tvStatus;
    EventRepository eventRepo;

    
    String organizerUid, title, description, venue, startDate, startTime,
            endDate, endTime, location, institutionName, eventType, category,
            capacity, ticketMode, paymentMethod, externalTicketLink, salesStart, salesStartTime, salesEnd, salesEndTime,
            bannerImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_setup);

        
        Intent i        = getIntent();
        organizerUid    = i.getStringExtra("organizerUid");
        title           = i.getStringExtra("title");
        description     = i.getStringExtra("description");
        venue           = i.getStringExtra("venue");
        startDate       = i.getStringExtra("startDate");
        startTime       = i.getStringExtra("startTime");
        endDate         = i.getStringExtra("endDate");
        endTime         = i.getStringExtra("endTime");
        location        = i.getStringExtra("location");
        institutionName = i.getStringExtra("institutionName");
        eventType       = i.getStringExtra("eventType");
        category        = i.getStringExtra("category");
        capacity        = i.getStringExtra("capacity");
        ticketMode      = i.getStringExtra("ticketMode");
        paymentMethod   = i.getStringExtra("paymentMethod");
        externalTicketLink = i.getStringExtra("externalTicketLink");
        salesStart      = i.getStringExtra("salesStart");
        salesStartTime  = i.getStringExtra("salesStartTime");
        salesEnd        = i.getStringExtra("salesEnd");
        salesEndTime    = i.getStringExtra("salesEndTime");
        bannerImageUri  = i.getStringExtra("bannerImageUri");

        etEntryReqs          = findViewById(R.id.etEntryReqs);
        etOrganizerContact   = findViewById(R.id.etOrganizerContact);
        etAdditionalInfo     = findViewById(R.id.etAdditionalInfo);
        switchAnnouncements  = findViewById(R.id.switchAnnouncements);
        tvStatus             = findViewById(R.id.tvStatus);
        eventRepo            = new EventRepository();

        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        
        findViewById(R.id.btnPublish).setOnClickListener(v -> publishEvent());
    }

    private void publishEvent() {
        
        String eventDate = startDate + (startTime.isEmpty() ? "" : " " + startTime);
        Integer parsedCapacity = null;

        if (capacity != null && !capacity.isEmpty()) {
            try {
                parsedCapacity = Integer.parseInt(capacity);
                if (parsedCapacity < 0) {
                    tvStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("Capacity cannot be negative.");
                    tvStatus.setTextColor(getColor(R.color.error_red));
                    return;
                }
            } catch (NumberFormatException e) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Capacity must be a number.");
                tvStatus.setTextColor(getColor(R.color.error_red));
                return;
            }
        }

        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Publishing event...");

        
        
        boolean isFree = "free_rsvp".equals(ticketMode);
        boolean isExternal = "external_link".equals(ticketMode);
        final Integer finalCapacity = parsedCapacity;

        eventRepo.createEvent(
                organizerUid, title, description, eventDate, venue, isFree,
                new EventRepository.EventCallback() {
                    @Override
                    public void onSuccess(String eventId) {
                        if (bannerImageUri == null || bannerImageUri.trim().isEmpty()) {
                            finalizeEvent(eventId, isFree, isExternal, finalCapacity, "", "");
                        } else {
                            uploadBannerAndFinalize(eventId, isFree, isExternal, finalCapacity);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        tvStatus.setText("Error: " + error);
                        tvStatus.setTextColor(getColor(R.color.error_red));
                    }
                });
    }

    private void uploadBannerAndFinalize(String eventId, boolean isFree, boolean isExternal, Integer finalCapacity) {
        tvStatus.setText("Uploading banner...");
        String storagePath = "event_banners/" + eventId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference bannerRef = FirebaseStorage.getInstance().getReference().child(storagePath);

        bannerRef.putFile(Uri.parse(bannerImageUri))
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return bannerRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> finalizeEvent(eventId, isFree, isExternal, finalCapacity,
                        downloadUri.toString(), storagePath))
                .addOnFailureListener(e -> {
                    tvStatus.setText("Banner upload failed: " + (e.getMessage() == null ? "Unknown error" : e.getMessage()));
                    tvStatus.setTextColor(getColor(R.color.error_red));
                });
    }

    private void finalizeEvent(String eventId, boolean isFree, boolean isExternal, Integer finalCapacity,
                               String bannerUrl, String bannerStoragePath) {
        TicketRepository ticketRepo = new TicketRepository();
        if (finalCapacity != null) {
            ticketRepo.updateCapacity(eventId, finalCapacity,
                    new TicketRepository.TicketCallback() {
                        @Override public void onSuccess(String id) {  }
                        @Override public void onFailure(String e) {  }
                    });
        }

        if (salesStart != null && !salesStart.isEmpty() &&
                salesEnd != null && !salesEnd.isEmpty()) {
            eventRepo.setSalesTime(eventId, salesStart, salesEnd,
                    new EventRepository.EventCallback() {
                        @Override public void onSuccess(String id) {  }
                        @Override public void onFailure(String e) {  }
                    });
        }

        if (!isExternal) {
            ticketRepo.saveDefaultTicketTiers(eventId, isFree,
                    new TicketRepository.TicketCallback() {
                        @Override public void onSuccess(String message) {  }
                        @Override public void onFailure(String error) {  }
                    });
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("dateOnly", startDate);
        metadata.put("startTime", startTime);
        metadata.put("endDate", endDate);
        metadata.put("endTime", endTime);
        metadata.put("location", location);
        metadata.put("institutionName", institutionName);
        metadata.put("eventType", eventType);
        metadata.put("category", category);
        metadata.put("ticketMode", ticketMode);
        metadata.put("paymentMethod", paymentMethod == null ? "" : paymentMethod);
        metadata.put("externalTicketLink", externalTicketLink == null ? "" : externalTicketLink);
        metadata.put("externalLinkEnabled", isExternal);
        metadata.put("externalLinkClicks", 0);
        metadata.put("ticketSalesStartTime", salesStartTime);
        metadata.put("ticketSalesEndTime", salesEndTime);
        metadata.put("priceSummary", isExternal ? "External registration" : (isFree ? "Free RSVP" : "From PKR 300"));
        metadata.put("minTicketPrice", isExternal || isFree ? 0 : 300);
        metadata.put("paymentQrUrl", "");
        metadata.put("bannerImageUrl", bannerUrl);
        metadata.put("bannerStoragePath", bannerStoragePath);
        if (bannerUrl != null && !bannerUrl.trim().isEmpty()) {
            metadata.put("bannerUpdatedAt", FieldValue.serverTimestamp());
        }
        metadata.put("entryRequirements", etEntryReqs.getText().toString().trim());
        metadata.put("organizerContact", etOrganizerContact.getText().toString().trim());
        metadata.put("additionalInfo", etAdditionalInfo.getText().toString().trim());
        metadata.put("announcementsEnabled", switchAnnouncements.isChecked());

        eventRepo.updateEventMetadata(eventId, metadata,
                new EventRepository.EventCallback() {
                    @Override public void onSuccess(String id) {
                        Intent intent = new Intent(InfoSetupActivity.this,
                                PaymentSuccessActivity.class);
                        intent.putExtra("target", "organizer");
                        intent.putExtra("uid", organizerUid);
                        intent.putExtra("title", "Event published");
                        intent.putExtra("message", "Your event is live and ready for attendees.");
                        startActivity(intent);
                        finish();
                    }

                    @Override public void onFailure(String error) {
                        tvStatus.setText("Event saved, but banner metadata failed: " + error);
                        tvStatus.setTextColor(getColor(R.color.error_red));
                    }
                });
    }
}
