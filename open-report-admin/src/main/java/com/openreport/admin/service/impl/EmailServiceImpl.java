package com.openreport.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.openreport.admin.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${open-report.email.from}")
    private String from;

    @Value("${open-report.email.senderName:Open Report}")
    private String senderName;

    @Override
    public boolean sendEmail(String to, String subject, String content) {
        return sendEmail(Collections.singletonList(to), null, subject, content);
    }

    @Override
    public boolean sendEmail(String to, String cc, String subject, String content) {
        List<String> toList = parseEmails(to);
        List<String> ccList = parseEmails(cc);
        return sendEmail(toList, ccList, subject, content);
    }

    @Override
    public boolean sendEmail(List<String> toList, List<String> ccList, String subject, String content) {
        if (toList == null || toList.isEmpty()) {
            logger.warn("收件人列表为空，跳过邮件发送");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(buildFromAddress());
            message.setTo(toList.toArray(new String[0]));
            if (ccList != null && !ccList.isEmpty()) {
                message.setCc(ccList.toArray(new String[0]));
            }
            message.setSubject(subject);
            message.setText(content, true);
            mailSender.send(message);
            logger.info("邮件发送成功，收件人：{}，主题：{}", toList, subject);
            return true;
        } catch (Exception e) {
            logger.error("邮件发送失败，收件人：{}，主题：{}", toList, subject, e);
            return false;
        }
    }

    @Override
    public boolean sendEmailWithAttachment(String to, String subject, String content, File attachment) {
        return sendEmailWithAttachment(Collections.singletonList(to), null, subject, content, attachment);
    }

    @Override
    public boolean sendEmailWithAttachment(String to, String cc, String subject, String content, File attachment) {
        List<String> toList = parseEmails(to);
        List<String> ccList = parseEmails(cc);
        List<File> attachments = attachment != null ? Collections.singletonList(attachment) : null;
        return sendEmailWithAttachments(toList, ccList, subject, content, attachments);
    }

    @Override
    public boolean sendEmailWithAttachment(List<String> toList, List<String> ccList, String subject, String content, File attachment) {
        List<File> attachments = attachment != null ? Collections.singletonList(attachment) : null;
        return sendEmailWithAttachments(toList, ccList, subject, content, attachments);
    }

    @Override
    public boolean sendEmailWithAttachments(List<String> toList, List<String> ccList, String subject, String content, List<File> attachments) {
        if (toList == null || toList.isEmpty()) {
            logger.warn("收件人列表为空，跳过邮件发送");
            return false;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(buildFromAddress());
            helper.setTo(toList.toArray(new String[0]));
            if (ccList != null && !ccList.isEmpty()) {
                helper.setCc(ccList.toArray(new String[0]));
            }
            helper.setSubject(subject);
            helper.setText(content, true);

            if (attachments != null) {
                for (File file : attachments) {
                    if (file != null && file.exists()) {
                        FileSystemResource resource = new FileSystemResource(file);
                        helper.addAttachment(file.getName(), resource);
                    }
                }
            }

            mailSender.send(mimeMessage);
            logger.info("带附件邮件发送成功，收件人：{}，主题：{}，附件数：{}", toList, subject, attachments != null ? attachments.size() : 0);
            return true;
        } catch (Exception e) {
            logger.error("带附件邮件发送失败，收件人：{}，主题：{}", toList, subject, e);
            return false;
        }
    }

    private String buildFromAddress() {
        try {
            return javax.mail.internet.MimeUtility.encodeText(senderName) + " <" + from + ">";
        } catch (Exception e) {
            return from;
        }
    }

    private List<String> parseEmails(String emails) {
        if (StrUtil.isBlank(emails)) {
            return new ArrayList<>();
        }
        return Arrays.stream(emails.split("[,;，；]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
