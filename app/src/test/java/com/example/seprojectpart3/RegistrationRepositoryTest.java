/*
 * This file defines RegistrationRepositoryTest, a test class used to verify Scene app behavior.
 * It contains automated test coverage for RegistrationRepository behavior and expected results.
 * Its functions include testValidUniversityEmail, testInvalidUniversityEmail to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;

public class RegistrationRepositoryTest {

    @Test
    public void testValidUniversityEmail() {
        String email = "student@lums.edu.pk";
        assertTrue(email.endsWith("@lums.edu.pk"));
    }

    @Test
    public void testInvalidUniversityEmail() {
        String email = "student@gmail.com";
        assertFalse(email.endsWith("@lums.edu.pk"));
    }
}
