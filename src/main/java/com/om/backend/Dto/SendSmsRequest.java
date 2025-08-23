package com.om.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Adjust field names to EXACTLY what the API expects (case-sensitive if required). */
@Data
public class SendSmsRequest {
    private String ApiKey;
    private String ClientId;
    private String SenderId;
    private String Message;
    private String MobileNumber;

    // DLT
    private String TemplateId;

    // Optional flags, keep nullable
    private Boolean Is_Unicode;
    private Boolean Is_Flash;
    private String IsRegisteredForDelivery; // "true" to request DLR callbacks
    private String ValidityPeriod;
    private String DataCoding;
    private String scheduleTime;
    private String groupId;

    public SendSmsRequest() {
    }

    public SendSmsRequest(String apiKey, String clientId, String senderId, String message, String mobileNumber, String templateId, Boolean is_Unicode, Boolean is_Flash, String isRegisteredForDelivery, String validityPeriod, String dataCoding, String scheduleTime, String groupId) {
        ApiKey = apiKey;
        ClientId = clientId;
        SenderId = senderId;
        Message = message;
        MobileNumber = mobileNumber;
        TemplateId = templateId;
        Is_Unicode = is_Unicode;
        Is_Flash = is_Flash;
        IsRegisteredForDelivery = isRegisteredForDelivery;
        ValidityPeriod = validityPeriod;
        DataCoding = dataCoding;
        this.scheduleTime = scheduleTime;
        this.groupId = groupId;
    }

    public String getApiKey() {
        return ApiKey;
    }

    public void setApiKey(String apiKey) {
        ApiKey = apiKey;
    }

    public String getClientId() {
        return ClientId;
    }

    public void setClientId(String clientId) {
        ClientId = clientId;
    }

    public String getSenderId() {
        return SenderId;
    }

    public void setSenderId(String senderId) {
        SenderId = senderId;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public String getMobileNumber() {
        return MobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        MobileNumber = mobileNumber;
    }

    public String getTemplateId() {
        return TemplateId;
    }

    public void setTemplateId(String templateId) {
        TemplateId = templateId;
    }

    public Boolean getIs_Unicode() {
        return Is_Unicode;
    }

    public void setIs_Unicode(Boolean is_Unicode) {
        Is_Unicode = is_Unicode;
    }

    public Boolean getIs_Flash() {
        return Is_Flash;
    }

    public void setIs_Flash(Boolean is_Flash) {
        Is_Flash = is_Flash;
    }

    public String getIsRegisteredForDelivery() {
        return IsRegisteredForDelivery;
    }

    public void setIsRegisteredForDelivery(String isRegisteredForDelivery) {
        IsRegisteredForDelivery = isRegisteredForDelivery;
    }

    public String getValidityPeriod() {
        return ValidityPeriod;
    }

    public void setValidityPeriod(String validityPeriod) {
        ValidityPeriod = validityPeriod;
    }

    public String getDataCoding() {
        return DataCoding;
    }

    public void setDataCoding(String dataCoding) {
        DataCoding = dataCoding;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
