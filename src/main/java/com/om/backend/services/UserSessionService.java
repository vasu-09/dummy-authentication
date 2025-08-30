package com.om.backend.services;

import com.om.backend.Dto.SessionDto;
import com.om.backend.Dto.SessionMappers;
import com.om.backend.Model.UserSession;
import com.om.backend.Repositories.UserSessionRepository;

import com.om.backend.util.JwtIntrospection;
import com.om.backend.util.Hashes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSessionService {

    private final UserSessionRepository sessionRepo;

    public UserSessionService(UserSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    // -------- creators used by login --------
     @Transactional
    public void createUserSession(Long userId, String sessionId, String deviceModel, String platform, String appVersion) {
        UserSession s = new UserSession();
        s.setId(sessionId);
        s.setUserId(userId);
        s.setDeviceModel(deviceModel);
        s.setPlatform(platform);
        s.setAppVersion(appVersion);
        s.setCreatedAt(Instant.now());
        s.setLastSeenAt(Instant.now());
        sessionRepo.save(s);
    }

     @Transactional
    public void createOrUpdateSession(Long userId, String sessionId, String deviceModel, String platform, String appVersion) {
        UserSession s = sessionRepo.findById(sessionId).orElseGet(UserSession::new);
        s.setId(sessionId);
        s.setUserId(userId);
        if (StringUtils.hasText(deviceModel)) s.setDeviceModel(deviceModel);
        if (StringUtils.hasText(platform))    s.setPlatform(platform);
        if (StringUtils.hasText(appVersion))  s.setAppVersion(appVersion);
        if (s.getCreatedAt() == null) s.setCreatedAt(Instant.now());
        s.setLastSeenAt(Instant.now());
        sessionRepo.save(s);
    }

    // -------- device / FCM --------
     @Transactional
    public void registerOrUpdateDevice(Long userId, String sessionId, String fcmToken, String deviceModel, String appVersion, String platform) {
        UserSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (!s.getUserId().equals(userId)) throw new IllegalArgumentException("Not your session");
        s.setFcmToken(fcmToken);
        if (StringUtils.hasText(deviceModel)) s.setDeviceModel(deviceModel);
        if (StringUtils.hasText(appVersion))  s.setAppVersion(appVersion);
        if (StringUtils.hasText(platform))    s.setPlatform(platform);
        s.setLastSeenAt(Instant.now());
        sessionRepo.save(s);
    }

    // -------- refresh token lifecycle (store hash/jti/exp on session) --------
     @Transactional
    public void bindRefreshToken(Long userId, String sessionId, String refreshJwtRaw) {
        UserSession s = sessionRepo.findById(sessionId).orElseThrow();
        if (!s.getUserId().equals(userId)) throw new IllegalArgumentException("Not your session");
        s.setRefreshTokenHash(Hashes.sha256(refreshJwtRaw));
        s.setRefreshJti(JwtIntrospection.extractJti(refreshJwtRaw).orElse(null));
        var exp = JwtIntrospection.extractExp(refreshJwtRaw).orElse(null); // seconds since epoch
        s.setRefreshExpiresAt(exp == null ? null : Instant.ofEpochSecond(exp));
        s.setLastSeenAt(Instant.now());
        sessionRepo.save(s);
    }

     @Transactional
    public void rotateRefreshToken(Long userId, String sessionId, String oldRefreshJwtRaw, String newRefreshJwtRaw) {
        UserSession s = sessionRepo.findById(sessionId).orElseThrow();
        if (!s.getUserId().equals(userId)) throw new IllegalArgumentException("Not your session");
        byte[] expected = s.getRefreshTokenHash();
         if (expected == null || !java.util.Arrays.equals(expected, Hashes.sha256(oldRefreshJwtRaw))) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        s.setRefreshTokenHash(Hashes.sha256(newRefreshJwtRaw));
        s.setRefreshJti(JwtIntrospection.extractJti(newRefreshJwtRaw).orElse(null));
        var exp = JwtIntrospection.extractExp(newRefreshJwtRaw).orElse(null);
        s.setRefreshExpiresAt(exp == null ? null : Instant.ofEpochSecond(exp));
        s.setRefreshRotatedAt(Instant.now());
        s.setLastSeenAt(Instant.now());
        sessionRepo.save(s);
    }

     @Transactional
    public void clearRefreshToken(Long userId, String sessionId) {
        UserSession s = sessionRepo.findById(sessionId).orElseThrow();
        if (!s.getUserId().equals(userId)) throw new IllegalArgumentException("Not your session");
        s.setRefreshTokenHash(null);
        s.setRefreshJti(null);
        s.setRefreshExpiresAt(null);
        s.setRefreshRotatedAt(Instant.now());
        sessionRepo.save(s);
    }

    // -------- list & revoke --------
     @Transactional
    public List<SessionDto> listSessions(Long userId, String currentSessionIdOrNull) {
        return sessionRepo.findAllByUserIdOrderByLastSeenAtDesc(userId).stream()
                .map(s -> SessionMappers.toDto(s, currentSessionIdOrNull))
                .collect(Collectors.toList());
    }

     @Transactional
    public void revokeSession(Long userId, String sessionId) {
        UserSession s = sessionRepo.findById(sessionId).orElseThrow();
        if (!s.getUserId().equals(userId)) throw new IllegalArgumentException("Not your session");
        if (s.getRevokedAt() == null) {
            s.setRevokedAt(Instant.now());
            s.setFcmToken(null);
            s.setRefreshTokenHash(null);
            s.setRefreshJti(null);
            s.setRefreshExpiresAt(null);
            s.setRefreshRotatedAt(Instant.now());
            sessionRepo.save(s);
        }
    }

     @Transactional
    public void revokeCurrentSession(Long userId, String bearerTokenJtiOrRaw) {
        String sid = JwtIntrospection.extractSid(bearerTokenJtiOrRaw)
                .orElseThrow(() -> new IllegalArgumentException("Token missing sid"));
        revokeSession(userId, sid);
    }


}
