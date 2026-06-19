package com.openreport.admin.service;

import java.io.File;
import java.util.List;

public interface EmailService {

    boolean sendEmail(String to, String subject, String content);

    boolean sendEmail(String to, String cc, String subject, String content);

    boolean sendEmail(List<String> toList, List<String> ccList, String subject, String content);

    boolean sendEmailWithAttachment(String to, String subject, String content, File attachment);

    boolean sendEmailWithAttachment(String to, String cc, String subject, String content, File attachment);

    boolean sendEmailWithAttachment(List<String> toList, List<String> ccList, String subject, String content, File attachment);

    boolean sendEmailWithAttachments(List<String> toList, List<String> ccList, String subject, String content, List<File> attachments);
}
