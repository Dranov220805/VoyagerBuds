package com.example.voyagerbuds.utils;

import android.os.AsyncTask;
import android.util.Log;

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

    private static final String TAG = "EmailSender";

    // SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587"; // TLS
    private static final String SMTP_USER = com.example.voyagerbuds.BuildConfig.SMTP_USER;
    private static final String SMTP_PASSWORD = com.example.voyagerbuds.BuildConfig.SMTP_PASSWORD;
    private static final String SMTP_FROM_EMAIL = com.example.voyagerbuds.BuildConfig.SMTP_FROM_EMAIL;
    private static final String SMTP_FROM_NAME = com.example.voyagerbuds.BuildConfig.SMTP_FROM_NAME;

    public interface EmailCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    public static void sendEmergencyEmail(String recipientEmail, String subject, String body, EmailCallback callback) {
        new SendEmailTask(recipientEmail, subject, body, callback).execute();
    }

    public static void sendEmailSync(String recipientEmail, String subject, String body) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_FROM_EMAIL, SMTP_FROM_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Exception> {
        private final String recipientEmail;
        private final String subject;
        private final String body;
        private final EmailCallback callback;

        SendEmailTask(String recipientEmail, String subject, String body, EmailCallback callback) {
            this.recipientEmail = recipientEmail;
            this.subject = subject;
            this.body = body;
            this.callback = callback;
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                sendEmailSync(recipientEmail, subject, body);
                return null; // Success
            } catch (Exception e) {
                Log.e(TAG, "Error sending email", e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception error) {
            if (callback != null) {
                if (error == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(error);
                }
            }
        }
    }
}
