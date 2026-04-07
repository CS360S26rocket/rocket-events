package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;

public class AuthRepositoryTest {

    @Test
    public void testInvalidEmailDomain() {
        AuthRepository repo = new AuthRepository();

        repo.loginUser("user@gmail.com", "password", new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String token, String uid) {
                fail("Login should not succeed with non-university email");
            }

            @Override
            public void onFailure(String errorMessage) {
                assertEquals("Please use your university email", errorMessage);
            }
        });
    }

    @Test
    public void testLogoutUser() {
        AuthRepository repo = new AuthRepository();
        repo.logoutUser();
        assertNull(repo.getCurrentUser());
    }
}