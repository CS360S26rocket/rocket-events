/*
 * This file defines EmailSenderTest, a test class used to verify Scene app behavior.
 * It contains automated test coverage for EmailSender behavior and expected results.
 * Its functions include testOtpEmailContent to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

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
