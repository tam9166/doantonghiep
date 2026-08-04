package poly.edu.quanlynhahang.dto;

import lombok.Data;
import poly.edu.quanlynhahang.entity.WaitlistStatus;

import java.util.Date;

@Data
public class WaitlistResponse {
    private Long id;
    private String waitlistCode;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String reservationDate;
    private String preferredStartTime;
    private String preferredEndTime;
    private Integer guestCount;
    private Integer areaId;
    private String areaName;
    private String seatingPreference;
    private String specialRequest;
    private WaitlistStatus status;
    private String linkedReservationCode;
    private String managerNote;
    private Date contactedAt;
    private Date createdAt;
    private Date updatedAt;
}
