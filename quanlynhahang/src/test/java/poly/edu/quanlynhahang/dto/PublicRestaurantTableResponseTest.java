package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.RestaurantTable;
import tools.jackson.databind.ObjectMapper;

class PublicRestaurantTableResponseTest {

    @Test
    void doesNotExposeInternalReservationNotes() throws Exception {
        RestaurantTable table = new RestaurantTable();
        table.setId(1);
        table.setName("B01");
        table.setReservedTime("ORDER:1234 | customer note");

        String json = new ObjectMapper().writeValueAsString(PublicRestaurantTableResponse.from(table));

        assertTrue(json.contains("B01"));
        assertFalse(json.contains("reservedTime"));
        assertFalse(json.contains("customer note"));
    }
}
