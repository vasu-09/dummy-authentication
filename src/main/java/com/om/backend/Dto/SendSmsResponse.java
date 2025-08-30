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

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public List<DataItem> getData() {
        return data;
    }

    public void setData(List<DataItem> data) {
        this.data = data;
    }

    @Data
    public static class DataItem {
        @JsonProperty("MobileNumber")
        private String mobileNumber;
        @JsonProperty("MessageId")
        private String messageId;

        public String getMobileNumber() {
            return mobileNumber;
        }

        public void setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
    }


    public boolean isOk() {
        return errorCode == null || errorCode == 0;
    }
}
