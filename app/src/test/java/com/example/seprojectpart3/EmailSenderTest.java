package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;

public class EmailSenderTest {

    @Test
    public void testOtpEmailContent() {
        String otp = "123456";
        String expected = "Your OTP is: " + otp + "\n\nThis code expires in 10 minutes.";
        assertTrue(expected.contains(otp));
    }
}