package com.dodo.backend.mail.service;

import jakarta.mail.BodyPart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GmailServiceImpl}이 회원 탈퇴 인증 메일을 만들 때 지켜야 하는 MIME 구조를 검증합니다.
 * <p>
 * 메일 템플릿은 로고를 {@code cid:logo}로 참조하므로, 생성된 메시지는 HTML 본문과 Logo.png를 같은
 * {@code multipart/related} 파트 안에 담아야 합니다. 그래야 웹메일 클라이언트에서 로고가 일반
 * 첨부파일처럼 노출되지 않습니다.
 */
@ExtendWith(MockitoExtension.class)
class GmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private GmailServiceImpl gmailService;

    /**
     * Logo.png가 다운로드 가능한 첨부파일이 아니라 CID 인라인 이미지로 전송되는지 확인합니다.
     * <p>
     * Mock 처리된 {@link JavaMailSender}는 실제 발송 전처럼 MIME 헤더를 확정하지 않으므로,
     * 검증 전에 {@link MimeMessage#saveChanges()}를 호출합니다.
     */
    @Test
    @DisplayName("로고 이미지를 첨부가 아닌 multipart/related 인라인 이미지로 전송한다")
    void sendWithdrawalEmail_LogoInlineRelated() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(any(String.class), any(Context.class)))
                .thenReturn("<html><body><img src=\"cid:logo\" alt=\"DoDo\"></body></html>");
        ReflectionTestUtils.setField(gmailService, "fromEmail", "noreply@dodo.com");

        gmailService.sendWithdrawalEmail("user@example.com");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        sentMessage.saveChanges();
        assertThat(sentMessage.getContentType()).startsWith("multipart/related");

        MimeMultipart multipart = (MimeMultipart) sentMessage.getContent();
        assertThat(multipart.getCount()).isEqualTo(2);

        BodyPart logoPart = multipart.getBodyPart(1);
        assertThat(logoPart.getDisposition()).isEqualTo(Part.INLINE);
        assertThat(logoPart.getContentType()).startsWith("image/png");
        assertThat(logoPart.getHeader("Content-ID")).containsExactly("<logo>");
    }
}
