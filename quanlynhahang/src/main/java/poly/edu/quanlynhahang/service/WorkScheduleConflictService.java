package poly.edu.quanlynhahang.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.WorkShiftDefinition;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;

@Service
public class WorkScheduleConflictService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final WorkScheduleRepository repository;

    public WorkScheduleConflictService(WorkScheduleRepository repository) {
        this.repository = repository;
    }

    public void requireAvailable(String username, LocalDate workDate,
                                 WorkShiftDefinition shift, Long excludedId) {
        Date from = toDate(workDate.minusDays(1));
        Date to = toDate(workDate.plusDays(1));
        List<WorkSchedule> candidates = repository
                .findByAccountUsernameAndWorkDateBetween(username, from, to);
        LocalDateTime requestedStart = workDate.atTime(shift.startTime());
        LocalDateTime requestedEnd = end(workDate, shift.startTime(), shift.endTime());
        boolean overlap = candidates.stream()
                .filter(existing -> excludedId == null || !excludedId.equals(existing.getId()))
                .anyMatch(existing -> overlaps(requestedStart, requestedEnd, existing));
        if (overlap) {
            throw new IllegalArgumentException("Ca làm bị trùng thời gian với lịch đã xếp của nhân viên");
        }
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, WorkSchedule existing) {
        LocalDate date = java.time.Instant.ofEpochMilli(existing.getWorkDate().getTime())
                .atZone(BUSINESS_ZONE).toLocalDate();
        LocalDateTime existingStart = date.atTime(existing.getStartTime());
        LocalDateTime existingEnd = end(date, existing.getStartTime(), existing.getEndTime());
        return start.isBefore(existingEnd) && existingStart.isBefore(end);
    }

    private LocalDateTime end(LocalDate date, LocalTime start, LocalTime end) {
        return (end.isAfter(start) ? date : date.plusDays(1)).atTime(end);
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(BUSINESS_ZONE).toInstant());
    }
}
