/*
 * This file defines CampusMapActivity, an Android activity used by the Scene app.
 * It contains the campus map screen and location display flow.
 * Its functions include onCreate, loadEventLocation, placePin to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

















public class CampusMapActivity extends AppCompatActivity {

    private ImageView imgCampusMap;
    private ImageView imgPin;
    private TextView tvLocationName;
    private TextView tvEventTitle;
    private TextView tvLocationDetails;

    private FirebaseFirestore db;

    
    
    private static final double MAP_MIN_LAT = 24.8600;   
    private static final double MAP_MAX_LAT = 24.8700;   
    private static final double MAP_MIN_LNG = 67.0700;   
    private static final double MAP_MAX_LNG = 67.0800;   

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

    






    private void placePin(double lat, double lng) {
        imgCampusMap.post(() -> {
            int mapWidth = imgCampusMap.getWidth();
            int mapHeight = imgCampusMap.getHeight();

            if (mapWidth == 0 || mapHeight == 0) return;

            
            double normalizedX = (lng - MAP_MIN_LNG) / (MAP_MAX_LNG - MAP_MIN_LNG);
            double normalizedY = 1.0 - (lat - MAP_MIN_LAT) / (MAP_MAX_LAT - MAP_MIN_LAT);

            
            normalizedX = Math.max(0, Math.min(1, normalizedX));
            normalizedY = Math.max(0, Math.min(1, normalizedY));

            
            float pinX = (float) (normalizedX * mapWidth) - (imgPin.getWidth() / 2f);
            float pinY = (float) (normalizedY * mapHeight) - imgPin.getHeight();

            imgPin.setX(imgCampusMap.getX() + pinX);
            imgPin.setY(imgCampusMap.getY() + pinY);
            imgPin.setVisibility(View.VISIBLE);
        });
    }
}
