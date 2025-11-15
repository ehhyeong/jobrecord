//2233076 10주차 추가

package com.jobproj.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 이메일을 비동기로 발송합니다.
     * @param toEmail 받는 사람 이메일
     * @param subject 제목
     * @param text 내용
     */
    @Async
    public void sendEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            // message.setFrom("보내는사람@gmail.com"); // application.yml의 username과 동일하면 생략 가능

            mailSender.send(message);
            log.info("EmailService: {}님에게 메일 발송 성공", toEmail);

        } catch (Exception e) {
            log.error("EmailService: {}님에게 메일 발송 실패: {}", toEmail, e.getMessage());
            // (실패해도 예외를 던지지 않음 - 메일 발송 실패가
            //  비밀번호 찾기 로직 전체를 중단시키면 안 됨)
        }
    }
}