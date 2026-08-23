package poly.edu.quanlynhahang.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class StringListJsonConverterTest {
    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void malformedPersistedJsonIsReportedInsteadOfSilentlyBecomingAnEmptyList() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("not-json"));
    }

    @Test
    void validJsonStillRoundTrips() {
        assertEquals(List.of("a", "b"),
                converter.convertToEntityAttribute(converter.convertToDatabaseColumn(List.of("a", "b"))));
    }
}
