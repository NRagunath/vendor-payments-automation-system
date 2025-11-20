package com.shanthigear.controller;

import com.shanthigear.entity.EmailDomainConfig;
import com.shanthigear.service.EmailDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-domains")
@RequiredArgsConstructor
public class EmailDomainController {
    
    private final EmailDomainService emailDomainService;
    
    @GetMapping
    public List<EmailDomainConfig> getAll() {
        return emailDomainService.getAllConfigs();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EmailDomainConfig> getById(@PathVariable Long id) {
        return emailDomainService.getConfig(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/domain/{domain}")
    public ResponseEntity<EmailDomainConfig> getByDomain(@PathVariable String domain) {
        return emailDomainService.getConfigByDomain(domain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public EmailDomainConfig create(@RequestBody EmailDomainConfig config) {
        return emailDomainService.saveConfig(config);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EmailDomainConfig> update(
            @PathVariable Long id, 
            @RequestBody EmailDomainConfig config) {
        
        return emailDomainService.getConfig(id)
            .map(existing -> {
                config.setId(id);
                return ResponseEntity.ok(emailDomainService.saveConfig(config));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (emailDomainService.getConfig(id).isPresent()) {
            emailDomainService.deleteConfig(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
