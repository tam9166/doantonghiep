package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.DepositPolicyResponse;
import poly.edu.quanlynhahang.entity.DepositPolicy;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.DepositPolicyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Service
public class DepositPolicyService {
    private final DepositPolicyRepository depositPolicyRepository;

    public DepositPolicyService(DepositPolicyRepository depositPolicyRepository) {
        this.depositPolicyRepository = depositPolicyRepository;
    }

    @Transactional(readOnly = true)
    public List<DepositPolicy> findAll() {
        return depositPolicyRepository.findAllByOrderByPriorityDescIdDesc();
    }

    @Transactional
    public DepositPolicy save(DepositPolicy policy) {
        if (policy.getPolicyCode() == null || policy.getPolicyCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã chính sách không được để trống");
        }
        if (policy.getNameVi() == null || policy.getNameVi().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tên chính sách không được để trống");
        }
        if (policy.getPolicyType() == null || policy.getPolicyType().isBlank()) {
            policy.setPolicyType("PERCENTAGE");
        }
        policy.setUpdatedAt(new Date());
        return depositPolicyRepository.save(policy);
    }

    @Transactional
    public DepositPolicy update(Long id, DepositPolicy request) {
        DepositPolicy existing = depositPolicyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chính sách cọc"));
        existing.setPolicyCode(request.getPolicyCode());
        existing.setNameVi(request.getNameVi());
        existing.setNameEn(request.getNameEn());
        existing.setPolicyType(request.getPolicyType());
        existing.setPercentageRate(request.getPercentageRate());
        existing.setFixedAmount(request.getFixedAmount());
        existing.setAmountPerGuest(request.getAmountPerGuest());
        existing.setMinimumAmount(request.getMinimumAmount());
        existing.setMaximumAmount(request.getMaximumAmount());
        existing.setAreaId(request.getAreaId());
        existing.setTableType(request.getTableType());
        existing.setDayOfWeek(request.getDayOfWeek());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setMinimumGuests(request.getMinimumGuests());
        existing.setMinimumOrderAmount(request.getMinimumOrderAmount());
        existing.setPriority(request.getPriority());
        existing.setActive(request.getActive());
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        return save(existing);
    }

