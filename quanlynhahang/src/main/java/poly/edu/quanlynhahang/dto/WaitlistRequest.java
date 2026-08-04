package poly.edu.quanlynhahang.dto;

import lombok.Data;

@Data
public class WaitlistRequest {
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String reservationDate;
    private String preferredStartTime;
    private String preferredEndTime;
    private Integer guestCount;
    private Integer areaId;
    private String seatingPreference;
    private String specialRequest;
}
