package com.om.backend.Config;

import com.om.backend.services.CustomUserDetails;
import com.om.backend.services.JWTService;
import com.om.backend.services.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;

    @Autowired
    ApplicationContext applicationContext;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String phonenumber = null;

        if (authHeader!=null && authHeader.startsWith("Bearer ")){
            token = authHeader.substring(7);
            phonenumber = jwtService.extractPhonenumber(token);
        }
        if (phonenumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            MyUserDetailsService userDetailsService = applicationContext.getBean(MyUserDetailsService.class);
            UserDetails userDetails = userDetailsService.loadUserByUsername(phonenumber);
            if (jwtService.validToken(token, userDetails)) {
                Long userId = ((CustomUserDetails) userDetails).getId();
                UsernamePasswordAuthenticationToken authentoken =
                        new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of()); // No roles for now
                authentoken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentoken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
