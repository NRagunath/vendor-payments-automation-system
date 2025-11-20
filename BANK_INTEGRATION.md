# HDFC Bank Integration Guide

This document provides comprehensive guidance for integrating with HDFC Bank's payment processing API in the Vendor Payment Notifier application.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
  - [Environment Variables](#1-environment-variables)
  - [Application Properties](#2-application-properties)
- [API Endpoints](#api-endpoints)
- [Implementation Details](#implementation-details)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Security Best Practices](#security-best-practices)
- [Support](#support)

## Prerequisites

1. **HDFC Bank API Access**
   - Sandbox API credentials (for testing)
   - Production API credentials (for live environment)
   - Approved IP addresses whitelisted with HDFC Bank

2. **Development Environment**
   - Java 21 or later
   - Maven 3.9.0 or later
   - Access to HDFC Bank's sandbox environment
   - Spring Boot 3.1.0 or later

## Configuration

### 1. Environment Variables

Create a `.env` file in your project root (never commit this file):

```env
# ========================================
# HDFC BANK API CONFIGURATION
# ========================================
# Sandbox Environment (for testing)
BANK_API_URL=https://api.hdfcbank.com/sandbox/v1
BANK_API_KEY=your_sandbox_api_key_here
BANK_CLIENT_ID=your_sandbox_client_id_here

# Production Environment (uncomment when ready)
# BANK_API_URL=https://api.hdfcbank.com/production/v1
# BANK_API_KEY=your_production_api_key_here
# BANK_CLIENT_ID=your_production_client_id_here

# API Settings
BANK_API_TIMEOUT=10000
BANK_API_MAX_RETRIES=3
BANK_API_CONNECT_TIMEOUT=5000
BANK_API_READ_TIMEOUT=10000

# ========================================
# APPLICATION SETTINGS
# ========================================
# Set active profile (dev, test, prod)
SPRING_PROFILES_ACTIVE=dev

# Database Configuration
ORACLE_USER=your_db_username
ORACLE_PASSWORD=your_db_password

# Email Configuration
SMTP_USERNAME=your_email@example.com
SMTP_PASSWORD=your_email_password
```

### 2. Application Properties

The application will automatically use the environment variables. You can also override them in:

- `application.properties` - Default settings
- `application-dev.properties` - Development environment overrides
- `application-test.properties` - Test environment overrides
- `application-prod.properties` - Production environment overrides

Example `application-dev.properties`:
```properties
# HDFC Bank API Configuration
bank.api.base-url=${BANK_API_URL}
bank.api.key=${BANK_API_KEY}
bank.api.client-id=${BANK_CLIENT_ID}
bank.api.timeout=${BANK_API_TIMEOUT:10000}
bank.api.connect-timeout=${BANK_API_CONNECT_TIMEOUT:5000}
bank.api.read-timeout=${BANK_API_READ_TIMEOUT:10000}
bank.api.max-retries=${BANK_API_MAX_RETRIES:3}
bank.api.ssl-validation=true

# Connection Pool Settings
bank.service.max-pool-size=10
bank.service.queue-capacity=500

# Enable debug logging for bank integration
logging.level.com.shanthigear.bank.hdfc=DEBUG
```

## API Endpoints

### Process Payment
- **Endpoint**: `POST /api/payments/process`
- **Authentication**: API Key in `X-API-Key` header
- **Request Body**: JSON payment request
- **Response**: Payment reference number

### Check Payment Status
- **Endpoint**: `GET /api/payments/{referenceNumber}`
- **Authentication**: API Key in `X-API-Key` header
- **Response**: Payment status details

## Implementation Details

### Key Components

1. **BankApiClient**
   - Handles low-level HTTP communication with HDFC Bank's API
   - Manages authentication headers and API keys
   - Processes API responses and error handling

2. **HdfcBankService**
   - Implements business logic for payment processing
   - Handles error cases and implements retry logic
   - Maps between application DTOs and HDFC API formats

3. **BankServiceConfig**
   - Configures connection pools and timeouts
   - Manages API credentials and environment-specific settings
   - Sets up SSL/TLS configuration

### Error Handling

The system handles various error scenarios:

- `BankApiException`: For bank API communication errors
- `PaymentProcessingException`: For payment processing failures
- `InvalidPaymentException`: For invalid payment requests
- `BankAuthenticationException`: For authentication/authorization failures

## Testing

### Unit Tests

Run the test suite with:

```bash
mvn test
```

### Integration Testing

1. Set up test properties in `src/test/resources/application-test.properties`
2. Run integration tests with:

```bash
mvn test -Dspring.profiles.active=test
```

### Manual Testing
- Run with `dev` profile
- Verify logs for API calls
- Test error scenarios and edge cases
- Validate response handling and error messages

## Troubleshooting

### Common Issues

1. **Connection Timeouts**
   - Verify network connectivity to HDFC API endpoints
   - Check firewall settings and IP whitelisting
   - Review and adjust timeout settings if needed
   - Check if the bank's API is operational

2. **Authentication Failures**
   - Verify API key and client ID are correct and not expired
   - Check for proper header formatting (`X-API-Key`)
   - Ensure the API key has required permissions
   - Verify token expiration and refresh logic

3. **Payment Processing Errors**
   - Validate payment request payload
   - Check account status and limits
   - Verify sufficient funds
   - Review error responses from the bank API

4. **SSL/TLS Issues**
   - Ensure Java truststore is updated with HDFC's certificates
   - Verify TLS version compatibility (TLS 1.2 or higher required)
   - Check certificate chain and validity

## Security Best Practices

1. **Credential Management**
   - Never commit sensitive data to version control
   - Keep `.env` in `.gitignore`
   - Use environment variables in CI/CD
   - Rotate API keys regularly (every 90 days recommended)
   - Use different credentials for development, testing, and production

2. **Network Security**
   - Use HTTPS for all API calls
   - Enable IP whitelisting with HDFC Bank
   - Monitor API usage and set up alerts for suspicious activity
   - Implement rate limiting to prevent abuse

3. **Application Security**
   - Validate all input data
   - Implement proper error handling without exposing sensitive information
   - Keep dependencies updated
   - Follow secure coding practices

4. **Data Protection**
   - Encrypt sensitive data at rest
   - Implement proper logging without exposing sensitive information
   - Follow the principle of least privilege for database access

## Support

### HDFC Bank Support
- **Developer Support**: [developer@hdfcbank.com](mailto:developer@hdfcbank.com)
- **API Documentation**: [HDFC Developer Portal](https://developer.hdfcbank.com)
- **Emergency Support**: +1-555-0100 (24/7 for production issues)

### Application Support
For issues with the Vendor Payment Notifier application, please contact your system administrator or open an issue in the project's issue tracker.

### Incident Response
For security incidents, please contact:
- **Security Team**: security@yourcompany.com
- **24/7 Incident Response**: +1-555-0200
