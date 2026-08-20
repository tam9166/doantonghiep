package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.Ingredient;
import tools.jackson.databind.ObjectMapper;

class IngredientApiContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidIngredientAndBatchPayloads() {
        assertFalse(validator.validate(new IngredientUpsertRequest("", "", new BigDecimal("-1.0"),
                new BigDecimal("-1"), "x".repeat(501), 0)).isEmpty());
        assertFalse(validator.validate(new IngredientBatchCreateRequest(BigDecimal.ZERO,
                new BigDecimal("-1"), null)).isEmpty());
    }

    @Test
    void ingredientResponseExposesOnlyInventoryFields() throws Exception {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(4L);
        ingredient.setName("Rau cải");
        ingredient.setQuantity(new BigDecimal("3.0000"));
        ingredient.setUnit("kg");

        String json = new ObjectMapper().writeValueAsString(IngredientResponse.from(ingredient));

        assertTrue(json.contains("Rau cải"));
        assertFalse(json.contains("hibernateLazyInitializer"));
    }
}
