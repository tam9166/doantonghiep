package poly.edu.quanlynhahang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Validated
@ConfigurationProperties(prefix = "restaurant.payment")
public class PaymentProperties {

    public static final String PRODUCTION_BANK_CODE = "MB";
    public static final String PRODUCTION_ACCOUNT_NUMBER = "919112006789";
    public static final String PRODUCTION_ACCOUNT_HOLDER = "HOANG NGUYEN MINH TAM";

    @NotBlank
    private String bankCode;

    private String bankBin;

    @NotBlank
    @Pattern(regexp = "^\\d{8,20}$")
    private String accountNumber;

    @NotBlank
    private String accountHolder;

    @NotBlank
    private String qrProvider;

    @Min(1)
    private int qrExpirationMinutes = 15;

    private boolean demoMode;

    public void assertProductionReady() {
        if (demoMode) {
            throw new IllegalStateException("Payment demo mode is forbidden in production.");
        }
        if (!PRODUCTION_BANK_CODE.equalsIgnoreCase(bankCode.trim())) {
            throw new IllegalStateException("Production payment bank code is invalid.");
        }
        if (!PRODUCTION_ACCOUNT_NUMBER.equals(accountNumber.trim())) {
            throw new IllegalStateException("Production payment account number is invalid.");
        }
        if (!PRODUCTION_ACCOUNT_HOLDER.equalsIgnoreCase(accountHolder.trim())) {
            throw new IllegalStateException("Production payment account holder is invalid.");
        }
        if (!"VIETQR".equalsIgnoreCase(qrProvider.trim())) {
            throw new IllegalStateException("Unsupported production QR provider.");
        }
    }

    public String maskedAccountNumber() {
        String value = accountNumber == null ? "" : accountNumber.trim();
        if (value.length() < 8) {
            return "********";
        }
        return value.substring(0, 4) + "******" + value.substring(value.length() - 4);
    }
}