    @Transactional
    public void deactivate(Long id) {
        DepositPolicy existing = depositPolicyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chính sách cọc"));
        existing.setActive(false);
        existing.setUpdatedAt(new Date());
        depositPolicyRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public DepositCalculation calculate(BigDecimal totalAmount,
                                        int guestCount,
                                        LocalDate date,
                                        LocalTime time,
                                        Integer areaId,
                                        RestaurantTable table,
                                        BigDecimal fallbackRate) {
        DepositPolicy policy = depositPolicyRepository.findByActiveTrue().stream()
                .filter(p -> matches(p, totalAmount, guestCount, date, time, areaId, table))
                .sorted((a, b) -> Integer.compare(value(b.getPriority()), value(a.getPriority())))
                .findFirst()
                .orElse(null);

        if (policy == null) {
            BigDecimal amount = totalAmount.multiply(fallbackRate).setScale(0, RoundingMode.HALF_UP);
            DepositPolicyResponse response = new DepositPolicyResponse();
            response.setPolicyCode("CONFIG_DEFAULT");
            response.setNameVi("Cọc mặc định");
            response.setPolicyType("PERCENTAGE");
            response.setPercentageRate(fallbackRate);
            response.setDepositAmount(amount);
            response.setFormula("total * " + fallbackRate);
            response.setExplanation("Áp dụng tỷ lệ cọc mặc định trong cấu hình hệ thống.");
            return new DepositCalculation(amount, fallbackRate, response);
        }

        BigDecimal amount = calculateAmount(policy, totalAmount, guestCount);
        BigDecimal rate = totalAmount.signum() == 0 ? BigDecimal.ZERO
                : amount.divide(totalAmount, 4, RoundingMode.HALF_UP);
        DepositPolicyResponse response = toResponse(policy);
        response.setDepositAmount(amount);
        response.setFormula(formula(policy));
        response.setExplanation("Áp dụng chính sách " + policy.getNameVi() + " theo priority " + policy.getPriority() + ".");
        return new DepositCalculation(amount, rate, response);
    }

    private boolean matches(DepositPolicy p, BigDecimal total, int guests, LocalDate date, LocalTime time,
                            Integer areaId, RestaurantTable table) {
        if (!Boolean.TRUE.equals(p.getActive())) return false;
        if (p.getEffectiveFrom() != null && date.isBefore(p.getEffectiveFrom())) return false;
        if (p.getEffectiveTo() != null && date.isAfter(p.getEffectiveTo())) return false;
        if (p.getAreaId() != null && !p.getAreaId().equals(areaId)) return false;
        if (p.getDayOfWeek() != null && p.getDayOfWeek() != date.getDayOfWeek().getValue()) return false;
        if (p.getStartTime() != null && time.isBefore(p.getStartTime())) return false;
        if (p.getEndTime() != null && time.isAfter(p.getEndTime())) return false;
        if (p.getMinimumGuests() != null && guests < p.getMinimumGuests()) return false;
        if (p.getMinimumOrderAmount() != null && total.compareTo(p.getMinimumOrderAmount()) < 0) return false;
        if (p.getTableType() != null && table != null) {
            String type = p.getTableType().toUpperCase();
            if ("PRIVATE_ROOM".equals(type) && !Boolean.TRUE.equals(table.getPrivateRoom())) return false;
            if ("WINDOW".equals(type) && !Boolean.TRUE.equals(table.getWindowSeat())) return false;
        }
        return true;
    }

    private BigDecimal calculateAmount(DepositPolicy policy, BigDecimal total, int guests) {
        BigDecimal amount;
        String type = policy.getPolicyType() == null ? "PERCENTAGE" : policy.getPolicyType().toUpperCase();
        if ("FIXED".equals(type)) {
            amount = nvl(policy.getFixedAmount());
        } else if ("PER_GUEST".equals(type)) {
            amount = nvl(policy.getAmountPerGuest()).multiply(BigDecimal.valueOf(guests));
        } else {
            amount = total.multiply(nvl(policy.getPercentageRate()));
        }
        if (policy.getMinimumAmount() != null && amount.compareTo(policy.getMinimumAmount()) < 0) {
            amount = policy.getMinimumAmount();
        }
        if (policy.getMaximumAmount() != null && amount.compareTo(policy.getMaximumAmount()) > 0) {
            amount = policy.getMaximumAmount();
        }
        if (amount.compareTo(total) > 0) {
            amount = total;
        }
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private DepositPolicyResponse toResponse(DepositPolicy policy) {
        DepositPolicyResponse response = new DepositPolicyResponse();
        response.setPolicyCode(policy.getPolicyCode());
        response.setNameVi(policy.getNameVi());
        response.setNameEn(policy.getNameEn());
        response.setPolicyType(policy.getPolicyType());
        response.setPercentageRate(policy.getPercentageRate());
        response.setFixedAmount(policy.getFixedAmount());
        response.setAmountPerGuest(policy.getAmountPerGuest());
        response.setMinimumAmount(policy.getMinimumAmount());
        response.setMaximumAmount(policy.getMaximumAmount());
        return response;
    }

    private String formula(DepositPolicy policy) {
        String type = policy.getPolicyType() == null ? "PERCENTAGE" : policy.getPolicyType().toUpperCase();
        if ("FIXED".equals(type)) return "fixed_amount";
        if ("PER_GUEST".equals(type)) return "amount_per_guest * guest_count";
        return "total * percentage_rate";
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    public record DepositCalculation(BigDecimal amount, BigDecimal rate, DepositPolicyResponse policy) {}
}
