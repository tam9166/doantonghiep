package poly.edu.quanlynhahang.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.IntUnaryOperator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.WheelSpinResponse;
import poly.edu.quanlynhahang.config.LuckyWheelProperties;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.entity.WheelSpinHistory;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;
import poly.edu.quanlynhahang.repository.WheelSpinHistoryRepository;

@Service
public class LuckyWheelService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final WheelSpinHistoryRepository spinHistoryRepository;
    private final PointsLedgerService pointsLedgerService;
    private final LuckyWheelProperties properties;
    private final IntUnaryOperator rewardIndexSelector;

    @Autowired
    public LuckyWheelService(AccountRepository accountRepository,
                             OrderRepository orderRepository,
                             VoucherRepository voucherRepository,
                             WheelSpinHistoryRepository spinHistoryRepository,
                             PointsLedgerService pointsLedgerService,
                             LuckyWheelProperties properties) {
        this(accountRepository, orderRepository, voucherRepository, spinHistoryRepository,
                pointsLedgerService, properties, RANDOM::nextInt);
    }

    LuckyWheelService(AccountRepository accountRepository,
                      OrderRepository orderRepository,
                      VoucherRepository voucherRepository,
                      WheelSpinHistoryRepository spinHistoryRepository,
                      PointsLedgerService pointsLedgerService,
                      LuckyWheelProperties properties,
                      IntUnaryOperator rewardIndexSelector) {
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
        this.voucherRepository = voucherRepository;
        this.spinHistoryRepository = spinHistoryRepository;
        this.pointsLedgerService = pointsLedgerService;
        this.properties = properties;
        this.rewardIndexSelector = rewardIndexSelector;
    }

    @Transactional
    public WheelSpinResponse spin(String username) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Account account = accountRepository.findLockedByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND"));
        if (spinHistoryRepository.findByAccountUsernameAndSpinDate(username, today).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LUCKY_WHEEL_DAILY_LIMIT");
        }

        Date start = Date.from(today.atStartOfDay(BUSINESS_ZONE).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        if (!orderRepository.existsEligibleLuckyWheelOrder(
                username, 4, true, properties.getMinimumEligibleOrderTotal(), start, end)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "LUCKY_WHEEL_NOT_ELIGIBLE");
        }

        Reward reward = drawReward();
        WheelSpinHistory history = new WheelSpinHistory();
        history.setAccount(account);
        history.setSpinDate(today);
        history.setRewardType(reward.type());
        history.setRewardValue(reward.value());
        history = spinHistoryRepository.save(history);

        String voucherCode = null;
        if ("discount".equals(reward.type())) {
            int discount = Math.min(reward.value(), properties.getMaximumDiscountPercent());
            voucherCode = generateVoucherCode(discount);
            Voucher voucher = new Voucher();
            voucher.setCode(voucherCode);
            voucher.setDiscountPercent(discount);
            voucher.setCreateDate(new Date());
            voucher.setIsUsed(false);
            voucher.setActive(true);
            voucher.setUsageLimit(1);
            voucher.setUsedCount(0);
            voucher.setAccount(account);
            voucherRepository.save(voucher);
            history.setVoucherCode(voucherCode);
            spinHistoryRepository.save(history);
        } else if ("points".equals(reward.type())) {
            pointsLedgerService.credit(
                    username,
                    PointsEventType.LUCKY_WHEEL,
                    "LUCKY_WHEEL:" + history.getId(),
                    reward.value(),
                    "Điểm thưởng vòng quay ngày " + today);
        }

        return new WheelSpinResponse(
                reward.type(),
                reward.value(),
                reward.label(),
                voucherCode,
                account.getPoints(),
                account.getMembershipTier());
    }

    private Reward drawReward() {
        List<LuckyWheelProperties.Reward> rewards = properties.getRewards();
        LuckyWheelProperties.Reward configured = rewards.get(rewardIndexSelector.applyAsInt(rewards.size()));
        int value = "discount".equals(configured.getType())
                ? Math.min(configured.getValue(), properties.getMaximumDiscountPercent())
                : configured.getValue();
        return new Reward(configured.getType(), value, configured.getLabel());
    }

    private String generateVoucherCode(int discount) {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        StringBuilder suffix = new StringBuilder();
        for (byte value : bytes) {
            suffix.append(String.format("%02X", value));
        }
        return "LUCKY-" + discount + "-" + suffix;
    }

    private record Reward(String type, int value, String label) {
    }
}
