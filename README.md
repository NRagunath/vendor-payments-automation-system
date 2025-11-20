# Vendor Payment Notifier

A comprehensive Spring Boot application for managing vendor payments, processing payment files in Excel format, sending payment notifications, and tracking payment statuses. This application provides a RESTful API for handling vendor payment operations with email notifications, Excel file processing, and scheduled tasks for overdue payments.

## ✨ Features

- **Vendor Management**: Create, update, and manage vendor information
- **Excel Processing**: Import vendor payment data from Excel files with validation
- **Payment Processing**: Handle payment creation, updates, and status tracking
- **Email Notifications**: Automatic email notifications for payment status changes
- **Scheduled Tasks**: Daily checks for overdue payments
- **RESTful API**: Comprehensive API documentation with Swagger/OpenAPI
- **Asynchronous Processing**: Non-blocking operations for better performance
- **Validation**: Request validation and proper error handling
- **Logging**: Comprehensive logging for monitoring and debugging
- **Template Management**: Generate and download Excel templates for payment uploads

## 🚀 Prerequisites

- Java 17 or later
- Maven 3.8 or later
- Oracle Database 12c or later (or compatible database)
- SMTP server for email notifications (e.g., Gmail, SendGrid)
- Apache POI for Excel processing (included in dependencies)

## 🛠️ Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd VendorPaymentNotifier
   ```

2. **Database Setup**
   - Create an Oracle database
   - Update database configuration in `src/main/resources/application.properties`

3. **Configuration**
   - Configure email settings in `application.properties`
   - Update bank API credentials if integrating with payment gateways
   - Customize application settings as needed

4. **Build and Run**
   ```bash
   # Build the application
   mvn clean install
   
   # Run the application
   mvn spring-boot:run
   ```

## 🌐 API Endpoints

### Excel Processing
- `POST /api/v1/vendor-payments/excel/upload` - Upload and process Excel file with vendor payments
- `GET /api/v1/vendor-payments/excel/template` - Download Excel template for vendor payments

### API Documentation

Once the application is running, you can access:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## ⚙️ Configuration

Key configuration options in `application.properties`:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Database Configuration
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/YOUR_SID
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Email Configuration
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your-email@example.com
spring.mail.password=your-email-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# File Upload Configuration
app.upload.dir=${user.home}/.vendor-payments/uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Async Configuration
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100

# Logging
logging.level.com.shanthigear=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG

# CORS Configuration
app.cors.allowed-origins=http://localhost:3000,http://localhost:4200
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=*
app.cors.allow-credentials=true

# Application Settings
app.payment.overdue-check-cron=0 0 9 * * *  # Run daily at 9 AM
app.notification.enabled=true
```

## ✉️ Email Testing

### Using Ethereal Email (Test SMTP)

1. **Get Ethereal Test Credentials**
   ```bash
   npx ethereal-email
   ```
   This will generate new test SMTP credentials. Save the email and password.

2. **Update Email Configuration**
   Edit `src/main/resources/application.properties` with your Ethereal credentials:
   ```properties
   # SMTP Configuration
   spring.mail.host=smtp.ethereal.email
   spring.mail.port=587
   spring.mail.username=YOUR_ETHEREAL_EMAIL
   spring.mail.password=YOUR_ETHEREAL_PASSWORD
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   
   # Sender Configuration
   app.email.sender=YOUR_ETHEREAL_EMAIL
   app.email.sender-name=Vendor Payment System
   ```

3. **Send a Test Email**
   Run the test email sender:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.main-class=com.shanthigear.TestEmailSender
   ```
   Or use the simple email test:
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.shanthigear.SimpleEmailTest"
   ```

4. **Check Sent Emails**
   - Go to https://ethereal.email/messages
   - Log in with your Ethereal credentials
   - You should see the test email in your inbox

### Common Issues
- If emails don't appear, check the console for errors
- Ensure your internet connection allows SMTP traffic (port 587)
- Verify the Ethereal account is active (sessions expire after some time)
- Check spam/junk folder in Ethereal

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=YourTestClass

# Run with coverage (requires JaCoCo)
mvn clean test jacoco:report
```

## 🏗️ Building for Production

```bash
# Build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run the application
java -jar target/vendor-payment-notifier-1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

## 🔒 Security Considerations

- Always use HTTPS in production
- Store sensitive information (database passwords, API keys) in environment variables or a secure vault
- Implement proper authentication and authorization
- Regularly update dependencies for security patches

## 📝 License

Proprietary - All rights reserved.

## 📧 Contact

For support or inquiries, please contact support@shanthigear.com
