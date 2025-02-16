package com.sallejoven.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/info")
    public ResponseEntity<String> getPublicInfo() {
        return ResponseEntity.ok("This is a public endpoint accessible by anyone.");
    }
}