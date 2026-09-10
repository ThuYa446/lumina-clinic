package com.lumina.clinic.web;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping("/api/csrf")
    Map<String, String> csrf(CsrfToken token) {
        // Resolving the deferred token writes the cookie Angular's XSRF interceptor reads.
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
