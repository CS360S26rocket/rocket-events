package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Story #27 — Event location on campus map
 * M3 · Sprint 3
 *
 * Displays event location on a static campus map image.
 * Decision (day 1): using a static asset approach instead of
 * a maps API to avoid external dependencies and API key costs.
 *
 * The campus map image is stored in res/drawable/campus_map.png.
 * Events store their location as:
 *   - locationName  (String)  e.g. "Auditorium Block A"
 *   - latitude      (double)  campus-relative y-coordinate
 *   - longitude     (double)  campus-relative x-coordinate
 *
 * A pin marker is placed on the map image at the relative position.
 */
public class CampusMapActivity extends AppCompatActivity {

    private ImageView imgCampusMap;
    private ImageView imgPin;
    private TextView tvLocationName;
    private TextView tvEventTitle;
    private TextView tvLocationDetails;

    private FirebaseFirestore db;

    // Campus map image bounds (pixel coordinates of the static asset)
    // Adjust these to match your actual campus_map.png dimensions
    private static final double MAP_MIN_LAT = 24.8600;   // bottom edge latitude
    private static final double MAP_MAX_LAT = 24.8700;   // top edge latitude
    private static final double MAP_MIN_LNG = 67.0700;   // left edge longitude
    private static final double MAP_MAX_LNG = 67.0800;   // right edge longitude

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_map);

        db = FirebaseFirestore.getInstance();

        imgCampusMap = findViewById(R.id.imgCampusMap);
        imgPin = findViewById(R.id.imgPin);
        tvLocationName = findViewById(R.id.tvLocationName);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvLocationDetails = findViewById(R.id.tvLocationDetails);

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventLocation(eventId);
    }

    private void loadEventLocation(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = doc.getString("title");
                    String locationName = doc.getString("locationName");
                    String venue = doc.getString("venue");
                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");

                    tvEventTitle.setText(title != null ? title : "Event");
                    tvLocationName.setText(locationName != null ? locationName : "Campus");

                    if (venue != null) {
                        tvLocationDetails.setText(venue);
                        tvLocationDetails.setVisibility(View.VISIBLE);
                    }

                    if (lat != null && lng != null) {
                        placePin(lat, lng);
                    } else {
                        // No coordinates — show map without pin
                        imgPin.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "Exact location not available on map",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load location: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Places a pin marker on the campus map image at the position
     * corresponding to the given latitude/longitude.
     *
     * Uses a post() callback to ensure the ImageView has been measured
     * before calculating pixel positions.
     */
    private void placePin(double lat, double lng) {
        imgCampusMap.post(() -> {
            int mapWidth = imgCampusMap.getWidth();
            int mapHeight = imgCampusMap.getHeight();

            if (mapWidth == 0 || mapHeight == 0) return;

            // Normalize coordinates to 0..1 range within campus bounds
            double normalizedX = (lng - MAP_MIN_LNG) / (MAP_MAX_LNG - MAP_MIN_LNG);
            double normalizedY = 1.0 - (lat - MAP_MIN_LAT) / (MAP_MAX_LAT - MAP_MIN_LAT);

            // Clamp to map bounds
            normalizedX = Math.max(0, Math.min(1, normalizedX));
            normalizedY = Math.max(0, Math.min(1, normalizedY));

            // Convert to pixel position (center the pin on the point)
            float pinX = (float) (normalizedX * mapWidth) - (imgPin.getWidth() / 2f);
            float pinY = (float) (normalizedY * mapHeight) - imgPin.getHeight();

            imgPin.setX(imgCampusMap.getX() + pinX);
            imgPin.setY(imgCampusMap.getY() + pinY);
            imgPin.setVisibility(View.VISIBLE);
        });
    }
}
