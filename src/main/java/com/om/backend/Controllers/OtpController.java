//package com.om.backend.Controllers;
//
//
//import com.netflix.discovery.converters.Auto;
//import com.om.backend.Config.OtpAuthenticationToken;
//import com.om.backend.Dto.SendSmsResponse;
//import com.om.backend.Dto.UserDTo;
//import com.om.backend.services.*;
//import com.om.backend.util.OtpMessageBuilder;
//import com.om.backend.util.PhoneNumberUtil;
//import com.om.backend.util.SmsClient;
//import jakarta.validation.constraints.NotBlank;
//import org.checkerframework.checker.units.qual.A;
//import org.checkerframework.checker.units.qual.Area;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Duration;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/auth")
//@CrossOrigin(origins = "${cors.allowed-origins}")
//public class OtpController {
//
//    @Autowired
//    private MyUserDetailsService userDetailsService;
//   @Autowired
//    private JWTService jwtService;
//   @Autowired
//    private  OtpService otpService;                     // send OTP via SMS
//   @Autowired
//    private  AuthenticationManager authenticationManager; // delegates to OtpAuthenticationProvider
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private MyUserDetailsService myUserDetailsService;
//
//    @Autowired
//    private UserSessionService userSessionService;
//
//    @Autowired
//    private RefreshTokenService refreshTokenService;
//
//    /** Step 1: request OTP (sends SMS) */
//    @PostMapping("/request")
//    public ResponseEntity<?> request(@RequestParam String phone) {
//        otpService.sendOtp(phone);
//        return ResponseEntity.ok().build();
//    }
//
//    /** Step 2: authenticate with phone+otp (Provider validates & loads/creates user) */
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestParam String phone,
//                                   @RequestParam String otp,
//                                   @RequestParam int registrationId,
//                                   @RequestParam(defaultValue = "1") int deviceId) {
//        // 1) Authenticate (delegates to OtpAuthenticationProvider → validates OTP, loads/creates user)
//
//        Authentication auth = authenticationManager.authenticate(OtpAuthenticationToken.unauthenticated(phone, otp));
//
//        var principal = (CustomUserDetails) auth.getPrincipal();
//        Long userId = principal.getUser().getId();
//
//        String sessionId = UUID.randomUUID().toString();
//        userSessionService.createOrUpdateSession(userId, sessionId, req.deviceModel, req.platform, req.appVersion);
//
//// 2) mint tokens (make sure access has sid=sessionId; refresh too if you like)
//        String accessJwt  = otpService.mintAccessToken(userId, sessionId);     // implement in OtpService
//        String refreshJwt = otpService.mintRefreshToken(userId, sessionId);    // implement in OtpService
//
//// 3) bind refresh token to this session (store hash + jti + exp in user_session)
//        userSessionService.bindRefreshToken(userId, sessionId, refreshJwt);
//
//// 4) respond
//        return ResponseEntity.ok(new TokenPair(accessJwt, refreshJwt, sessionId));
//
//
//
//        // 2) Issue short-lived access token
//        String accessToken = jwtService.generateToken(principal); // your existing method
//
//        // 3) Issue long-lived refresh token bound to device
//        var issued = refreshTokenService.issue(userId, registrationId, deviceId, Duration.ofDays(90));
//        String refreshToken = (String) issued.get("raw");
//
//        // (Optional) set cookie instead of body:
//        // ResponseCookie cookie = ResponseCookie.from("rt", refreshToken).httpOnly(true).secure(true)
//        //      .sameSite("Strict").path("/auth/refresh").maxAge(Duration.ofDays(90)).build();
//
//        return ResponseEntity.ok(Map.of(
//                "accessToken", accessToken,
//                "refreshToken", refreshToken,
//                "expiresAt", issued.get("expiresAt")
//        ));
//    }
//
//    @PostMapping("/refresh")
//    public ResponseEntity<?> refresh(@RequestParam String refreshToken,
//                                     @RequestParam int registrationId,
//                                     @RequestParam(defaultValue = "1") int deviceId) {
//        try {
//            // 1) verify & rotate the refresh token for this device
//            var rotated = refreshTokenService.verifyAndRotate(refreshToken, registrationId, deviceId, Duration.ofDays(90));
//            String newRefresh = (String) rotated.get("raw");
//
//            // 2) Issue a new access token for that user
//            // we need the user for access token; fetch via token hash lookup was done inside service.
//            // simple approach: store userId inside the refresh token payload (signed) OR
//            // return userId in the service; here's a pragmatic way:
//            // Add a method to RefreshTokenService to resolve userId by current hash if needed,
//            // or return it from verifyAndRotate alongside "raw".
//
//            // For brevity, let’s add return of userId from service (update Map in service):
//            // return Map.of("raw", raw, "expiresAt", ..., "id", e.getId(), "userId", userId);
//
//            Long userId = (Long) rotated.get("userId");
//            String username = userService.getUser(String.valueOf(userId)).getPhoneNumber(); // or a direct load of CustomUserDetails
//
//            var userDetails = (CustomUserDetails) myUserDetailsService.loadUserByUsername(username);
//            String newAccess = jwtService.generateToken(userDetails);
//
//            return ResponseEntity.ok(Map.of(
//                    "accessToken", newAccess,
//                    "refreshToken", newRefresh,
//                    "expiresAt", rotated.get("expiresAt")
//            ));
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
//        }
//    }
//
//    @PostMapping("/logout-device")
//    public ResponseEntity<?> logoutDevice(@RequestParam int registrationId,
//                                          @RequestParam(defaultValue = "1") int deviceId,
//                                          @AuthenticationPrincipal CustomUserDetails me) {
//        refreshTokenService.revokeAllForDevice(me.getUser().getId(), registrationId, deviceId);
//        return ResponseEntity.ok().build();
//    }
//
//    static class LoginRequest {
//        public String phone;
//        public String otp;
//        public String deviceModel;   // "Pixel 7"
//        public String platform;      // "android"
//        public String appVersion;    // "1.3.0"
//    }
//    static class TokenPair {
//        public String accessToken;
//        public String refreshToken;
//        public String sessionId;
//        public TokenPair() {}
//        public TokenPair(String at, String rt, String sid){ this.accessToken=at; this.refreshToken=rt; this.sessionId=sid; }
//    }
//    static class RefreshRequest { public String refreshToken; }
//
//}


