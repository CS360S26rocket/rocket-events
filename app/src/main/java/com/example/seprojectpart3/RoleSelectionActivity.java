/*
 * This file defines RoleSelectionActivity, an Android activity used by the Scene app.
 * It contains the role choice screen for campus users and organizers.
 * Its functions include onCreate to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        
        findViewById(R.id.cardCampusUser).setOnClickListener(v -> {
            startActivity(new Intent(this, CampusLoginActivity.class));
        });

        
        findViewById(R.id.cardOrganizer).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerLoginActivity.class));
        });
    }
}
