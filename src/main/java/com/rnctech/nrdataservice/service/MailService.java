package com.rnctech.nrdataservice.service;

import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.rnctech.nrdataservice.resource.EmailRequest;

/**
 * @author Zilin Chen
 * @since 2020.01
 */

@Service
public class MailService {
	
	public static Logger logger = Logger.getLogger(MailService.class);
			
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailProperties mailProperties;
    
    @Value("${spring.mail.sendto}")
    private String sendto;
    
    public String sendMail(EmailRequest mailreq) {
    	
        String subject = mailreq.getSubject();
        String body = mailreq.getBody();
        String recip = (null != sendto)? sendto: mailreq.getEmail();

        sendMail(mailProperties.getUsername(), recip, subject, body);
        return subject + " sent to "+recip;
    }
    
    private void sendMail(String fromEmail, String toEmail, String subject, String body) {
        try {
            logger.debug("Sending Email to " + toEmail);
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper messageHelper = new MimeMessageHelper(message, StandardCharsets.UTF_8.toString());
            messageHelper.setSubject(subject);
            messageHelper.setText(body, true);
            messageHelper.setFrom(fromEmail);
            messageHelper.setTo(toEmail);

            mailSender.send(message);
        } catch (MessagingException ex) {
            logger.error("Failed to send email to "+toEmail);
        }
    }

	public String getSendto() {
		return sendto;
	}

	public void setSendto(String sendto) {
		this.sendto = sendto;
	}
}
