package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;

public class OrganizerRepositoryTest {

    @Test
    public void testEmailNormalization() {
        String email = "  CSSociety@LUMS.EDU.PK ";
        String normalized = email.trim().toLowerCase();
        assertEquals("cssociety@lums.edu.pk", normalized);
    }

    @Test
    public void testValidDomain() {
        String email = "cssociety@lums.edu.pk";
        String domain = email.substring(email.indexOf("@") + 1);
        assertTrue(domain.endsWith(".edu.pk"));
    }

    @Test
    public void testInvalidDomain() {
        String email = "cssociety@gmail.com";
        String domain = email.substring(email.indexOf("@") + 1);
        assertFalse(domain.endsWith(".edu.pk"));
    }
}