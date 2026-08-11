package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableCombinationPlannerTest {
    private final TableCombinationPlanner planner = new TableCombinationPlanner();

    @Test
    void doesNotSuggestCombinationWhenOneTableIsSufficient() {
        assertTrue(planner.findBestCombination(List.of(table(1, 8, "A1"), table(2, 4, "A2")), 6).isEmpty());
    }

    @Test
    void selectsTwoClosestTablesWithSmallestCapacitySurplus() {
        var result = planner.findBestCombination(List.of(
                table(1, 4, "A1"), table(2, 4, "A2"), table(3, 6, "B9")), 7).orElseThrow();

        assertEquals(List.of(1, 2), result.stream().map(RestaurantTable::getId).toList());
    }

    @Test
    void reportsNoCombinationWhenMoreThanFourTablesWouldBeRequired() {
        assertTrue(planner.findBestCombination(List.of(
                table(1, 2, "A1"), table(2, 2, "A2"), table(3, 2, "A3"),
                table(4, 2, "A4"), table(5, 2, "A5")), 9).isEmpty());
    }

    private RestaurantTable table(int id, int capacity, String position) {
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setCapacity(capacity);
        table.setMaxCapacity(capacity);
        table.setPositionDescription(position);
        return table;
    }
}
