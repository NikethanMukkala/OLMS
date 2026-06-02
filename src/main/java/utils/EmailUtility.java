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
            // Load Brevo key from config.properties if not set via env var
            if (BREVO_API_KEY == null || BREVO_API_KEY.isEmpty()) {
                BREVO_API_KEY = props.getProperty("brevo.api.key", "");
            }
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

            if ("true".equals(System.getenv("RENDER"))) {
                try {
                    String htmlBody = buildHtmlEmail("Your OTP Code",
                        "<p>Your one-time password (OTP) for OLMS login is:</p>" +
                        "<div style='font-size:32px;font-weight:bold;letter-spacing:8px;color:#4f46e5;margin:20px 0;'>" + otp + "</div>" +
                        "<p>This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>");
                    String textBody = "Your OTP for OLMS login is: " + otp + "\n\nThis OTP is valid for 5 minutes.";
                    sendViaBrevoApi(recipientEmail, "OLMS - Your Login OTP", textBody, htmlBody, null, null);
                } catch (Exception e) {
                    logger.error("Failed to send OTP via Brevo API: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            } else {
                System.out.println("[DEBUG] Local SMTP fallback for OTP sending to: " + recipientEmail);
                System.out.println("[DEBUG] SENDER_EMAIL: " + SENDER_EMAIL);
                System.out.println("[DEBUG] APP_PASSWORD length: " + (APP_PASSWORD != null ? APP_PASSWORD.length() : 0));
                
                Properties props = getMailProperties();
                final String safePassword = APP_PASSWORD != null ? APP_PASSWORD.replace(" ", "") : "";

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, safePassword);
                    }
                });

                try {
                    String htmlBody = buildHtmlEmail("Your OTP Code",
                        "<p>Your one-time password (OTP) for OLMS login is:</p>" +
                        "<div style='font-size:32px;font-weight:bold;letter-spacing:8px;color:#4f46e5;margin:20px 0;'>" + otp + "</div>" +
                        "<p>This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>");

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(SENDER_EMAIL, "OLMS Library"));
                    message.setReplyTo(new Address[]{new InternetAddress(SENDER_EMAIL, "OLMS Library")});
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    message.setSubject("OLMS - Your Login OTP");
                    message.setSentDate(new java.util.Date());
                    message.setContent(htmlBody, "text/html; charset=UTF-8");

                    Transport.send(message);
                    logger.info("OTP successfully sent via JavaMail SMTP to {}", recipientEmail);
                    System.out.println("[DEBUG] OTP successfully sent!");

                } catch (AuthenticationFailedException e) {
                    logger.error("SMTP Authentication Failed. Ensure SENDER_EMAIL and APP_PASSWORD are set correctly.", e);
                    System.err.println("[DEBUG] SMTP Authentication Failed:");
                    e.printStackTrace();
                } catch (MessagingException e) {
                    logger.error("Failed to send OTP email: {}", e.getMessage(), e);
                    System.err.println("[DEBUG] MessagingException:");
                    e.printStackTrace();
                } catch (Exception e) {
                    logger.error("Unexpected error: {}", e.getMessage(), e);
                    System.err.println("[DEBUG] Unexpected Exception:");
                    e.printStackTrace();
                }
            }
        });
    }

    public static void sendEmail(String recipientEmail, String subject, String body) {
        CompletableFuture.runAsync(() -> {
            if ("true".equals(System.getenv("RENDER"))) {
                try {
                    String htmlBody = buildHtmlEmail(subject,
                        "<p style='white-space:pre-line;'>" + body.replace("\n", "<br>") + "</p>");
                    sendViaBrevoApi(recipientEmail, subject, body, htmlBody, null, null);
                } catch (Exception e) {
                    logger.error("Failed to send email via Brevo API: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            } else {
                System.out.println("[DEBUG] Local SMTP fallback for sendEmail to: " + recipientEmail);
                System.out.println("[DEBUG] SENDER_EMAIL: " + SENDER_EMAIL);
                
                Properties props = getMailProperties();
                final String safePassword = APP_PASSWORD != null ? APP_PASSWORD.replace(" ", "") : "";

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, safePassword);
                    }
                });

                try {
                    String htmlBody = buildHtmlEmail(subject,
                        "<p style='white-space:pre-line;'>" + body.replace("\n", "<br>") + "</p>");

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(SENDER_EMAIL, "OLMS Library"));
                    message.setReplyTo(new Address[]{new InternetAddress(SENDER_EMAIL, "OLMS Library")});
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    message.setSubject(subject);
                    message.setSentDate(new java.util.Date());
                    message.setContent(htmlBody, "text/html; charset=UTF-8");

                    Transport.send(message);
                    logger.info("Email successfully sent to {}", recipientEmail);
                    System.out.println("[DEBUG] Email successfully sent to: " + recipientEmail);

                } catch (Exception e) {
                    logger.error("Failed to send email to {}: {}", recipientEmail, e.getMessage(), e);
                    System.err.println("[DEBUG] Failed to send email:");
                    e.printStackTrace();
                }
            }
        });
    }

    public static void sendEmailWithAttachment(String recipientEmail, String subject, String body, byte[] attachmentData, String attachmentName) {
        CompletableFuture.runAsync(() -> {
            if ("true".equals(System.getenv("RENDER"))) {
                try {
                    String htmlBody = buildHtmlEmail(subject,
                        "<p style='white-space:pre-line;'>" + body.replace("\n", "<br>") + "</p>");
                    sendViaBrevoApi(recipientEmail, subject, body, htmlBody, attachmentData, attachmentName);
                } catch (Exception e) {
                    logger.error("Failed to send email with attachment via Brevo API: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            } else {
                System.out.println("[DEBUG] Local SMTP fallback for sendEmailWithAttachment to: " + recipientEmail);
                System.out.println("[DEBUG] SENDER_EMAIL: " + SENDER_EMAIL);
                
                Properties props = getMailProperties();
                final String safePassword = APP_PASSWORD != null ? APP_PASSWORD.replace(" ", "") : "";

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, safePassword);
                    }
                });

                try {
                    String htmlBody = buildHtmlEmail(subject,
                        "<p style='white-space:pre-line;'>" + body.replace("\n", "<br>") + "</p>");

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(SENDER_EMAIL, "OLMS Library"));
                    message.setReplyTo(new Address[]{new InternetAddress(SENDER_EMAIL, "OLMS Library")});
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    message.setSubject(subject);
                    message.setSentDate(new java.util.Date());

                    // Build multipart: HTML body + attachment
                    MimeBodyPart htmlBodyPart = new MimeBodyPart();
                    htmlBodyPart.setContent(htmlBody, "text/html; charset=UTF-8");

                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    javax.activation.DataSource source = new javax.mail.util.ByteArrayDataSource(attachmentData, "application/pdf");
                    attachmentBodyPart.setDataHandler(new javax.activation.DataHandler(source));
                    attachmentBodyPart.setFileName(attachmentName);

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(htmlBodyPart);
                    multipart.addBodyPart(attachmentBodyPart);

                    message.setContent(multipart);

                    Transport.send(message);
                    logger.info("Email with attachment successfully sent to {}", recipientEmail);
                    System.out.println("[DEBUG] Email with attachment successfully sent to: " + recipientEmail);

                } catch (Exception e) {
                    logger.error("Failed to send email with attachment to {}: {}", recipientEmail, e.getMessage(), e);
                    System.err.println("[DEBUG] Failed to send email with attachment:");
                    e.printStackTrace();
                }
            }
        });
    }

    private static void sendViaBrevoApi(String recipientEmail, String subject, String textBody, String htmlBody, byte[] attachmentData, String attachmentName) throws Exception {
        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("api-key", BREVO_API_KEY);
        conn.setDoOutput(true);

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        
        com.google.gson.JsonObject sender = new com.google.gson.JsonObject();
        String senderEmail = SENDER_EMAIL != null && !SENDER_EMAIL.isEmpty() ? SENDER_EMAIL : "olmsbb4@gmail.com";
        sender.addProperty("email", senderEmail);
        sender.addProperty("name", "OLMS Library");
        payload.add("sender", sender);

        com.google.gson.JsonObject replyTo = new com.google.gson.JsonObject();
        replyTo.addProperty("email", senderEmail);
        replyTo.addProperty("name", "OLMS Library");
        payload.add("replyTo", replyTo);
        
        com.google.gson.JsonArray to = new com.google.gson.JsonArray();
        com.google.gson.JsonObject recipient = new com.google.gson.JsonObject();
        recipient.addProperty("email", recipientEmail);
        to.add(recipient);
        payload.add("to", to);
        
        payload.addProperty("subject", subject);
        if (textBody != null) {
            payload.addProperty("textContent", textBody);
        }
        if (htmlBody != null) {
            payload.addProperty("htmlContent", htmlBody);
        }

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
                try (java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A")) {
                    String errorResponse = s.hasNext() ? s.next() : "";
                    logger.error("Brevo API Error: {} - {}", responseCode, errorResponse);
                }
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

    private static String buildHtmlEmail(String title, String contentHtml) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'>" +
            "<style>" +
            "body{font-family:Arial,sans-serif;background:#f4f4f7;margin:0;padding:0;}" +
            ".wrapper{max-width:560px;margin:40px auto;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);}" +
            ".header{background:#4f46e5;padding:28px 32px;}" +
            ".header h1{color:#ffffff;margin:0;font-size:22px;letter-spacing:0.5px;}"+
            ".body{padding:32px;color:#333333;line-height:1.6;}" +
            ".body h2{color:#4f46e5;margin-top:0;}" +
            ".footer{background:#f4f4f7;padding:16px 32px;font-size:12px;color:#888888;text-align:center;}" +
            "</style></head><body>" +
            "<div class='wrapper'>" +
            "<div class='header'><h1>&#128218; OLMS - Online Library Management</h1></div>" +
            "<div class='body'>" +
            "<h2>" + title + "</h2>" +
            contentHtml +
            "</div>" +
            "<div class='footer'>This is an automated message from OLMS. Please do not reply to this email.</div>" +
            "</div></body></html>";
    }
}
