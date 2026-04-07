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