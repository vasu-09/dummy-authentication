//package com.om.backend.services;
//
//import com.om.backend.Config.SmsProperties;
//import com.om.backend.util.OtpMessageBuilder;
//import com.om.backend.util.PhoneNumberUtil;
//import com.om.backend.util.SmsClient;
//import lombok.RequiredArgsConstructor;
//
//import org.apache.commons.lang.RandomStringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//
//@Service
//@RequiredArgsConstructor
//public class OtpService {
//    @Autowired
//    private  StringRedisTemplate redis;
//    private  SmsProperties props;
//    private  SmsClient smsClient;
//    private  OtpMessageBuilder messageBuilder;
//
//    /** Step 1: generate & send OTP via SMS */
//    public void sendOtp(String rawPhone) {
//        String phone = PhoneNumberUtil.toE164India(rawPhone);
//        enforceRateLimits(phone);
//
//        String otp = RandomStringUtils.randomNumeric(props.getOtp().getDigits());
//        redis.opsForValue().set(otpKey(phone), otp, Duration.ofMinutes(props.getOtp().getTtlMinutes()));
//
//        String msg = messageBuilder.build(otp);
//        var res = smsClient.sendOtpMessage(msg, phone, true);
//        if (!res.isOk()) {
//            throw new RuntimeException("SMS send failed: " + res.getErrorDescription() + " (code " + res.getErrorCode() + ")");
//        }
//    }
//
//    /** Step 2: validate during AuthenticationProvider */
//    public boolean validateOtp(String rawPhone, String providedOtp) {
//        String phone = PhoneNumberUtil.toE164India(rawPhone);
//        String key = otpKey(phone);
//        String expected = redis.opsForValue().get(key);
//        if (expected != null && expected.equals(providedOtp)) {
//            redis.delete(key); // one-time use
//            return true;
//        }
//        return false;
//    }
//
//    private String otpKey(String e164Phone) { return "otp:" + e164Phone; }
//
//    private void enforceRateLimits(String e164Mobile) {
//        // per-minute
//        String pmKey = "otp:rl:1m:" + e164Mobile;
//        Long pm = redis.opsForValue().increment(pmKey);
//        if (pm != null && pm == 1) redis.expire(pmKey, Duration.ofMinutes(1));
//        if (pm != null && pm > props.getOtp().getPerMinuteLimit()) {
//            throw new RateLimitException("Too many OTP requests. Try again in a minute.");
//        }
//        // per-hour
//        String phKey = "otp:rl:1h:" + e164Mobile;
//        Long ph = redis.opsForValue().increment(phKey);
//        if (ph != null && ph == 1) redis.expire(phKey, Duration.ofHours(1));
//        if (ph != null && ph > props.getOtp().getPerHourLimit()) {
//            throw new RateLimitException("Too many OTP requests. Try again later.");
//        }
//    }
//
//    public static class RateLimitException extends RuntimeException {
//        public RateLimitException(String msg) { super(msg); }
//    }
//
//
//}



/**
 * Handles OTP send/verify and mints JWTs by delegating to a JwtSigner bean.
 * Make sure your JwtSigner sets the 'sid' claim to the sessionId you pass in.
 */
package com.om.backend.services;

import com.om.backend.Config.SmsProperties;
import com.om.backend.Dto.SendSmsResponse;
import com.om.backend.Model.User;
import com.om.backend.Model.Otp;

import com.om.backend.Repositories.OtpRepository;
import com.om.backend.Repositories.UserRepository;
import com.om.backend.util.OtpMessageBuilder;
import com.om.backend.util.PhoneNumberUtil;          // your util with toE164India(...)


import com.om.backend.util.SmsClient;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OTP service backed by Redis for OTP storage & rate limiting, and your SMS provider for delivery.
 * Persists users in DB; (optional) can persist OTP rows for audit.
 */
@Service
public class OtpService {

    // ===== Injected deps (from your old class) =====
    private final StringRedisTemplate redis;
    private final SmsProperties props;
    private final SmsClient smsClient;
    private final OtpMessageBuilder messageBuilder;

    // ===== Existing deps from the "new" design =====
    private final OtpRepository otpRepo;      // optional (audit); keep if you want
    private final UserRepository userRepo;
    private final JwtSigner jwtSigner;
    private final Clock clock;

    public OtpService(
            StringRedisTemplate redis,
            SmsProperties props,
            SmsClient smsClient,
            OtpMessageBuilder messageBuilder,
            OtpRepository otpRepo,
            UserRepository userRepo,
            JwtSigner jwtSigner,
            Clock clock
    ) {
        this.redis = redis;
        this.props = props;
        this.smsClient = smsClient;
        this.messageBuilder = messageBuilder;
        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
        this.jwtSigner = jwtSigner;
        this.clock = clock;
    }

    // ===================== SEND OTP =====================

