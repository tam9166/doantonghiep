package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.TableLayout;
import tools.jackson.databind.ObjectMapper;

class TableLayoutContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsOutOfRangeFloorPlanCoordinates() {
        var invalid = new TableLayoutUpsertRequest(1, null, "Tầng 1",
                new BigDecimal("10001"), BigDecimal.ZERO,
                new BigDecimal("30"), new BigDecimal("170"), "RECTANGLE", BigDecimal.ZERO);

        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    void responseDoesNotExposePersistenceFields() throws Exception {
        TableLayout layout = new TableLayout();
        layout.setId(8L);
        layout.setTableId(3);
        layout.setFloorName("Tầng 2");
        layout.setActive(true);
        layout.setUpdatedAt(new Date());

        String json = new ObjectMapper().writeValueAsString(TableLayoutResponse.from(layout));

        assertTrue(json.contains("Tầng 2"));
        assertFalse(json.contains("updatedAt"));
        assertFalse(json.contains("active"));
        assertFalse(json.contains("\"id\""));
    }
}
