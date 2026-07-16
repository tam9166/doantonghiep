package poly.edu.quanlynhahang.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties(prefix = "restaurant.lucky-wheel")
public class LuckyWheelProperties {

    @Positive
    private double minimumEligibleOrderTotal = 3_000_000.0;

    @Positive
    private int maximumDiscountPercent = 5;

    @Valid
    @NotEmpty
    private List<Reward> rewards = new ArrayList<>(List.of(
            new Reward("points", 5, "5 ĐIỂM"),
            new Reward("points", 10, "10 ĐIỂM"),
            new Reward("discount", 5, "GIẢM 5%"),
            new Reward("miss", 0, "TRƯỢT"),
            new Reward("gift", 0, "TẶNG MÓN"),
            new Reward("points", 50, "50 ĐIỂM")));

    public double getMinimumEligibleOrderTotal() {
        return minimumEligibleOrderTotal;
    }

    public void setMinimumEligibleOrderTotal(double minimumEligibleOrderTotal) {
        this.minimumEligibleOrderTotal = minimumEligibleOrderTotal;
    }

    public int getMaximumDiscountPercent() {
        return maximumDiscountPercent;
    }

    public void setMaximumDiscountPercent(int maximumDiscountPercent) {
        this.maximumDiscountPercent = maximumDiscountPercent;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public void setRewards(List<Reward> rewards) {
        this.rewards = rewards;
    }

    public static class Reward {
        @NotBlank
        private String type;

        @PositiveOrZero
        private int value;

        @NotBlank
        private String label;

        public Reward() {
        }

        public Reward(String type, int value, String label) {
            this.type = type;
            this.value = value;
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
