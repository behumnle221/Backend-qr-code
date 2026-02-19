package com.fapshi.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetCodeEmail(String to, String code, String userName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Réinitialisation de mot de passe - Qr-CodePay");

        String greeting = (userName != null && !userName.trim().isEmpty()) 
            ? "Bonjour " + userName + "," 
            : "Bonjour,";

        String htmlContent = """
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Réinitialisation - Qr-CodePay</title>
                <style>
                    body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 30px auto; background: #fff; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden; }
                    .header { background: #007BFF; color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 30px; text-align: center; color: #333; }
                    .code-box { font-size: 36px; font-weight: bold; letter-spacing: 8px; background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; display: inline-block; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 14px; color: #777; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Réinitialisation de mot de passe</h1>
                    </div>
                    <div class="content">
                        <p>%s</p>
                        <p>Voici votre code de réinitialisation pour <strong>Qr-CodePay</strong> :</p>
                        <div class="code-box">%s</div>
                        <p>Ce code est valide pendant <strong>10 minutes</strong>.</p>
                        <p>Ne partagez jamais ce code avec qui que ce soit.</p>
                        <p>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>
                    </div>
                    <div class="footer">
                        <p>Cordialement,<br>L'équipe Qr-CodePay</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(greeting, code);

        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
    }
}