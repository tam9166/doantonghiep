package poly.edu.quanlynhahang.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StaffOperationsEndpointSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCallerCannotUseOperationsAssistant() throws Exception {
        mockMvc.perform(post("/api/staff/ai/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Món nào khả dụng?\"}"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCallerCannotUseStructuredOperationsAssistant() throws Exception {
        mockMvc.perform(post("/api/staff/assistant/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Món nào khả dụng?\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cashierCanQueryUnpaidInvoicesButKitchenCannot() throws Exception {
        String payload = "{\"message\":\"Có bao nhiêu hóa đơn chưa thanh toán?\",\"locale\":\"vi\"}";

        mockMvc.perform(post("/api/staff/assistant/query")
                        .with(user("cashier").roles("CASHIER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("UNPAID_INVOICE_SUMMARY"))
                .andExpect(jsonPath("$.data.invoiceCount").isNumber());

        mockMvc.perform(post("/api/staff/assistant/query")
                        .with(user("kitchen").roles("KITCHEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "MANAGER", "KITCHEN", "WAITER", "CASHIER"})
    void eachStaffRoleCanUseOperationsAssistant(String role) throws Exception {
        mockMvc.perform(post("/api/staff/ai/operations")
                        .with(user("staff-" + role.toLowerCase()).roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Món nào khả dụng?\"}"))
                .andExpect(status().isOk());
    }
}
