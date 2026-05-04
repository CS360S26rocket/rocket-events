/*
 * This file defines RegistrationRepository, a data repository used by the Scene app.
 * It contains student registration and approved campus user data operations.
 * Its functions include registerUser, createAuthAccount, saveUserProfile, isValidUniversityEmail to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;







public class RegistrationRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void registerUser(String email, String password,
                             String name, String role,
                             RegistrationCallback callback) {

        if (!isValidUniversityEmail(email)) {
            callback.onFailure("Must use a valid university email");
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onFailure("An account with this email already exists");
                        return;
                    }
                    createAuthAccount(email, password, name, role, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void createAuthAccount(String email, String password,
                                   String name, String role,
                                   RegistrationCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    
                    saveUserProfile(uid, email, name, role, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void saveUserProfile(String uid, String email,
                                 String name, String role,
                                 RegistrationCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("email", email);
        user.put("name", name);
        user.put("role", role);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(v -> callback.onSuccess(uid))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private boolean isValidUniversityEmail(String email) {

        return email != null &&
                (email.endsWith("@lums.edu.pk") );
    }

    public interface RegistrationCallback {
        void onSuccess(String uid);
        void onFailure(String errorMessage);
    }
}
