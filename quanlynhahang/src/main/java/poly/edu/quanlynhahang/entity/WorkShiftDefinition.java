package poly.edu.quanlynhahang.entity;

import java.time.LocalTime;
import java.util.Arrays;

public enum WorkShiftDefinition {
    MORNING("Sáng", LocalTime.of(6, 0), LocalTime.of(14, 0)),
    AFTERNOON("Chiều", LocalTime.of(14, 0), LocalTime.of(22, 0)),
    NIGHT("Tối", LocalTime.of(22, 0), LocalTime.of(6, 0));

    private final String label;
    private final LocalTime startTime;
    private final LocalTime endTime;

    WorkShiftDefinition(String label, LocalTime startTime, LocalTime endTime) {
        this.label = label;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String label() { return label; }
    public LocalTime startTime() { return startTime; }
    public LocalTime endTime() { return endTime; }

    public static WorkShiftDefinition fromLabel(String label) {
        return Arrays.stream(values())
                .filter(value -> value.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ca làm việc không hợp lệ: " + label));
    }
}
