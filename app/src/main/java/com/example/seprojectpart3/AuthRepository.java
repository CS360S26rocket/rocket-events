/*
 * This file defines AuthRepository, a data repository used by the Scene app.
 * It contains authentication helpers for login, registration, roles, and user profile records.
 * Its functions include loginUser, logoutUser, getCurrentUser to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.FirebaseFirestore;




public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public void loginUser(String email, String password, AuthCallback callback) {
        
        if (!email.endsWith(".edu.pk") && !email.endsWith("@lums.edu.pk")) {
            callback.onFailure("Please use your university email");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    
                    user.getIdToken(true)
                            .addOnSuccessListener(tokenResult -> {
                                String token = tokenResult.getToken();

                                
                                FirebaseMessaging.getInstance().getToken()
                                        .addOnSuccessListener(fcmToken -> {
                                            FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(user.getUid())
                                                    .update("fcmToken", fcmToken);
                                        });

                                callback.onSuccess(token, user.getUid());
                            });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void logoutUser() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public interface AuthCallback {
        void onSuccess(String token, String uid);
        void onFailure(String errorMessage);
    }
}
