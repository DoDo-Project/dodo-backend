package com.dodo.backend.mail.service;

import com.dodo.backend.common.util.VerificationCodeGenerator;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Google SMTP(Gmail)를 이용하여 실제 메일 발송을 수행하는 {@link MailService} 구현체입니다.
 * <p>
 * Thymeleaf 템플릿 엔진을 활용하여 HTML 형식의 메일 본문을 생성하며,
 * application.yml에 정의된 발신자 정보를 사용하여 인증 메일을 전송합니다.
 */
@Service
@RequiredArgsConstructor
public class GmailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * {@inheritDoc}
     * <p>
     * {@link VerificationCodeGenerator}를 통해 6자리 번호를 생성하고,
     * 전용 HTML 템플릿을 조립하여 메일을 발송한 후 생성된 코드를 반환합니다.
     * <p>
     * Logo.png는 메일 템플릿의 {@code cid:logo}에서 참조하는 CID 인라인 이미지로 전송합니다.
     * 네이버 메일 같은 웹메일 클라이언트에서 로고가 일반 첨부파일로 노출되지 않고 HTML 본문 안에
     * 렌더링되도록 {@link MimeMessageHelper#MULTIPART_MODE_RELATED} 구조를 사용해야 합니다.
     */
    @Override
    public String sendWithdrawalEmail(String email) {

        String code = VerificationCodeGenerator.generateCode();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[DODO] 계정 탈퇴 본인 확인 인증 번호");

            Context context = new Context();
            context.setVariable("code", code);
            String htmlContent = templateEngine.process("mail/withdrawal-verification", context);

            helper.setText(htmlContent, true);
            helper.addInline("logo", new ClassPathResource("templates/mail/Logo.png"), "image/png");
            helper.setFrom(fromEmail);

            mailSender.send(message);

            return code;

        } catch (Exception e) {
            throw new RuntimeException("메일 발송 중 기술적 오류 발생", e);
        }
    }
}
