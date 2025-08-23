package com.om.backend.Dto;

import lombok.Data;
import java.util.List;

/** Map to your provider’s JSON schema (typical pattern below). */
@Data
public class SendSmsResponse {
    private int ErrorCode;
    private String ErrorDescription;
    private List<DataItem> Data;

    public int getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(int errorCode) {
        ErrorCode = errorCode;
    }

    public String getErrorDescription() {
        return ErrorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        ErrorDescription = errorDescription;
    }

    public List<DataItem> getData() {
        return Data;
    }

    public void setData(List<DataItem> data) {
        Data = data;
    }

    @Data
    public static class DataItem {
        private String MobileNumber;
        private String MessageId;

        public String getMobileNumber() {
            return MobileNumber;
        }

        public void setMobileNumber(String mobileNumber) {
            MobileNumber = mobileNumber;
        }

        public String getMessageId() {
            return MessageId;
        }

        public void setMessageId(String messageId) {
            MessageId = messageId;
        }
    }

    public boolean isOk() { return ErrorCode == 0; }
}