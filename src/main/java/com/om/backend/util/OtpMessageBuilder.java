package com.om.backend.util;

import com.om.backend.Config.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OtpMessageBuilder {

    @Autowired
    private  SmsProperties props;

    /** Replace first {#var#} with OTP, second {#var#} with TTL minutes. */
    public String build(String otp) {
        String content = props.getDlt().getContent();
        content = content.replaceFirst("\\{#var#\\}", otp);
        return content;
    }
}
