package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class ApplicationStartupValidatorTest {

    @Test
    void productionFailsFastWhenJwtSecretIsMissing() {
        ApplicationStartupValidator validator = validatorFor("production", "");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void stagingAlsoFailsFastWhenJwtSecretIsMissing() {
        ApplicationStartupValidator validator = validatorFor("staging", "");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void developmentMayUseTheStableDevelopmentOnlyKey() {
        ApplicationStartupValidator validator = validatorFor("dev", "");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void productionRejectsDisabledCaptcha() {
        ApplicationStartupValidator validator = productionValidator();
        ReflectionTestUtils.setField(validator, "captchaEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void productionRejectsMissingCaptchaSecret() {
        ApplicationStartupValidator validator = productionValidator();
        ReflectionTestUtils.setField(validator, "captchaSecret", "");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void productionRejectsMissingCaptchaProvider() {
        ApplicationStartupValidator validator = productionValidator();
        ReflectionTestUtils.setField(validator, "captchaProvider", " ");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void productionRejectsUnknownCaptchaProvider() {
        ApplicationStartupValidator validator = productionValidator();
        ReflectionTestUtils.setField(validator, "captchaProvider", "mock-like-provider");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    private ApplicationStartupValidator validatorFor(String profile, String jwtSecret) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", profile);
        environment.setActiveProfiles(profile);
        PaymentProperties paymentProperties = new PaymentProperties();
        ApplicationStartupValidator validator = new ApplicationStartupValidator(environment, paymentProperties);
        ReflectionTestUtils.setField(validator, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(validator, "paymentWebhookSecret", "");
        return validator;
    }

    private ApplicationStartupValidator productionValidator() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.setBankCode("MB");
        paymentProperties.setAccountNumber("12345678");
        paymentProperties.setAccountHolder("MOC VI");
        paymentProperties.setQrProvider("VIETQR");
        paymentProperties.setDemoMode(false);
        ApplicationStartupValidator validator = new ApplicationStartupValidator(environment, paymentProperties);
        ReflectionTestUtils.setField(validator, "jwtSecret", "a-production-jwt-secret-that-is-long-enough");
        ReflectionTestUtils.setField(validator, "paymentWebhookSecret", "webhook-secret");
        ReflectionTestUtils.setField(validator, "captchaEnabled", true);
        ReflectionTestUtils.setField(validator, "captchaProvider", "turnstile");
        ReflectionTestUtils.setField(validator, "captchaSecret", "captcha-secret");
        return validator;
    }
}
