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
    void acceptsRequiredProductionMbAccount() {
        PaymentProperties properties = validProperties();

        assertDoesNotThrow(properties::assertProductionReady);
        assertEquals("9191******6789", properties.maskedAccountNumber());
    }

    @Test
    void rejectsDemoModeAndWrongProductionAccount() {
        PaymentProperties demo = validProperties();
        demo.setDemoMode(true);
        assertThrows(IllegalStateException.class, demo::assertProductionReady);

        PaymentProperties wrongAccount = validProperties();
        wrongAccount.setAccountNumber("12345678");
        assertThrows(IllegalStateException.class, wrongAccount::assertProductionReady);
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
        properties.setBankCode(PaymentProperties.PRODUCTION_BANK_CODE);
        properties.setBankBin("970422");
        properties.setAccountNumber(PaymentProperties.PRODUCTION_ACCOUNT_NUMBER);
        properties.setAccountHolder(PaymentProperties.PRODUCTION_ACCOUNT_HOLDER);
        properties.setQrProvider("VIETQR");
        properties.setQrExpirationMinutes(15);
        properties.setDemoMode(false);
        return properties;
    }
}
