/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team.apk.Service;

/**
 *
 * @author HP
 */

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

//Service responsable de l'envoi des emails.
//Ici, il sert surtout à envoyer le code de vérification.

@Service
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final int expirationMinutes;
    private final String fromAddress;

    public EmailDeliveryService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                @Value("${app.email-verification.code-expiration-minutes:10}") int expirationMinutes,
                                @Value("${spring.mail.username:no-reply@app.local}") String fromAddress) {
        // getIfAvailable évite une erreur directe si SMTP n'est pas configuré.
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.expirationMinutes = expirationMinutes;
        this.fromAddress = fromAddress;
    }
    
//    Envoie un email contenant le code de vérification.
    public void sendVerificationCode(String destinationEmail, String verificationCode) {
        if (mailSender == null) {
            throw new IllegalStateException("SMTP non configuré.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(destinationEmail);
        message.setSubject("Votre code de vérification");
        message.setText(
                "Bonjour,\n\n" +
                "Votre code de vérification est : " + verificationCode + "\n\n" +
                "Ce code expire dans " + expirationMinutes + " minutes.\n" +
                "Si vous demandez un nouveau code, l'ancien devient invalide.\n"
        );

        mailSender.send(message);
    }
    
    
    //    Envoie un email contenant le code de réinitialisation de mot de passe.
    public void sendPasswordResetCode(String destinationEmail, String resetCode) {
        if (mailSender == null) {
            throw new IllegalStateException("SMTP non configuré.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(destinationEmail);
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText(
                "Bonjour,\n\n" +
                "Vous avez demandé à réinitialiser votre mot de passe.\n\n" +
                "Votre code de réinitialisation est : " + resetCode + "\n\n" +
                "Ce code expire dans " + expirationMinutes + " minutes.\n" +
                "Si vous n'avez pas effectué cette demande, ignorez cet email.\n"
        );

        mailSender.send(message);
    }
}
