package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.WorkShiftDefinition;

@Service
public class AttendancePolicyService {
    private static final Logger log = LoggerFactory.getLogger(AttendancePolicyService.class);
    private final int lateGraceMinutes;

    public AttendancePolicyService(@Value("${app.attendance.late-grace-minutes:15}") int lateGraceMinutes) {
        this.lateGraceMinutes = lateGraceMinutes;
    }

    public boolean isLate(LocalTime actual, List<WorkSchedule> schedules) {
        if (actual == null || schedules == null || schedules.isEmpty()) return false;
        LocalTime expected = schedules.stream()
                .map(this::startTime)
                .min(Comparator.comparingLong(start -> circularDistanceMinutes(start, actual)))
                .orElse(null);
        if (expected == null) return false;
        long delta = ChronoUnit.MINUTES.between(expected, actual);
        if (delta < -720) delta += 1440;
        if (delta > 720) delta -= 1440;
        return delta > lateGraceMinutes;
    }

    public BigDecimal totalHours(LocalTime checkIn, LocalTime checkOut) {
        if (checkIn == null || checkOut == null) return BigDecimal.ZERO.setScale(2);
        long minutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        if (minutes < 0) minutes += 1440;
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private LocalTime startTime(WorkSchedule schedule) {
        if (schedule.getStartTime() != null) return schedule.getStartTime();
        try {
            return WorkShiftDefinition.fromLabel(schedule.getShift()).startTime();
        } catch (IllegalArgumentException exception) {
            log.warn("Schedule {} has no usable start time and an unknown shift label: {}",
                    schedule.getId(), schedule.getShift());
            return null;
        }
    }

    private long circularDistanceMinutes(LocalTime first, LocalTime second) {
        long difference = Math.abs(ChronoUnit.MINUTES.between(first, second));
        return Math.min(difference, 1440 - difference);
    }
}
