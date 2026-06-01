package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmailUtility {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailUtility.class);

    // Credentials loaded from configuration
    private static String SENDER_EMAIL;
    private static String APP_PASSWORD;
    private static String BREVO_API_KEY;

    static {
        SENDER_EMAIL = System.getenv("EMAIL_USER");
        APP_PASSWORD = System.getenv("EMAIL_PASSWORD");
        BREVO_API_KEY = System.getenv("BREVO_API_KEY");

        try (java.io.InputStream input = EmailUtility.class.getClassLoader().getResourceAsStream("config.properties")) {
            java.util.Properties props = new java.util.Properties();
            if (input != null) props.load(input);
            if (SENDER_EMAIL == null) SENDER_EMAIL = props.getProperty("email.user", "");
            if (APP_PASSWORD == null) APP_PASSWORD = props.getProperty("email.password", "");
        } catch (Exception e) {
            logger.error("Failed to load email config: {}", e.getMessage(), e);
        }
    }

    public static void sendOTP(String recipientEmail, String otp) {
        CompletableFuture.runAsync(() -> {
            logger.info("========== OTP GENERATED ==========");
            logger.info("Recipient: {}", recipientEmail);
            logger.info("OTP: {}", otp);
            logger.info("===================================");

            try {
                sendViaBrevoApi(recipientEmail, "OLMS - Your Login OTP", "Your OTP for OLMS login is: " + otp + "\n\nThis OTP is valid for 5 minutes.", null, null);
            } catch (Exception e) {
                logger.error("Failed to send OTP via Brevo API: {}", e.getMessage(), e);
            }
            
            /* -- SMTP Logic Commented Out for Render Compatibility --
            Properties props = getMailProperties();
            final String safePassword = APP_PASSWORD.replace(" ", "");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, safePassword);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject("OLMS - Your Login OTP");
                message.setText("Your OTP for OLMS login is: " + otp + "\n\nThis OTP is valid for 5 minutes.");

                Transport.send(message);
                logger.info("OTP successfully sent via JavaMail SMTP to {}", recipientEmail);

            } catch (AuthenticationFailedException e) {
                logger.error("SMTP Authentication Failed. Ensure SENDER_EMAIL and APP_PASSWORD are set correctly.", e);
            } catch (MessagingException e) {
                logger.error("Failed to send OTP email: {}", e.getMessage(), e);
            }
            */
        });
    }

    public static void sendEmail(String recipientEmail, String subject, String body) {
        CompletableFuture.runAsync(() -> {
            try {
                sendViaBrevoApi(recipientEmail, subject, body, null, null);
            } catch (Exception e) {
                logger.error("Failed to send email via Brevo API: {}", e.getMessage(), e);
            }
            
            /* -- SMTP Logic Commented Out for Render Compatibility --
            Properties props = getMailProperties();
            final String safePassword = APP_PASSWORD.replace(" ", "");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, safePassword);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                logger.info("Email successfully sent to {}", recipientEmail);

            } catch (Exception e) {
                logger.error("Failed to send email to {}: {}", recipientEmail, e.getMessage(), e);
            }
            */
        });
    }

    public static void sendEmailWithAttachment(String recipientEmail, String subject, String body, byte[] attachmentData, String attachmentName) {
        CompletableFuture.runAsync(() -> {
            try {
                sendViaBrevoApi(recipientEmail, subject, body, attachmentData, attachmentName);
            } catch (Exception e) {
                logger.error("Failed to send email with attachment via Brevo API: {}", e.getMessage(), e);
            }
            
            /* -- SMTP Logic Commented Out for Render Compatibility --
            Properties props = getMailProperties();
            final String safePassword = APP_PASSWORD.replace(" ", "");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, safePassword);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);

                MimeBodyPart textBodyPart = new MimeBodyPart();
                textBodyPart.setText(body);

                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                javax.activation.DataSource source = new javax.mail.util.ByteArrayDataSource(attachmentData, "application/pdf");
                attachmentBodyPart.setDataHandler(new javax.activation.DataHandler(source));
                attachmentBodyPart.setFileName(attachmentName);

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textBodyPart);
                multipart.addBodyPart(attachmentBodyPart);

                message.setContent(multipart);

                Transport.send(message);
                logger.info("Email with attachment successfully sent to {}", recipientEmail);

            } catch (Exception e) {
                logger.error("Failed to send email with attachment to {}: {}", recipientEmail, e.getMessage(), e);
            }
            */
        });
    }

    private static void sendViaBrevoApi(String recipientEmail, String subject, String body, byte[] attachmentData, String attachmentName) throws Exception {
        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("api-key", BREVO_API_KEY);
        conn.setDoOutput(true);

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        
        com.google.gson.JsonObject sender = new com.google.gson.JsonObject();
        sender.addProperty("email", SENDER_EMAIL != null && !SENDER_EMAIL.isEmpty() ? SENDER_EMAIL : "nikethanmk@gmail.com");
        sender.addProperty("name", "OLMS");
        payload.add("sender", sender);
        
        com.google.gson.JsonArray to = new com.google.gson.JsonArray();
        com.google.gson.JsonObject recipient = new com.google.gson.JsonObject();
        recipient.addProperty("email", recipientEmail);
        to.add(recipient);
        payload.add("to", to);
        
        payload.addProperty("subject", subject);
        payload.addProperty("textContent", body);

        if (attachmentData != null && attachmentName != null) {
            com.google.gson.JsonArray attachments = new com.google.gson.JsonArray();
            com.google.gson.JsonObject attachment = new com.google.gson.JsonObject();
            attachment.addProperty("content", java.util.Base64.getEncoder().encodeToString(attachmentData));
            attachment.addProperty("name", attachmentName);
            attachments.add(attachment);
            payload.add("attachment", attachments);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            logger.info("Email successfully sent via Brevo API to {}", recipientEmail);
        } else {
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                String errorResponse = s.hasNext() ? s.next() : "";
                logger.error("Brevo API Error: {} - {}", responseCode, errorResponse);
            } else {
                logger.error("Brevo API Error with response code: {}", responseCode);
            }
        }
    }

    private static Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return props;
    }
}