    /** Step 1: generate & send OTP via SMS (Redis + rate limits). */
    @Transactional(noRollbackFor = RuntimeException.class)
    public void sendOtp(String rawPhone) {
        String phone = PhoneNumberUtil.toE164India(rawPhone);

        // 1) rate-limit (Redis counters)
        enforceRateLimits(phone);

        // 2) generate OTP using your config
        int digits = Math.max(4, props.getOtp().getDigits());
        String otp = RandomStringUtils.randomNumeric(digits);

        // 3) store OTP in Redis with TTL (overwrite any existing)
        Duration ttl = Duration.ofMinutes(props.getOtp().getTtlMinutes());
        redis.opsForValue().set(otpKey(phone), otp, ttl);

        // 4) (optional) also persist to DB for audit/troubleshooting (comment out if not needed)
        if (props.getOtp().isPersistForAudit()) {
            Otp row = otpRepo.findByPhone(phone).orElseGet(Otp::new);
            row.setPhoneNumber(phone);
            row.setOtpCode(otp);
            row.setExpiredAt(Instant.now(clock).plus(ttl));
            otpRepo.save(row);
        }

        // Send via your SMS provider
        String msg = messageBuilder.build(otp);
        SendSmsResponse res = smsClient.sendOtpMessage(msg, phone, true);
        if (!isOk(res)) {
            String desc = (res != null && res.getErrorDescription() != null) ? res.getErrorDescription() : "unknown";
            Integer code = (res != null) ? res.getErrorCode() : null;
            throw new RuntimeException("SMS send failed: " + desc + (code != null ? " (code " + code + ")" : ""));
        }

    }

    private boolean isOk(SendSmsResponse r) {
        if (r == null) return false;
        Integer code = r.getErrorCode();
        return (code == null || code == 0);
    }

    // ===================== VERIFY OTP =====================

    /**
     * Step 2: verify OTP from Redis; delete on success; return userId (create user if needed).
     * This replaces the previous DB-only verification.
     */
    @Transactional
    public Long verifyOtp(String rawPhone, String otpCode) {
        String phone = PhoneNumberUtil.toE164India(rawPhone);

        // 1) fetch OTP from Redis
        String key = otpKey(phone);
        String expected = redis.opsForValue().get(key);
        if (expected == null) {
            // (Optional) backward-compat: if Redis missing, check DB audit row once
            Optional<Otp> audit = otpRepo.findByPhone(phone);
            if (audit.isEmpty() || Instant.now(clock).isAfter(audit.get().getExpiredAt())) {
                throw new IllegalArgumentException("OTP expired or not requested");
            }
            if (!otpCode.equals(audit.get().getOtpCode())) {
                throw new IllegalArgumentException("Invalid OTP");
            }
            // clear audit row if you want true single-use semantics
            otpRepo.delete(audit.get());
        } else {
            // Redis path (preferred)
            if (!otpCode.equals(expected)) {
                throw new IllegalArgumentException("Invalid OTP");
            }
            // delete OTP (single-use)
            redis.delete(key);
        }

        // 2) resolve/create user
        return userRepo.findByPhone(phone)
                .map(User::getId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setPhoneNumber(phone);
                    u.setCreatedAt(Instant.now(clock));
                    u.setUpdatedAt(Instant.now(clock));
                    userRepo.save(u);
                    return u.getId();
                });
    }

    // ===================== TOKEN MINTING =====================

    /** Mint a short-lived access token (must include 'sid' = sessionId). */
    public String mintAccessToken(Long userId, String sessionId) {
        return jwtSigner.signAccessToken(userId, sessionId);
    }

    /** Mint a long-lived refresh token (recommended to include 'sid' = sessionId). */
    public String mintRefreshToken(Long userId, String sessionId) {
        return jwtSigner.signRefreshToken(userId, sessionId);
    }

    // ===================== HELPERS =====================

    private String otpKey(String phone) {
        // dedicated namespace in Redis
        return "otp:" + phone;
    }

    private String rlMinuteKey(String phone) {
        return "otp:rl:minute:" + phone;
    }

    private String rlDailyKey(String phone) {
        return "otp:rl:day:" + phone;
    }

    /**
     * Basic dual-window rate limiting:
     *  - per-minute: e.g., 1–3 sends
     *  - per-day:    e.g., 5–10 sends
     * These thresholds come from SmsProperties.
     */
    private void enforceRateLimits(String phone) {
        int perMinute = props.getOtp().getPerMinuteLimit(); // e.g., 2
        int perDay    = props.getOtp().getPerHourLimit();    // e.g., 10

        // minute window (60s)
        Long minuteCount = redis.opsForValue().increment(rlMinuteKey(phone));
        if (minuteCount != null && minuteCount == 1L) {
            redis.expire(rlMinuteKey(phone), 60, TimeUnit.SECONDS);
        }
        if (minuteCount != null && minuteCount > perMinute) {
            throw new IllegalStateException("Too many OTP requests. Try again later.");
        }

        // daily window (24h)
        Long dayCount = redis.opsForValue().increment(rlDailyKey(phone));
        if (dayCount != null && dayCount == 1L) {
            redis.expire(rlDailyKey(phone), 24, TimeUnit.HOURS);
        }
        if (dayCount != null && dayCount > perDay) {
            throw new IllegalStateException("Daily OTP limit exceeded.");
        }
    }

    /** Minimal signer abstraction expected to be provided elsewhere in your app. */
    public interface JwtSigner {
        String signAccessToken(Long userId, String sessionId);
        String signRefreshToken(Long userId, String sessionId);
    }
}

