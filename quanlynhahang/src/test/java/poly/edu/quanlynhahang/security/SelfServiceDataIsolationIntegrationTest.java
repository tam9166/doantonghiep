package poly.edu.quanlynhahang.security;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.ServiceZoneAssignment;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.WorkShiftDefinition;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.ServiceZoneAssignmentRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SelfServiceDataIsolationIntegrationTest {
    private static final String ACTOR = "reg_waiter_a";
    private static final String OTHER = "reg_waiter_b";
    private static final LocalDate FUTURE_DATE = LocalDate.of(2099, 12, 29);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TimekeepingRepository timekeepingRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private ServiceZoneAssignmentRepository zoneRepository;

    @Test
    void selfServiceEndpointsIgnoreAnotherUsernameFromTheClient() throws Exception {
        Account actor = saveAccount(ACTOR);
        Account other = saveAccount(OTHER);
        saveFutureRecords(actor, "Tầng A");
        saveFutureRecords(other, "Tầng B");

        mockMvc.perform(post("/api/timekeeping/check")
                        .with(user(ACTOR).roles("WAITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"IN\",\"username\":\"" + OTHER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.username").value(ACTOR));

        mockMvc.perform(get("/api/timekeeping/me")
                        .with(user(ACTOR).roles("WAITER"))
                        .param("startDate", FUTURE_DATE.toString())
                        .param("endDate", FUTURE_DATE.toString())
                        .param("username", OTHER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee.username").value(ACTOR));

        mockMvc.perform(get("/api/schedules/my-schedules")
                        .with(user(ACTOR).roles("WAITER"))
                        .param("startDate", FUTURE_DATE.toString())
                        .param("endDate", FUTURE_DATE.toString())
                        .param("username", OTHER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee.username").value(ACTOR));

        mockMvc.perform(get("/api/service-zones/my")
                        .with(user(ACTOR).roles("WAITER"))
                        .param("date", FUTURE_DATE.toString())
                        .param("username", OTHER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee.username").value(ACTOR))
                .andExpect(jsonPath("$[0].floor").value("Tầng A"));
    }

    @Test
    void accountSerializationAndNormalAdminResponsesDoNotExposeSecrets() throws Exception {
        Account account = new Account();
        account.setUsername("serialization-check");
        account.setPassword("sensitive-password-hash");
        account.setFullname("Serialization Check");
        account.setEmail("serialization-check@example.test");
        account.setTokenVersion(9L);
        account.setEnabled(false);
        account.setMustChangePassword(true);

        assertNoAccountSecrets(objectMapper.writeValueAsString(account));

        String staffJson = mockMvc.perform(get("/api/admin/staff")
                        .with(user("admin-audit").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertNoAccountSecrets(staffJson);

        String customerJson = mockMvc.perform(get("/api/admin/staff/customers")
                        .with(user("admin-audit").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertNoAccountSecrets(customerJson);

        String orderJson = mockMvc.perform(get("/api/admin/orders")
                        .with(user("admin-audit").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertNoAccountSecrets(orderJson);
    }

    @Test
    void anonymousBatchOneImagesAllowGetAndHeadOnly() throws Exception {
        mockMvc.perform(get("/images/products/com-ga-hoi-an-v2.jpg"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/images/products/com-ga-hoi-an-v2.jpg"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    private Account saveAccount(String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("not-used-by-mock-authentication");
        account.setFullname(username);
        account.setEmail(username + "@example.test");
        return accountRepository.saveAndFlush(account);
    }

    private void saveFutureRecords(Account account, String floor) {
        Timekeeping timekeeping = new Timekeeping();
        timekeeping.setAccount(account);
        timekeeping.setWorkDate(FUTURE_DATE);
        timekeeping.setCheckInTime(LocalTime.of(9, 0));
        timekeeping.setStatus("Đúng giờ");
        timekeepingRepository.save(timekeeping);

        Date legacyDate = Date.from(FUTURE_DATE.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        WorkSchedule schedule = new WorkSchedule();
        schedule.setAccount(account);
        schedule.setWorkDate(legacyDate);
        schedule.applyShift(WorkShiftDefinition.MORNING);
        workScheduleRepository.save(schedule);

        ServiceZoneAssignment zone = new ServiceZoneAssignment();
        zone.setAccount(account);
        zone.setFloor(floor);
        zone.setShift("Sáng");
        zone.setWorkDate(legacyDate);
        zoneRepository.save(zone);
    }

    private void assertNoAccountSecrets(String json) {
        String normalized = json.toLowerCase();
        assertFalse(normalized.contains("\"password\""));
        assertFalse(normalized.contains("passwordhash"));
        assertFalse(normalized.contains("tokenversion"));
        assertFalse(normalized.contains("\"authorities\""));
        assertFalse(normalized.contains("mustchangepassword"));
        assertFalse(normalized.contains("\"enabled\""));
    }
}
