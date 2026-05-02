package com.example.seprojectpart3;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

/**
 * Full-screen image viewer for payment proof screenshots.
 * Launched from PendingProofsActivity when organizer taps a proof image.
 */
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

        // Tap to close
        ivFullImage.setOnClickListener(v -> finish());
    }
}