package com.om.backend.Controllers;


import com.om.backend.services.OtpService;
import com.om.backend.services.UserSessionService;
import com.om.backend.util.JwtIntrospection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class OtpController {

    private final OtpService otpService;
    private final UserSessionService userSessionService;

    public OtpController(OtpService otpService, UserSessionService userSessionService) {
        this.otpService = otpService;
        this.userSessionService = userSessionService;
    }

    // -------- DTOs --------

    public static class SendOtpRequest {
        public String phone;
    }

    public static class VerifyOtpRequest {
        public String phone;
        public String otp;
        public String deviceModel;     // e.g., "Pixel 7"
        public String platform;        // e.g., "android"
        public String appVersion;      // e.g., "1.3.0"
        public String fcmToken;        // optional: register device's FCM immediately
    }

    public static class RefreshRequest {
        public String refreshToken;
    }

    public static class TokenPair {
        public String accessToken;
        public String refreshToken;
        public String sessionId;
        public TokenPair() {}
        public TokenPair(String at, String rt, String sid) {
            this.accessToken = at; this.refreshToken = rt; this.sessionId = sid;
        }
    }

    public static class LoginResponse {
        public Long   userId;
        public String sessionId;
        public String accessToken;
        public String refreshToken;
        public Instant issuedAt = Instant.now();
        public LoginResponse() {}
        public LoginResponse(Long uid, String sid, String at, String rt) {
            this.userId = uid; this.sessionId = sid; this.accessToken = at; this.refreshToken = rt;
        }
    }

    // -------- Endpoints --------

    /**
     * Request an OTP to be sent to the given phone number.
     */
    @PostMapping("/otp/send")
    public ResponseEntity<Void> sendOtp(@RequestBody SendOtpRequest req) {
        if (req == null || req.phone == null || req.phone.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        otpService.sendOtp(req.phone); // implement to generate & deliver OTP (SMS)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Verify OTP and log the user in:
     *  - create/update a session row
     *  - mint access/refresh tokens (with sid = sessionId)
     *  - bind the refresh token to the session (hash/jti/exp)
     *  - (optional) register device FCM token if provided
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody VerifyOtpRequest req) {
        if (req == null || isBlank(req.phone) || isBlank(req.otp)) {
            return ResponseEntity.badRequest().build();
        }

        // 1) Validate OTP and resolve userId (create user if you support just-in-time)
        Long userId = otpService.verifyOtp(req.phone, req.otp); // returns userId or throws

        // 2) Create a sessionId and upsert the session row
        String sessionId = UUID.randomUUID().toString();
        String platform = isBlank(req.platform) ? "android" : req.platform;
        userSessionService.createOrUpdateSession(userId, sessionId, req.deviceModel, platform, req.appVersion);

        // 3) Mint tokens (IMPORTANT: include sid=sessionId in the JWTs)
        String accessJwt  = otpService.mintAccessToken(userId, sessionId);
        String refreshJwt = otpService.mintRefreshToken(userId, sessionId);

        // 4) Bind refresh token to the session (store hash/jti/exp)
        userSessionService.bindRefreshToken(userId, sessionId, refreshJwt);

        // 5) (Optional) Register/update FCM token for this device session
        if (!isBlank(req.fcmToken)) {
            userSessionService.registerOrUpdateDevice(userId, sessionId, req.fcmToken, req.deviceModel, req.appVersion, platform);
        }

        return ResponseEntity.ok(new LoginResponse(userId, sessionId, accessJwt, refreshJwt));
    }

    /**
     * Rotate the refresh token and issue a new access token.
     * Expects a valid refresh token (with sid claim) that matches the session's stored hash.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshRequest body) {
        if (body == null || isBlank(body.refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        // Extract essentials from old refresh (no need to re-verify here; service validates by hash/jti/exp)
        Long   userId    = Long.valueOf(JwtIntrospection.extractSub(body.refreshToken).orElseThrow(() -> new IllegalArgumentException("sub missing")));
        String sessionId = JwtIntrospection.extractSid(body.refreshToken).orElseThrow(() -> new IllegalArgumentException("sid missing"));

        // Mint new pair and rotate stored refresh
        String newAccess  = otpService.mintAccessToken(userId, sessionId);
        String newRefresh = otpService.mintRefreshToken(userId, sessionId);
        userSessionService.rotateRefreshToken(userId, sessionId, body.refreshToken, newRefresh);

        return ResponseEntity.ok(new TokenPair(newAccess, newRefresh, sessionId));
    }

    // -------- helpers --------
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
