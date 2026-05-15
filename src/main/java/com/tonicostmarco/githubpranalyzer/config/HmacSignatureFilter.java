package com.tonicostmarco.githubpranalyzer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacSignatureFilter extends OncePerRequestFilter {

    @Value("${github.webhook.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().equals("/webhook/notify")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 102400);

        String signature = wrappedRequest.getHeader("X-Hub-Signature-256");

        byte[] body = wrappedRequest.getContentAsByteArray();

        try {
            if (!isValidSignature(signature, body)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isValidSignature(String signature, byte[] body) throws InvalidKeyException, NoSuchAlgorithmException {

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        mac.init(keySpec);

        byte[] hash = mac.doFinal(body);
        String hex = "sha256=" + HexFormat.of().formatHex(hash);
        return hex.equals(signature);

    }


}