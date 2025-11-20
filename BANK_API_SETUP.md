# HDFC Bank API Configuration for Windows

This document provides Windows-specific instructions for configuring the HDFC Bank API integration in the Vendor Payment Notifier application.

## Required Environment Variables

### Authentication
- `BANK_API_URL`: Base URL of the HDFC Bank API (e.g., `https://api.hdfcbank.com`)
- `BANK_CLIENT_ID`: Your bank-issued client ID for API access
- `BANK_CLIENT_SECRET`: Your bank-issued client secret for API access
- `BANK_API_KEY`: API key provided by the bank (if different from client ID)

### Webhook Configuration
- `BANK_WEBHOOK_URL`: Your application's endpoint to receive webhook notifications (e.g., `https://yourdomain.com/api/webhooks`)
- `BANK_WEBHOOK_SECRET`: Secret for validating webhook requests

### SSL Configuration
- `SSL_KEYSTORE_PATH`: Path to the keystore file (e.g., `C:\path\to\keystore.jks`)
- `SSL_KEYSTORE_PASSWORD`: Password for the keystore
- `SSL_TRUSTSTORE_PATH`: Path to the truststore file (e.g., `C:\path\to\truststore.jks`)
- `SSL_TRUSTSTORE_PASSWORD`: Password for the truststore

## Setting Up Environment Variables

### Option 1: Temporary (Command Prompt)
```cmd
setx BANK_API_URL "https://api.hdfcbank.com"
setx BANK_CLIENT_ID "your_client_id"
setx BANK_CLIENT_SECRET "your_client_secret"
setx BANK_API_KEY "your_api_key"
```

### Option 2: Permanent (System Properties)
1. Press `Windows + R`, type `sysdm.cpl` and press Enter
2. Go to the "Advanced" tab
3. Click "Environment Variables"
4. Under "System variables", click "New"
5. Add each variable and its value
6. Click OK to save

### Option 3: Using .env File (Recommended for Development)
1. Create a file named `.env` in your project root
2. Add your variables:
   ```
   BANK_API_URL=https://api.hdfcbank.com
   BANK_CLIENT_ID=your_client_id
   BANK_CLIENT_SECRET=your_client_secret
   BANK_API_KEY=your_api_key
   ```
3. Add `.env` to your `.gitignore` file

## Running the Application

1. Open Command Prompt as Administrator
2. Navigate to your project directory
3. Run the application with production profile:
   ```
   java -jar -Dspring.profiles.active=prod your-application.jar
   ```

## Security Best Practices

1. **Never commit sensitive data** to version control
2. Use Windows Credential Manager for production secrets
3. Enable Windows Firewall and configure it to allow only necessary ports
4. Use Windows Defender or other antivirus software
5. Regularly rotate your API keys and secrets

## Troubleshooting

### Common Issues
1. **SSL Handshake Errors**:
   - Verify your keystore and truststore paths
   - Check if the certificates are valid and not expired

2. **Authentication Failures**:
   - Double-check your client ID and secret
   - Ensure there are no extra spaces in the values

3. **Connection Issues**:
   - Verify the API URL is correct
   - Check if your IP is whitelisted with HDFC Bank
   - Ensure your firewall allows outbound connections to the bank's API

### Checking Logs
Check the application logs in `logs/application.log` for detailed error messages.

## Support
For HDFC Bank API-specific issues, please contact HDFC Bank support with your client ID and the error details.
