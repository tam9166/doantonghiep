package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.ReservationRequest;
import poly.edu.quanlynhahang.dto.WaitlistActionRequest;
import poly.edu.quanlynhahang.dto.WaitlistRequest;
import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.entity.AreaPricing;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.AreaPricingRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

@SpringBootTest
class ReservationConcurrencyIntegrationTest {
    @Autowired ReservationService reservationService;
    @Autowired ReservationWaitlistService waitlistService;
    @Autowired TableAreaRepository areaRepository;
    @Autowired AreaPricingRepository areaPricingRepository;
    @Autowired RestaurantTableRepository tableRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired VoucherRepository voucherRepository;

    @MockitoBean NotificationService notificationService;
    @MockitoBean ActivityLogService activityLogService;
    @MockitoBean ReservationRealtimeService realtimeService;

    private String customerMarker;
    private Integer tableId;
    private Integer areaId;
    private Long voucherId;
    private String voucherCode;

    @AfterEach
    void cleanup() {
        cleanupRegressionRows();
    }

    @Test
    @Timeout(30)
    void twoCustomersCompetingForTheLastTableAllowExactlyOneReservation() throws Exception {
        createOnlyTableFixture();
        LocalDate bookingDate = LocalDate.now().plusDays(7);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(() -> createAfter(start, bookingDate, "race-first"));
            Future<Throwable> second = executor.submit(() -> createAfter(start, bookingDate, "race-second"));
            start.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            outcomes.add(first.get(20, TimeUnit.SECONDS));
            outcomes.add(second.get(20, TimeUnit.SECONDS));
            String outcomeSummary = outcomes.stream().map(outcome -> outcome == null ? "SUCCESS"
                    : outcome.getClass().getSimpleName() + ": " + outcome.getMessage()).toList().toString();
            assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count(), outcomeSummary);
            List<Throwable> failures = outcomes.stream().filter(java.util.Objects::nonNull).toList();
            assertEquals(1, failures.size());
            assertInstanceOf(ResponseStatusException.class, failures.getFirst());
            assertEquals(409, ((ResponseStatusException) failures.getFirst()).getStatusCode().value());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE customer_name = ?",
                    Integer.class, customerMarker));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    void concurrentRetryWithTheSameIdempotencyKeyCreatesOneReservation() throws Exception {
        createOnlyTableFixture();
        LocalDate bookingDate = LocalDate.now().plusDays(8);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(() -> createAfter(start, bookingDate, "same-key"));
            Future<Throwable> second = executor.submit(() -> createAfter(start, bookingDate, "same-key"));
            start.countDown();
            List<Throwable> outcomes = new ArrayList<>();
            outcomes.add(first.get(20, TimeUnit.SECONDS));
            outcomes.add(second.get(20, TimeUnit.SECONDS));

            assertTrue(outcomes.stream().allMatch(java.util.Objects::isNull),
                    () -> outcomes.stream().filter(java.util.Objects::nonNull)
                            .map(Throwable::toString).toList().toString());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE customer_name = ?",
                    Integer.class, customerMarker));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    void twoWaitlistEntriesCannotClaimTheSameReservation() throws Exception {
        createOnlyTableFixture();
        LocalDate bookingDate = LocalDate.now().plusDays(9);
        String reservationCode = reservationService.createReservation(
                request(bookingDate), "reservation-" + customerMarker + "-waitlist-link")
                .getReservationCode();
        Long firstId = waitlistService.create(waitlistRequest(bookingDate)).getId();
        Long secondId = waitlistService.create(waitlistRequest(bookingDate)).getId();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(
                    () -> convertWaitlistAfter(start, firstId, reservationCode));
            Future<Throwable> second = executor.submit(
                    () -> convertWaitlistAfter(start, secondId, reservationCode));
            start.countDown();
            List<Throwable> outcomes = new ArrayList<>();
            outcomes.add(first.get(20, TimeUnit.SECONDS));
            outcomes.add(second.get(20, TimeUnit.SECONDS));

            String outcomeSummary = outcomes.stream().map(outcome -> outcome == null ? "SUCCESS"
                    : outcome.getClass().getSimpleName() + ": " + outcome.getMessage()).toList().toString();
            assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count(), outcomeSummary);
            Throwable failure = outcomes.stream().filter(java.util.Objects::nonNull).findFirst().orElseThrow();
            assertInstanceOf(ResponseStatusException.class, failure);
            assertEquals(409, ((ResponseStatusException) failure).getStatusCode().value());
            assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM reservation_waitlist
                    WHERE linked_reservation_code = ? AND status = 'CONVERTED'
                    """, Integer.class, reservationCode));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    void twoReservationsCompetingForTheLastVoucherUseAllowExactlyOneSuccess() throws Exception {
        createOnlyTableFixture();
        makeAreaChargeable();
        Voucher voucher = new Voucher();
        voucherCode = "REGV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        voucher.setCode(voucherCode);
        voucher.setDiscountPercent(10);
        voucher.setUsageLimit(1);
        voucher.setIsUsed(false);
        voucher.setCreateDate(new java.util.Date());
        voucherId = voucherRepository.save(voucher).getId();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(() -> createWithVoucherAfter(
                    start, LocalDate.now().plusDays(10), "voucher-first"));
            Future<Throwable> second = executor.submit(() -> createWithVoucherAfter(
                    start, LocalDate.now().plusDays(11), "voucher-second"));
            start.countDown();
            List<Throwable> outcomes = new ArrayList<>();
            outcomes.add(first.get(20, TimeUnit.SECONDS));
            outcomes.add(second.get(20, TimeUnit.SECONDS));

            String outcomeSummary = outcomes.stream().map(outcome -> outcome == null ? "SUCCESS"
                    : outcome.getClass().getSimpleName() + ": " + outcome.getMessage()).toList().toString();
            assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count(), outcomeSummary);
            Throwable failure = outcomes.stream().filter(java.util.Objects::nonNull).findFirst().orElseThrow();
            assertInstanceOf(ResponseStatusException.class, failure);
            assertEquals(409, ((ResponseStatusException) failure).getStatusCode().value());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE customer_name = ?",
                    Integer.class, customerMarker));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM reservation_voucher_usages WHERE voucher_id = ?",
                    Integer.class, voucherId));
            assertTrue(Boolean.TRUE.equals(voucherRepository.findById(voucherId).orElseThrow().getIsUsed()));
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable createAfter(CountDownLatch start, LocalDate date, String keySuffix) {
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            reservationService.createReservation(request(date),
                    "reservation-" + customerMarker + "-" + keySuffix);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private Throwable convertWaitlistAfter(CountDownLatch start, Long id, String reservationCode) {
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            WaitlistActionRequest action = new WaitlistActionRequest();
            action.setLinkedReservationCode(reservationCode);
            waitlistService.convert(id, action);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private Throwable createWithVoucherAfter(CountDownLatch start, LocalDate date, String keySuffix) {
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            ReservationRequest request = request(date);
            request.setVoucherCode(voucherCode);
            request.setGuestCount(12);
            reservationService.createReservation(request,
                    "reservation-" + customerMarker + "-" + keySuffix);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void createOnlyTableFixture() {
        cleanupRegressionRows();
        customerMarker = "reg_reservation_race_" + UUID.randomUUID().toString().substring(0, 8);
        TableArea area = new TableArea();
        area.setNameVi(customerMarker);
        area.setNameEn(customerMarker);
        area.setAreaType(AreaType.DINING);
        area.setCapacity(30);
        area.setBasePrice(new BigDecimal("100000"));
        area.setStatus("ACTIVE");
        area = areaRepository.save(area);
        areaId = area.getId();

        RestaurantTable table = new RestaurantTable();
        table.setName(customerMarker);
        table.setFloor("REGRESSION");
        table.setAreaId(areaId);
        table.setCapacity(4);
        table.setMinCapacity(1);
        table.setMaxCapacity(4);
        table.setSeatCount(4);
        table.setIsOccupied(0);
        table.setActive(true);
        table.setDisplayOrder(0);
        tableId = tableRepository.save(table).getId();

        // The production booking-ready rule requires at least two active tables in a dining/private area.
        // This second table keeps the area operational but is intentionally too small for the two-guest
        // race, so the test still validates competition for exactly one assignable table.
        RestaurantTable supportTable = new RestaurantTable();
        supportTable.setName(customerMarker + "_support");
        supportTable.setFloor("REGRESSION");
        supportTable.setAreaId(areaId);
        supportTable.setCapacity(1);
        supportTable.setMinCapacity(1);
        supportTable.setMaxCapacity(1);
        supportTable.setSeatCount(1);
        supportTable.setIsOccupied(0);
        supportTable.setActive(true);
        supportTable.setDisplayOrder(1);
        tableRepository.save(supportTable);
    }

    private void makeAreaChargeable() {
        TableArea area = areaRepository.findById(areaId).orElseThrow();
        area.setAreaType(AreaType.PRIVATE_ROOM);
        areaRepository.save(area);

        AreaPricing pricing = new AreaPricing();
        pricing.setArea(area);
        pricing.setRoomFee(new BigDecimal("100000"));
        pricing.setMinimumSpend(BigDecimal.ZERO);
        pricing.setActive(true);
        areaPricingRepository.save(pricing);
    }

    private void cleanupRegressionRows() {
        jdbc.update("DELETE FROM reservation_waitlist WHERE customer_name LIKE 'reg_reservation_race_%'");
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM reservations WHERE customer_name LIKE 'reg_reservation_race_%'", Long.class);
        for (Long id : ids) {
            jdbc.update("DELETE FROM reservation_status_history WHERE reservation_id = ?", id);
            jdbc.update("DELETE FROM reservation_tables WHERE reservation_id = ?", id);
            jdbc.update("DELETE FROM reservation_voucher_usages WHERE reservation_id = ?", id);
            jdbc.update("DELETE FROM reservations WHERE id = ?", id);
        }
        if (voucherId != null) {
            jdbc.update("DELETE FROM reservation_voucher_usages WHERE voucher_id = ?", voucherId);
            voucherRepository.deleteById(voucherId);
            voucherId = null;
            voucherCode = null;
        }
        // area_pricing references the area, so remove every stale pricing row
        // by fixture name before deleting table_areas. This also handles a
        // prior interrupted run where the in-memory areaId is unavailable.
        jdbc.update("DELETE p FROM area_pricing p JOIN table_areas a ON a.id = p.area_id "
                + "WHERE a.name_vi LIKE 'reg_reservation_race_%'");
        jdbc.update("DELETE FROM restaurant_table WHERE name LIKE 'reg_reservation_race_%'");
        jdbc.update("DELETE FROM table_areas WHERE name_vi LIKE 'reg_reservation_race_%'");
    }

    private ReservationRequest request(LocalDate date) {
        ReservationRequest request = new ReservationRequest();
        request.setCustomerName(customerMarker);
        request.setCustomerPhone("0912345678");
        request.setCustomerEmail("race@example.test");
        request.setReservationDate(date.toString());
        request.setArrivalTime("18:00");
        request.setExpectedDurationMinutes(120);
        request.setGuestCount(2);
        request.setAreaId(areaId);
        request.setPreorderEnabled(false);
        request.setPreorderItems(new ArrayList<>());
        request.setPaymentOption(PaymentOption.DEPOSIT_50);
        return request;
    }

    private WaitlistRequest waitlistRequest(LocalDate date) {
        WaitlistRequest request = new WaitlistRequest();
        request.setCustomerName(customerMarker);
        request.setCustomerPhone("0912345678");
        request.setCustomerEmail("race@example.test");
        request.setReservationDate(date.toString());
        request.setPreferredStartTime("18:00");
        request.setPreferredEndTime("20:00");
        request.setGuestCount(2);
        request.setAreaId(areaId);
        return request;
    }
}
