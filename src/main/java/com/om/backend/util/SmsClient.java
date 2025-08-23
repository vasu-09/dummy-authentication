package com.om.backend.util;

import com.om.backend.Config.SmsProperties;
import com.om.backend.Dto.SendSmsRequest;
import com.om.backend.Dto.SendSmsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SmsClient {

    @Autowired
    private  WebClient smsWebClient;
    @Autowired
    private  SmsProperties props;

    public SendSmsResponse sendOtpMessage(String message, String e164Mobile, boolean requestDlr) {
        SendSmsRequest req = new SendSmsRequest(
                props.getApiKey(),
                props.getClientId(),
                props.getSenderId(),
                message,
                e164Mobile,
                props.getDlt().getTemplateId(),
                null, null,
                requestDlr ? "true" : null,
                null, null, null, null
        );

        return smsWebClient.post()
                .uri("/SendSMS")
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(SendSmsResponse.class)
                .onErrorResume(ex -> {
                    SendSmsResponse r = new SendSmsResponse();
                    r.setErrorCode(-1);
                    r.setErrorDescription(ex.getMessage());
                    return Mono.just(r);
                })
                .block();
    }
}