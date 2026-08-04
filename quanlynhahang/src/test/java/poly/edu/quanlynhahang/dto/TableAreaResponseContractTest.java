package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.TableArea;
import tools.jackson.databind.ObjectMapper;

class TableAreaResponseContractTest {

    @Test
    void responseExposesOnlyTheAreaContract() throws Exception {
        TableArea area = new TableArea();
        area.setId(7);
        area.setNameVi("Phong rieng");
        area.setStatus("ACTIVE");
        area.setCreatedAt(new java.util.Date(0));
        area.setUpdatedAt(new java.util.Date(1));

        String json = new ObjectMapper().writeValueAsString(TableAreaResponse.from(area));

        assertTrue(json.contains("Phong rieng"));
        assertFalse(json.contains("createdAt"));
        assertFalse(json.contains("updatedAt"));
    }
}
