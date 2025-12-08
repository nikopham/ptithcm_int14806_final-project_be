package com.ptithcm.movie.external.cloudflare;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/cloudflare")
    public ResponseEntity<String> handleCloudflare(
            @RequestHeader(value = "Webhook-Signature") String signature,
            @RequestBody String body
    ) {
        try {
            webhookService.handleCloudflareWebhook(signature, body);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error");
        }
    }
}