package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PaymentPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAValidProductionPaymentConfiguration() {
        PaymentProperties properties = validProperties();

        assertDoesNotThrow(properties::assertProductionReady);
        assertEquals("1234******7890", properties.maskedAccountNumber());
    }

    @Test
    void rejectsDemoModeAndUnsupportedQrProvider() {
        PaymentProperties demo = validProperties();
        demo.setDemoMode(true);
        assertThrows(IllegalStateException.class, demo::assertProductionReady);

        PaymentProperties unsupportedProvider = validProperties();
        unsupportedProvider.setQrProvider("UNKNOWN");
        assertThrows(IllegalStateException.class, unsupportedProvider::assertProductionReady);
    }

    @Test
    void beanValidationRejectsMissingOrMalformedConfiguration() {
        PaymentProperties properties = new PaymentProperties();
        properties.setBankCode("");
        properties.setAccountNumber("000");
        properties.setAccountHolder("");
        properties.setQrProvider("");
        properties.setQrExpirationMinutes(0);

        Set<ConstraintViolation<PaymentProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
        assertEquals(5, violations.size());
    }

    private PaymentProperties validProperties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setBankCode("MB");
        properties.setBankBin("970422");
        properties.setAccountNumber("1234567890");
        properties.setAccountHolder("TEST ACCOUNT HOLDER");
        properties.setQrProvider("VIETQR");
        properties.setQrExpirationMinutes(15);
        properties.setDemoMode(false);
        return properties;
    }
}
