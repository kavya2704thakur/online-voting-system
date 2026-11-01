package com.voting.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendEmail(String toEmail, String subject, String body) {
        if (mailSender == null) {
            System.out.println("Mail sender not active. Skipping email to: " + toEmail);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            System.out.println("Email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Email notification skipped: " + e.getMessage());
        }
    }
}