/*
 * This file defines EmailSender, a supporting Java class used by the Scene app.
 * It contains email delivery support for OTP and notification-style messages.
 * Its functions include sendOtpEmail, sendPlainEmail, getPasswordAuthentication to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;





public class EmailSender {
    private static final String SENDER_EMAIL = "mirhuzaifa230@gmail.com";
    private static final String APP_PASSWORD = "rlly klcs qvsb mcrn";
    public static void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        sendPlainEmail(
                toEmail,
                "Your password reset OTP",
                "Your OTP is: " + otp + "\n\nThis code expires in 10 minutes."
        );
    }

    public static void sendPlainEmail(String toEmail, String subject,
                                      String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }
}
