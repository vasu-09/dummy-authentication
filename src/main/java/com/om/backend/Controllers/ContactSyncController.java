package com.om.backend.Controllers;

import com.om.backend.Dto.ContactMatchDto;
import com.om.backend.Dto.ContactSyncRequest;
import com.om.backend.services.ContactSyncService;
import com.om.backend.services.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class ContactSyncController {

    @Autowired
    private  ContactSyncService contactSyncService;
    @Autowired
    private  JWTService jwtService;

    @PostMapping("/sync")
    public ResponseEntity<List<ContactMatchDto>> sync(@AuthenticationPrincipal Object principal,
                                                      @RequestBody ContactSyncRequest req) {
        String phoneOfRequester = jwtService.resolveUsernameFromPrincipal(principal); // you added this helper earlier
        return ResponseEntity.ok(contactSyncService.match(req.getPhones()));
    }
}
