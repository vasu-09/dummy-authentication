package com.om.backend.Controllers;

import com.om.backend.Dto.OneTimePreKeyDto;
import com.om.backend.Dto.PreKeyBundleDto;
import com.om.backend.services.JWTService;
import com.om.backend.services.PreKeyBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/keys")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class KeyRegistrationController {
    @Autowired
    private  JWTService jwtService;
    @Autowired
    private  PreKeyBundleService bundleService;

    /** Device uploads identity, signed prekey (+sig) and a batch of one-time prekeys (see note below) */
    @PostMapping("/register")
    public ResponseEntity<?> registerBundle(@AuthenticationPrincipal Object principal,
                                            @RequestBody PreKeyBundleDto dto) {
        // resolve user from principal (CustomUserDetails or username->user id)
        String phone = jwtService.resolveUsernameFromPrincipal(principal); // implement this helper to pull username
        bundleService.saveOrUpdateBundleForPhone(phone, dto);
        return ResponseEntity.ok().build();
    }

    /** Fetch a recipient’s bundle (consumes one-time prekey atomically) */

    @PostMapping("/one-time/upload")
    public ResponseEntity<?> uploadBatch(@AuthenticationPrincipal Object principal,
                                         @RequestParam(defaultValue = "1") int deviceId,
                                         @RequestBody List<OneTimePreKeyDto> batch) {
        String phone = jwtService.resolveUsernameFromPrincipal(principal);
        bundleService.uploadOneTimePreKeys(phone, deviceId, batch);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{phone}/bundle")
    public ResponseEntity<?> fetchBundleFor(@PathVariable String phone) {
        return ResponseEntity.ok(bundleService.consumeOneTimeBundleForPhone(phone));
    }

    public static class JwksController {
    }
}
