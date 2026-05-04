package com.example.seprojectpart3;

import static org.junit.Assert.*;

import org.junit.Test;

public class AuthRepositoryTest {

    @Test
    public void universityEmail_acceptsEduPkDomain() {
        assertTrue(isUniversityEmail("student@university.edu.pk"));
    }

    @Test
    public void universityEmail_acceptsLumsDomain() {
        assertTrue(isUniversityEmail("student@lums.edu.pk"));
    }

    @Test
    public void universityEmail_rejectsGmailDomain() {
        assertFalse(isUniversityEmail("user@gmail.com"));
    }

    @Test
    public void universityEmail_rejectsEmptyEmail() {
        assertFalse(isUniversityEmail(""));
    }

    private boolean isUniversityEmail(String email) {
        return email != null
                && (email.endsWith(".edu.pk") || email.endsWith("@lums.edu.pk"));
    }
}
