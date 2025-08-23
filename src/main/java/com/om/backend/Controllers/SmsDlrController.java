package com.om.backend.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController

public class SmsDlrController {
    private static final Logger log = LoggerFactory.getLogger(SmsDlrController.class);

    @GetMapping("/webhooks/sms-dlr")
    public String dlr(
            @RequestParam(required = false) String messageId,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String submitDate,
            @RequestParam(required = false) String doneDate,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String shortMessage
    ) {
        log.info("DLR: id={} mobile={} status={} submit={} done={} err={} msg={}",
                messageId, mobile, status, submitDate, doneDate, errorCode, shortMessage);
        return "OK";
    }
}
