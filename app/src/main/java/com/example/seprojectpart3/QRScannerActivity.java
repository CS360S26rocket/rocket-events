package com.example.seprojectpart3;

import android.Manifest;
import androidx.camera.core.ExperimentalGetImage;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * QR scanner activity.
 *
 * Launch with:
 * Intent intent = new Intent(this, QRScannerActivity.class);
 * intent.putExtra("eventId", eventId);
 * intent.putExtra("eventName", eventName);
 * startActivity(intent);
 */
public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";
    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final long RESULT_DISPLAY_MS = 2500;

    private PreviewView previewView;
    private View resultCard;
    private TextView tvAttendeeInfo;
    private TextView tvScanStatus;
    private TextView tvSyncStatus;
    private View overlaySuccess;
    private View overlayError;

    private String eventId;
    private String eventName;

    private QRValidationRepository validationRepo;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isProcessing = false;
    private String lastScannedTicketId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        if (eventId == null) {
            Toast.makeText(this, "Error: no event ID passed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();

        validationRepo = new QRValidationRepository(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        validationRepo.prefetchTicketsForEvent(eventId, new QRValidationRepository.PrefetchCallback() {
            @Override
            public void onComplete(int count) {
                mainHandler.post(() ->
                        tvSyncStatus.setText("Ready — " + count + " tickets cached"));
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() ->
                        tvSyncStatus.setText("Offline mode — using local cache only"));
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        resultCard = findViewById(R.id.resultCard);
        tvAttendeeInfo = findViewById(R.id.tvAttendeeInfo);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        overlaySuccess = findViewById(R.id.overlaySuccess);
        overlayError = findViewById(R.id.overlayError);

        TextView tvEventName = findViewById(R.id.tvEventName);
        tvEventName.setText(eventName != null ? eventName : "Event Scanner");

        resultCard.setVisibility(View.GONE);

        findViewById(R.id.btnSync).setOnClickListener(v -> syncOfflineScans());
    }
    @ExperimentalGetImage
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isProcessing) {
                        imageProxy.close();
                        return;
                    }

                    if (imageProxy.getImage() == null) {
                        imageProxy.close();
                        return;
                    }

                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees()
                    );

                    barcodeScanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    String value = barcode.getRawValue();
                                    if (value != null && !value.equals(lastScannedTicketId)) {
                                        isProcessing = true;
                                        lastScannedTicketId = value;
                                        onQRCodeDetected(value);
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(e -> Log.w(TAG, "Barcode analysis failed", e))
                            .addOnCompleteListener(task -> imageProxy.close());
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera init failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void onQRCodeDetected(@NonNull String ticketId) {
        Log.d(TAG, "QR detected: " + ticketId);

        validationRepo.validateTicket(ticketId, eventId, (result, attendeeName, tier) ->
                mainHandler.post(() -> showResult(result, attendeeName, tier))
        );
    }

    private void showResult(QRValidationRepository.ScanResult result,
                            String attendeeName,
                            String tier) {
        resultCard.setVisibility(View.VISIBLE);
        overlaySuccess.setVisibility(View.GONE);
        overlayError.setVisibility(View.GONE);

        switch (result) {
            case ADMITTED:
            case OFFLINE_ADMITTED:
                overlaySuccess.setVisibility(View.VISIBLE);
                tvScanStatus.setText(
                        result == QRValidationRepository.ScanResult.OFFLINE_ADMITTED
                                ? "✓ ADMITTED (Offline)"
                                : "✓ ADMITTED"
                );
                tvAttendeeInfo.setText(formatAttendeeInfo(attendeeName, tier));
                resultCard.setBackgroundColor(getColor(android.R.color.holo_green_dark));
                break;

            case ALREADY_USED:
                overlayError.setVisibility(View.VISIBLE);
                tvScanStatus.setText("✗ ALREADY SCANNED");
                tvAttendeeInfo.setText(formatAttendeeInfo(attendeeName, tier));
                resultCard.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
                break;

            case CANCELLED:
                overlayError.setVisibility(View.VISIBLE);
                tvScanStatus.setText("✗ TICKET CANCELLED");
                tvAttendeeInfo.setText("");
                resultCard.setBackgroundColor(getColor(android.R.color.holo_red_dark));
                break;

            case NOT_FOUND:
                overlayError.setVisibility(View.VISIBLE);
                tvScanStatus.setText("✗ INVALID TICKET");
                tvAttendeeInfo.setText("This QR code is not registered for this event.");
                resultCard.setBackgroundColor(getColor(android.R.color.holo_red_dark));
                break;

            case OFFLINE_UNKNOWN:
                overlayError.setVisibility(View.VISIBLE);
                tvScanStatus.setText("⚠ CANNOT VERIFY");
                tvAttendeeInfo.setText("No internet connection and ticket not in cache.\nDo not admit.");
                resultCard.setBackgroundColor(getColor(android.R.color.darker_gray));
                break;

            case ERROR:
            default:
                tvScanStatus.setText("⚠ SCAN ERROR");
                tvAttendeeInfo.setText("Try again.");
                resultCard.setBackgroundColor(getColor(android.R.color.darker_gray));
                break;
        }

        mainHandler.postDelayed(() -> {
            resultCard.setVisibility(View.GONE);
            lastScannedTicketId = "";
            isProcessing = false;
        }, RESULT_DISPLAY_MS);
    }

    private String formatAttendeeInfo(String name, String tier) {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        if (tier != null) {
            sb.append("  |  ").append(tier);
        }
        return sb.toString();
    }

    private void syncOfflineScans() {
        tvSyncStatus.setText("Syncing...");
        validationRepo.syncPendingScans((synced, failed) ->
                mainHandler.post(() -> {
                    if (failed == 0) {
                        tvSyncStatus.setText("Synced " + synced + " scan(s) ✓");
                    } else {
                        tvSyncStatus.setText("Synced " + synced + ", failed " + failed);
                    }
                })
        );
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(
                        this,
                        "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}