/*
 * This file defines PaymentSuccessActivity, an Android activity used by the Scene app.
 * It contains the confirmation tick screen shown after completed payment-style actions.
 * Its functions include onCreate, goHome to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");
        String uid = getIntent().getStringExtra("uid");
        String target = getIntent().getStringExtra("target");

        ((TextView) findViewById(R.id.tvSuccessTitle)).setText(
                title == null || title.trim().isEmpty() ? "Confirmed" : title);
        ((TextView) findViewById(R.id.tvSuccessMessage)).setText(
                message == null || message.trim().isEmpty()
                        ? "Your action was completed successfully."
                        : message);

        findViewById(R.id.btnSuccessDone).setOnClickListener(v -> goHome(uid, target));
        findViewById(R.id.rootSuccess).postDelayed(() -> goHome(uid, target), 1400);
    }

    private void goHome(String uid, String target) {
        Intent intent = "organizer".equals(target)
                ? new Intent(this, OrganizerDashboardActivity.class)
                : new Intent(this, CampusHomeActivity.class);
        intent.putExtra("uid", uid);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
