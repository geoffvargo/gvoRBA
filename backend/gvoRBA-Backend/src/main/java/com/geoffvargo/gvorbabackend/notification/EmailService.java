package com.geoffvargo.gvorbabackend.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.*;
import org.thymeleaf.context.*;

import java.nio.charset.*;
import java.util.*;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import lombok.*;
import lombok.extern.slf4j.*;

/**
 * Renders Thymeleaf templates into HTML emails and sends them through the configured
 * {@link JavaMailSender}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class EmailService {
	private final JavaMailSender mailSender;
	private final ITemplateEngine templateEngine;
	
	/// Sender address. lombok.config copies @Value onto the generated constructor parameter,
	/// which is what lets this stay final.
	@Value("${app.mail.from}")
	private final String from;
	
	/**
	 * Renders {@code template} with {@code model} and sends the result as an HTML email.
	 *
	 * @param to
	 * 	recipient address
	 * @param subject
	 * 	subject line
	 * @param template
	 * 	Thymeleaf template name, relative to {@code templates/} and without
	 * 	the extension (e.g. {@code email/booking-confirmed})
	 * @param model
	 * 	variables exposed to the template
	 * @param attachments
	 * 	files to attach; may be empty but not {@code null}
	 * @throws IllegalStateException
	 * 	if the message cannot be assembled
	 * @throws org.springframework.mail.MailException
	 * 	if the SMTP server rejects or cannot be reached
	 */
	public void sendHtml(String to, String subject, String template, Map<String, Object> model,
	                     List<EmailAttachment> attachments) {
		log.info("sendHtml() called.");
		
		/// Render the template body before touching the mail server.
		Context ctx = new Context(); // Thymeleaf variable holder (non-web, so no request/session)
		ctx.setVariables(model); // expose each model entry as ${key} in the template
		String html = templateEngine.process(template, ctx); // resolve the template and render it to an HTML string
		
		MimeMessage message = mailSender.createMimeMessage();
		
		try {
			/// multipart = true: body and attachments become separate MIME parts; addAttachment() throws without it.
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			
			/// true = body is HTML, not plain text.
			helper.setText(html, true);
			
			for (EmailAttachment attachment : attachments) {
				helper.addAttachment(
					attachment.filename(),
					new ByteArrayResource(attachment.content()),
					attachment.contentType());
			}
		} catch (MessagingException e) {
			throw new IllegalStateException("Failed to build email.");
		}
		
		/// Blocks on SMTP; callers are expected to invoke this off the request thread.
		mailSender.send(message);
	}
}
