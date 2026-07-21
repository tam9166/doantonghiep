package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Account;

public record EmployeeSummaryResponse(String username, String fullname) {
    public static EmployeeSummaryResponse from(Account account) {
        return new EmployeeSummaryResponse(account.getUsername(), account.getFullname());
    }
}
