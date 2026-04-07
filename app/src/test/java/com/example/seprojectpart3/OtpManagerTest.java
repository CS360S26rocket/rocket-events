package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;
import java.lang.reflect.Method;

public class OtpManagerTest {

    @Test
    public void testOtpFormat() throws Exception {
        OtpManager manager = new OtpManager();
        Method method = OtpManager.class.getDeclaredMethod("generateOtp");
        method.setAccessible(true);
        String otp = (String) method.invoke(manager);

        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }
}