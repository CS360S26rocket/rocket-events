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

// This class handles sending OTP emails for password reset functionality. It uses JavaMail API to send an email from a predefined sender's Gmail account.
// The `sendOtpEmail` method takes the recipient's email and the generated OTP as parameters, then configures the email session, authenticates the sender, 
// and sends the OTP email with a subject and message body. The email is sent via Gmail's SMTP server with TLS encryption enabled.

public class EmailSender {
    private static final String SENDER_EMAIL = "mirhuzaifa230@gmail.com";
    private static final String APP_PASSWORD = "rlly klcs qvsb mcrn";
    public static void sendOtpEmail(String toEmail, String otp) throws MessagingException {
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
        message.setSubject("Your password reset OTP");
        message.setText("Your OTP is: " + otp + "\n\nThis code expires in 10 minutes.");

        Transport.send(message);
    }
}
