package com.shanthigear.service;

import com.shanthigear.entity.EmailDomainConfig;
import com.shanthigear.repository.EmailDomainConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailDomainService {
    
    private final EmailDomainConfigRepository repository;
    
    @Transactional
    public EmailDomainConfig saveConfig(EmailDomainConfig config) {
        return repository.save(config);
    }
    
    @Transactional
    public void deleteConfig(Long id) {
        repository.deleteById(id);
    }
    
    public Optional<EmailDomainConfig> getConfig(Long id) {
        return repository.findById(id);
    }
    
    public Optional<EmailDomainConfig> getConfigByDomain(String domain) {
        return repository.findByDomain(domain);
    }
    
    public List<EmailDomainConfig> getAllConfigs() {
        return repository.findAll();
    }
    
    public boolean domainExists(String domain) {
        return repository.existsByDomain(domain);
    }
}
