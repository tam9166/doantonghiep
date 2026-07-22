package poly.edu.quanlynhahang.dto;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.controller.AdminPostController;
import poly.edu.quanlynhahang.controller.AdminProductController;
import poly.edu.quanlynhahang.controller.AdminTableController;
import poly.edu.quanlynhahang.controller.CategoryController;
import poly.edu.quanlynhahang.controller.RestaurantTableController;
import poly.edu.quanlynhahang.controller.VoucherController;
import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Post;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.Voucher;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityRequestIsolationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writeEndpointsNeverAcceptJpaEntitiesAsRequestBodies() throws Exception {
        assertRequestType(CategoryController.class.getMethod("addCategory", CategoryUpsertRequest.class), Category.class);
        assertRequestType(CategoryController.class.getMethod(
                "updateCategory", Integer.class, CategoryUpsertRequest.class), Category.class);
        assertRequestType(AdminProductController.class.getMethod("addProduct", ProductUpsertRequest.class), Product.class);
        assertRequestType(AdminProductController.class.getMethod(
                "updateProduct", Integer.class, ProductUpsertRequest.class), Product.class);
        assertRequestType(AdminPostController.class.getMethod("createPost", PostUpsertRequest.class), Post.class);
        assertRequestType(AdminPostController.class.getMethod(
                "updatePost", Integer.class, PostUpsertRequest.class), Post.class);
        assertRequestType(RestaurantTableController.class.getMethod(
                "addTable", RestaurantTableUpsertRequest.class), RestaurantTable.class);
        assertRequestType(AdminTableController.class.getMethod(
                "updateTable", Integer.class, RestaurantTableUpsertRequest.class), RestaurantTable.class);
        assertRequestType(VoucherController.class.getMethod("adminCreateVoucher", VoucherUpsertRequest.class), Voucher.class);
    }

    @Test
    void maliciousFieldsAreOutsideWhitelistedProductAndTableContracts() throws Exception {
        ProductUpsertRequest product = objectMapper.readValue("""
                {
                  "id": 99,
                  "name": "Com sen",
                  "price": 120000,
                  "costPrice": 1,
                  "averageRating": 5,
                  "category": {"id": 3}
                }
                """, ProductUpsertRequest.class);
        RestaurantTableUpsertRequest table = objectMapper.readValue("""
                {
                  "id": 88,
                  "name": "Ban VIP",
                  "isOccupied": 5,
                  "reservedTime": "attacker-controlled",
                  "capacity": 6
                }
                """, RestaurantTableUpsertRequest.class);

        assertEquals("Com sen", product.name());
        assertFalse(componentNames(ProductUpsertRequest.class).contains("costPrice"));
        assertFalse(componentNames(ProductUpsertRequest.class).contains("averageRating"));
        assertFalse(componentNames(RestaurantTableUpsertRequest.class).contains("isOccupied"));
        assertFalse(componentNames(RestaurantTableUpsertRequest.class).contains("reservedTime"));
        assertEquals(0, table.toNewEntity().getIsOccupied());
        assertEquals(null, table.toNewEntity().getReservedTime());
    }

    private void assertRequestType(Method method, Class<?> entityType) {
        assertTrue(Stream.of(method.getParameterTypes()).anyMatch(this::isUpsertRequest));
        assertFalse(Stream.of(method.getParameterTypes()).anyMatch(entityType::equals));
        assertNotEquals(entityType, method.getParameterTypes()[method.getParameterCount() - 1]);
    }

    private boolean isUpsertRequest(Class<?> type) {
        return type.getSimpleName().endsWith("UpsertRequest");
    }

    private Set<String> componentNames(Class<?> recordType) {
        return Stream.of(recordType.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
    }
}
