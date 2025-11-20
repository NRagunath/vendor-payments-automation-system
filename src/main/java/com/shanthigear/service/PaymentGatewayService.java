package com.shanthigear.service;

import com.shanthigear.model.Payment;
import com.shanthigear.model.PaymentStatus;

public interface PaymentGatewayService {
    Payment processPayment(Payment payment);
    PaymentStatus checkPaymentStatus(String paymentId);
    boolean cancelPayment(String paymentId);
}
