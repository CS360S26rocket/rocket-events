package com.example.seprojectpart3;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        // Campus User → Campus Login
        findViewById(R.id.cardCampusUser).setOnClickListener(v -> {
            startActivity(new Intent(this, CampusLoginActivity.class));
        });

        // Organizer → Organizer Login
        findViewById(R.id.cardOrganizer).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerLoginActivity.class));
        });
    }
}
