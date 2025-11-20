package com.shanthigear.repository;

import com.shanthigear.entity.EmailDomainConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailDomainConfigRepository extends JpaRepository<EmailDomainConfig, Long> {
    Optional<EmailDomainConfig> findByDomain(String domain);
    boolean existsByDomain(String domain);
}
