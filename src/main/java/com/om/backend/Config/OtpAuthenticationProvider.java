package com.om.backend.Config;



import com.om.backend.Model.User;
import com.om.backend.services.CustomUserDetails;
import com.om.backend.services.MyUserDetailsService;
import com.om.backend.services.OtpService;
import com.om.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/** Delegates OTP validation to OtpService; loads/creates user via UserService. */
@Component
@RequiredArgsConstructor
public class OtpAuthenticationProvider implements AuthenticationProvider {

    private  OtpService otpService;
    private  UserService userService;
    private  MyUserDetailsService myUserDetailsService; // <-- add this

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof OtpAuthenticationToken token)) return null;

        String phone = (String) token.getPrincipal();
        String otp   = (String) token.getCredentials();

        if (!otpService.validateOtp(phone, otp)) {
            throw new BadOtpException("Invalid or expired OTP");
        }

        // Load or create domain user
        User domainUser;
        try {
            domainUser = userService.getUser(phone); // returns com.om.backend.Model.User
        } catch (UsernameNotFoundException e) {
            // create a minimal user if not present
            domainUser = userService.createUserWithPhone(phone); // see method below
        }

        // Either wrap directly:
        // CustomUserDetails user = new CustomUserDetails(domainUser);

        // Or, preferred: go through your UserDetailsService (ensures authorities/flags are consistent)
        CustomUserDetails user = (CustomUserDetails) myUserDetailsService.loadUserByUsername(domainUser.getPhoneNumber());

        var result = OtpAuthenticationToken.authenticated(user, user.getAuthorities());
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OtpAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public static class BadOtpException extends org.springframework.security.core.AuthenticationException {
        public BadOtpException(String msg) { super(msg); }
    }


}