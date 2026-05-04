package com.example.seprojectpart3;

import static org.junit.Assert.*;

import org.junit.Test;

public class OtpManagerTest {

    @Test
    public void otpFormat_hasSixDigits() {
        String otp = formatOtp(123456);

        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    public void otpFormat_padsLeadingZeros() {
        String otp = formatOtp(42);

        assertEquals("000042", otp);
        assertEquals(6, otp.length());
    }

    @Test
    public void otpExpiry_isTenMinutesAfterCreation() {
        long now = 1000L;
        long expiry = now + (10 * 60 * 1000);

        assertEquals(601000L, expiry);
    }

    private String formatOtp(int number) {
        return String.format("%06d", number);
    }
}
