package poly.edu.quanlynhahang.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class HotMenuItemQueryIntegrationTest {
    @Autowired
    private OrderDetailRepository orderDetails;

    @Test
    @Transactional(readOnly = true)
    void hotMenuQueryUsesTheCanonicalOrderDetailsTable() {
        assertNotNull(orderDetails.findHotMenuItems(4));
    }
}
