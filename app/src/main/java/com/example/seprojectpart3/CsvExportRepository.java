/*
 * This file defines CsvExportRepository, a data repository used by the Scene app.
 * It contains CSV export generation for organizer attendee and ticketing data.
 * Its functions include exportAttendeeCsv, createCsvFile, safe, esc to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.content.Context;
import android.os.Environment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;











public class CsvExportRepository {

    private static final String REGISTRATIONS_COLLECTION = "registrations";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void exportAttendeeCsv(Context context, String eventId, CsvCallback callback) {
        if (callback == null) return;

        if (context == null) {
            callback.onFailure("Context is required");
            return;
        }
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onFailure("Event ID is required");
            return;
        }

        final String cleanEventId = eventId.trim();

        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("eventId", cleanEventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    StringBuilder csv = new StringBuilder();
                    csv.append("Name,Email,Ticket Type,Status,Registered At\n");

                    int rows = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = safe(doc.getString("name"));
                        String email = safe(doc.getString("email"));
                        String ticketType = safe(doc.getString("ticketType"));
                        String status = safe(doc.getString("status"));

                        Timestamp ts = doc.getTimestamp("registeredAt");
                        String regAt = ts != null ? ts.toDate().toString() : "";

                        csv.append("\"").append(esc(name)).append("\"").append(",");
                        csv.append("\"").append(esc(email)).append("\"").append(",");
                        csv.append("\"").append(esc(ticketType)).append("\"").append(",");
                        csv.append("\"").append(esc(status)).append("\"").append(",");
                        csv.append("\"").append(esc(regAt)).append("\"").append("\n");

                        rows++;
                    }

                    try {
                        File file = createCsvFile(context, cleanEventId);
                        FileWriter writer = new FileWriter(file);
                        writer.write(csv.toString());
                        writer.flush();
                        writer.close();

                        callback.onSuccess(file.getAbsolutePath(), rows);
                    } catch (IOException e) {
                        callback.onFailure("Failed to write CSV: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage() == null ? "Export failed" : e.getMessage()));
    }

    private File createCsvFile(Context context, String eventId) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "attendees_" + eventId + "_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) throw new IOException("External storage not available");
        if (!dir.exists()) dir.mkdirs();

        return new File(dir, fileName);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String esc(String s) {
        
        return s.replace("\"", "\"\"");
    }

    public interface CsvCallback {
        void onSuccess(String filePath, int rowCount);
        void onFailure(String error);
    }
}
