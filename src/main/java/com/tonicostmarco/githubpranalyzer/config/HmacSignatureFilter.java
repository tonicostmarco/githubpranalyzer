package com.tonicostmarco.githubpranalyzer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacSignatureFilter extends OncePerRequestFilter {

    @Value("${github.webhook.secret}")
    private String secret;

    @Value("${webhook.notify}")
    private String uri;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI().equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader("X-Hub-Signature-256");

        if (signature == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Empty signature");
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();

        try {
            if (!isValidSignature(signature, body)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        HttpServletRequestWrapper replayable = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                ByteArrayInputStream bis = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    public int read() {
                        return bis.read();
                    }

                    public boolean isFinished() {
                        return bis.available() == 0;
                    }

                    public boolean isReady() {
                        return true;
                    }

                    public void setReadListener(ReadListener l) {
                    }
                };
            }

            @Override
            public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream()));
            }
        };

        filterChain.doFilter(replayable, response);
    }

    private boolean isValidSignature(String signature, byte[] body) throws InvalidKeyException, NoSuchAlgorithmException {

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        mac.init(keySpec);

        byte[] hash = mac.doFinal(body);
        String hex = "sha256=" + HexFormat.of().formatHex(hash);
        return MessageDigest.isEqual(hex.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));

    }


}