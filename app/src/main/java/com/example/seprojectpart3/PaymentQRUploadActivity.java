/*
 * This file defines PaymentQRUploadActivity, an Android activity used by the Scene app.
 * It contains organizer payment QR upload and storage flow.
 * Its functions include onCreate, onActivityResult, uploadQRImage, onSuccess to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;













public class PaymentQRUploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private ImageView imgPreview;
    private Button btnSelectImage;
    private Button btnUpload;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvEventName;

    private Uri selectedImageUri;
    private String eventId;

    private FileStorageManager fileStorageManager; 
    private PaymentQRRepository paymentQRRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_qr_upload);

        
        imgPreview = findViewById(R.id.imgQRPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpload = findViewById(R.id.btnUploadQR);
        progressBar = findViewById(R.id.progressBarUpload);
        tvStatus = findViewById(R.id.tvUploadStatus);
        tvEventName = findViewById(R.id.tvEventName);

        
        eventId = getIntent().getStringExtra("eventId");
        String eventName = getIntent().getStringExtra("eventName");
        if (eventId == null) {
            Toast.makeText(this, "No event selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvEventName.setText(eventName != null ? eventName : "Event");

        
        fileStorageManager = new FileStorageManager();
        paymentQRRepo = new PaymentQRRepository();

        
        loadExistingQR();

        
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        
        btnUpload.setOnClickListener(v -> uploadQRImage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            
            try {
                InputStream inputStream = getContentResolver()
                        .openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imgPreview.setImageBitmap(bitmap);
                imgPreview.setVisibility(View.VISIBLE);
                btnUpload.setEnabled(true);
                tvStatus.setText("Image selected — tap Upload");
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadQRImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        
        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);
        btnSelectImage.setEnabled(false);
        tvStatus.setText("Uploading...");

        
        String storagePath = "payment_qr/" + eventId + "/qr_code.jpg";

        fileStorageManager.uploadFile(
                selectedImageUri,
                storagePath,
                this,
                new FileStorageManager.OnUploadListener() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        
                        saveQRUrlToEvent(downloadUrl);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            btnSelectImage.setEnabled(true);
                            tvStatus.setText("Upload failed: " + errorMessage);
                        });
                    }

                    @Override
                    public void onProgress(int percentage) {
                        runOnUiThread(() ->
                                tvStatus.setText("Uploading... " + percentage + "%"));
                    }
                }
        );
    }

    private void saveQRUrlToEvent(String downloadUrl) {
        paymentQRRepo.savePaymentQRUrl(eventId, downloadUrl,
                new PaymentQRRepository.OnQRSavedListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("Payment QR code uploaded successfully!");
                            btnUpload.setEnabled(false);
                            Toast.makeText(PaymentQRUploadActivity.this,
                                    "QR code saved",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            btnSelectImage.setEnabled(true);
                            tvStatus.setText("Failed to save: " + errorMessage);
                        });
                    }
                });
    }

    private void loadExistingQR() {
        paymentQRRepo.getPaymentQRUrl(eventId, url -> {
            if (url != null) {
                tvStatus.setText("QR code already uploaded. Select a new image to replace.");
                
            }
        });
    }
}
