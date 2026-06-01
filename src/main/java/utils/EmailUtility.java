package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailUtility {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailUtility.class);

    // Credentials loaded from configuration
    private static String SENDER_EMAIL;
    private static String APP_PASSWORD;

    static {
        try (java.io.InputStream input = EmailUtility.class.getClassLoader().getResourceAsStream("config.properties")) {
            java.util.Properties props = new java.util.Properties();
            if (input != null) props.load(input);
            SENDER_EMAIL = props.getProperty("email.user", "");
            APP_PASSWORD = props.getProperty("email.password", "");
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
        });
    }

    public static void sendEmail(String recipientEmail, String subject, String body) {
        CompletableFuture.runAsync(() -> {
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
        });
    }

    public static void sendEmailWithAttachment(String recipientEmail, String subject, String body, byte[] attachmentData, String attachmentName) {
        CompletableFuture.runAsync(() -> {
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
        });
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
