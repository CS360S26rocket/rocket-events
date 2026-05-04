/*
 * This file defines ProofImageViewerActivity, an Android activity used by the Scene app.
 * It contains the legacy proof image preview and approval or rejection screen.
 * Its functions include onCreate to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;





public class ProofImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proof_image_viewer);

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        ImageView ivFullImage = findViewById(R.id.ivFullProofImage);

        if (imageUrl == null) {
            Toast.makeText(this, "Image not available.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this)
                .load(imageUrl)
                .into(ivFullImage);

        
        ivFullImage.setOnClickListener(v -> finish());
    }
}
