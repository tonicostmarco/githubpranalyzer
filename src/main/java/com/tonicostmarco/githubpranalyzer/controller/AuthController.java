package com.tonicostmarco.githubpranalyzer.controller;

import com.tonicostmarco.githubpranalyzer.config.SecurityConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth/token")
public class AuthController {



    @PostMapping
    public ResponseEntity<Void> authorization(@RequestHeader("email") String email, @RequestHeader("password") String password) {


    }

}
