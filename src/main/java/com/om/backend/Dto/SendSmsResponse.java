package com.om.backend.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/** Map to your provider’s JSON schema (typical pattern below). */
@Data
public class SendSmsResponse {
    @JsonProperty("ErrorCode")
    private Integer errorCode;
    @JsonProperty("ErrorDescription")
    private String errorDescription;
    @JsonProperty("Data")
    private List<DataItem> data;
    @Data
    public static class DataItem {
        @JsonProperty("MobileNumber")
        private String mobileNumber;
        @JsonProperty("MessageId")
        private String messageId;
    }


    public boolean isOk() {
        return errorCode == null || errorCode == 0;
    }
}
