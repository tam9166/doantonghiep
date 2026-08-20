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

    private ApplicationStartupValidator validatorFor(String profile, String jwtSecret) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", profile);
        environment.setActiveProfiles(profile);
        PaymentProperties paymentProperties = new PaymentProperties();
        ApplicationStartupValidator validator = new ApplicationStartupValidator(environment, paymentProperties);
        ReflectionTestUtils.setField(validator, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(validator, "paymentWebhookSecret", "");
        return validator;
    }
}
