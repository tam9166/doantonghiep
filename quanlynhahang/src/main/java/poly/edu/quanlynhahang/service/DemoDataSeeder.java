package poly.edu.quanlynhahang.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Post;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PostRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("demo")
@org.springframework.core.annotation.Order(40)
public class DemoDataSeeder implements CommandLineRunner {
    private static final String DEMO_ORDER_PREFIX = "DEMO_TEST_DATA";
    private static final String DEFAULT_FOOD_IMAGE = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80";

    private static final Map<String, String> CATEGORY_IMAGES = Map.ofEntries(
            Map.entry("khai", "https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=600&q=80"),
            Map.entry("chinh", "https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=600&q=80"),
            Map.entry("lẩu", "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=600&q=80"),
            Map.entry("nướng", "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=600&q=80"),
            Map.entry("hải", "https://images.unsplash.com/photo-1559737558-2f5a35f4523b?auto=format&fit=crop&w=600&q=80"),
            Map.entry("tráng", "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=600&q=80"),
            Map.entry("uống", "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=80"),
            Map.entry("chay", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=80"),
            Map.entry("combo", "https://images.unsplash.com/photo-1555244162-803834f70033?auto=format&fit=crop&w=600&q=80")
    );

    private final ProductRepository productRepository;
    private final PostRepository postRepository;
    private final TableAreaRepository tableAreaRepository;
    private final RestaurantTableRepository tableRepository;
    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public DemoDataSeeder(ProductRepository productRepository,
                          PostRepository postRepository,
                          TableAreaRepository tableAreaRepository,
                          RestaurantTableRepository tableRepository,
                          VoucherRepository voucherRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository) {
        this.productRepository = productRepository;
        this.postRepository = postRepository;
        this.tableAreaRepository = tableAreaRepository;
        this.tableRepository = tableRepository;
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        repairProductImages();
        seedPosts();
        seedAreasAndTables();
        seedVouchers();
        seedStatisticsOrders();
    }

    private void repairProductImages() {
        List<Product> changed = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            if (hasBrokenImage(product.getImage())) {
                product.setImage(resolveFoodImage(product));
                changed.add(product);
            }
            if (product.getTaxRate() == null) {
                product.setTaxRate(8.0);
                if (!changed.contains(product)) {
                    changed.add(product);
                }
            }
            if (product.getAvailable() == null) {
                product.setAvailable(true);
                if (!changed.contains(product)) {
                    changed.add(product);
                }
            }
            if (product.getStatus() == null) {
                product.setStatus(true);
                if (!changed.contains(product)) {
                    changed.add(product);
                }
            }
        }
        if (!changed.isEmpty()) {
            productRepository.saveAll(changed);
            System.out.println(">> Da sua anh/trang thai cho " + changed.size() + " mon an demo.");
        }
    }

    private void seedPosts() {
        if (postRepository.count() > 0) {
            return;
        }

        postRepository.saveAll(List.of(
                post("Khai trương không gian Rêu Núi & Hoàng Đồng",
                        "Nhà hàng Mộc Vị làm mới không gian với tông rêu núi, hoàng đồng và thực đơn món Việt theo mùa.",
                        "NEWS",
                        "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=900&q=80"),
                post("Ưu đãi combo gia đình cuối tuần",
                        "Combo 4-6 người được chuẩn bị cho nhóm gia đình, có món khai vị, món chính, lẩu và tráng miệng.",
                        "NEWS",
                        "https://images.unsplash.com/photo-1555244162-803834f70033?auto=format&fit=crop&w=900&q=80"),
                post("Món mới: Lẩu riêu cua bắp bò",
                        "Nước lẩu riêu cua chua thanh kết hợp bắp bò, rau đồng và bún tươi.",
                        "NEWS",
                        "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=900&q=80"),
                post("Tuyển nhân viên phục vụ ca tối",
                        "Ưu tiên ứng viên nhanh nhẹn, giao tiếp tốt, có thể làm việc cuối tuần. Có đào tạo quy trình phục vụ.",
                        "RECRUITMENT",
                        "https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=900&q=80"),
                post("Tuyển phụ bếp món Việt",
                        "Cần phụ bếp hỗ trợ sơ chế, ra món và giữ vệ sinh khu bếp. Có kinh nghiệm là lợi thế.",
                        "RECRUITMENT",
                        "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?auto=format&fit=crop&w=900&q=80")
        ));
        System.out.println(">> Da them bai viet demo cho trang chu.");
    }

    private void seedAreasAndTables() {
        List<TableArea> areas = tableAreaRepository.findAll();
        if (areas.isEmpty()) {
            areas = tableAreaRepository.saveAll(List.of(
                    area("Sảnh chính", "Main hall", "Không gian mở phù hợp dùng bữa hằng ngày.", "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=900&q=80", 0, 36),
                    area("Khu cửa sổ", "Window area", "Bàn cạnh cửa sổ, nhiều ánh sáng tự nhiên.", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&w=900&q=80", 50000, 20),
                    area("Phòng VIP", "Private room", "Không gian riêng cho họp mặt, sinh nhật và tiếp khách.", "https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=900&q=80", 150000, 24),
                    area("Sân vườn", "Garden", "Góc xanh ngoài trời cho nhóm gia đình.", "https://images.unsplash.com/photo-1544148103-0773bf10d330?auto=format&fit=crop&w=900&q=80", 80000, 24)
            ));
            System.out.println(">> Da them khu vuc ban demo.");
        }

        if (tableRepository.count() > 0) {
            return;
        }

        List<RestaurantTable> tables = new ArrayList<>();
        for (int i = 0; i < areas.size(); i++) {
            TableArea area = areas.get(i);
            tables.add(table("Bàn " + (i + 1) + "A", "Tầng 1", 4, area, false, false, "Bàn tiêu chuẩn cho 2-4 khách"));
            tables.add(table("Bàn " + (i + 1) + "B", i == 2 ? "VIP" : "Tầng 1", i == 2 ? 8 : 6, area, i == 1, i == 2, "Bàn nhóm phù hợp đặt trước"));
        }
        tableRepository.saveAll(tables);
        System.out.println(">> Da them " + tables.size() + " ban demo.");
    }

    private void seedVouchers() {
        seedVoucher("WELCOME10", 10);
        seedVoucher("FAMILY15", 15);
        seedVoucher("MOCVI20", 20);
    }

    private void seedStatisticsOrders() {
        boolean hasDemoOrders = orderRepository.findAll().stream()
                .anyMatch(order -> order.getAddress() != null && order.getAddress().startsWith(DEMO_ORDER_PREFIX));
        if (hasDemoOrders) {
            return;
        }

        List<Product> products = productRepository.findAll().stream()
                .filter(product -> Boolean.TRUE.equals(product.getStatus()) && Boolean.TRUE.equals(product.getAvailable()))
                .limit(10)
                .toList();
        if (products.isEmpty()) {
            return;
        }

        List<RestaurantTable> tables = tableRepository.findAll();
        if (tables.isEmpty()) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        for (int day = 6; day >= 0; day--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, -day);

            RestaurantTable table = tables.get(day % tables.size());
            poly.edu.quanlynhahang.entity.Order order = new poly.edu.quanlynhahang.entity.Order();
            order.setCreateDate(calendar.getTime());
            order.setAddress(DEMO_ORDER_PREFIX + " | " + table.getName() + " | Don thong ke ngay " + (7 - day));
            order.setTableId(table.getId());
            order.setStatus(4);
            order.setIsPaid(true);

            List<OrderDetail> details = new ArrayList<>();
            BigDecimal subTotal = BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;
            int itemCount = Math.min(3, products.size());
            for (int i = 0; i < itemCount; i++) {
                Product product = products.get((day + i) % products.size());
                int quantity = 1 + ((day + i) % 3);
                BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                double taxRate = product.getTaxRate() != null ? product.getTaxRate() : 8.0;
                BigDecimal lineSubtotal = price.multiply(BigDecimal.valueOf(quantity));
                BigDecimal lineTax = lineSubtotal.multiply(BigDecimal.valueOf(taxRate))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                OrderDetail detail = new OrderDetail();
                detail.setOrder(order);
                detail.setProduct(product);
                detail.setQuantity(quantity);
                detail.setPrice(lineSubtotal);
                detail.setTaxRate(taxRate);
                detail.setTaxAmount(lineTax);
                detail.setStatus(2);
                details.add(detail);

                subTotal = subTotal.add(lineSubtotal);
                taxAmount = taxAmount.add(lineTax);
            }

            order.setSubTotal(subTotal);
            order.setTaxAmount(taxAmount);
            order.setTotalAmount(subTotal.add(taxAmount));
            order.setDeposit(BigDecimal.ZERO);

            poly.edu.quanlynhahang.entity.Order savedOrder = orderRepository.save(order);
            details.forEach(detail -> detail.setOrder(savedOrder));
            orderDetailRepository.saveAll(details);
        }
        System.out.println(">> Da them don hang demo cho trang thong ke.");
    }

    private Post post(String title, String content, String type, String image) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setType(type);
        post.setImage(image);
        post.setLikes(0);
        post.setActive(true);
        post.setCreateDate(new Date());
        return post;
    }

    private TableArea area(String nameVi, String nameEn, String descriptionVi, String imageUrl, long basePrice, int capacity) {
        TableArea area = new TableArea();
        area.setNameVi(nameVi);
        area.setNameEn(nameEn);
        area.setDescriptionVi(descriptionVi);
        area.setDescriptionEn(descriptionVi);
        area.setImageUrl(imageUrl);
        area.setBasePrice(BigDecimal.valueOf(basePrice));
        area.setCapacity(capacity);
        area.setStatus("ACTIVE");
        area.setCreatedAt(new Date());
        area.setUpdatedAt(new Date());
        return area;
    }

    private RestaurantTable table(String name, String floor, int capacity, TableArea area, boolean windowSeat, boolean privateRoom, String description) {
        RestaurantTable table = new RestaurantTable();
        table.setName(name);
        table.setFloor(floor);
        table.setCapacity(capacity);
        table.setMinCapacity(1);
        table.setMaxCapacity(capacity);
        table.setSeatCount(capacity);
        table.setIsOccupied(0);
        table.setHasView(windowSeat);
        table.setViewType(windowSeat ? "WINDOW" : (privateRoom ? "PRIVATE" : "STANDARD"));
        table.setAreaId(area.getId());
        table.setReservationPrice(area.getBasePrice());
        table.setPositionDescription(description);
        table.setWindowSeat(windowSeat);
        table.setPrivateRoom(privateRoom);
        table.setChildFriendly(true);
        table.setActive(true);
        table.setImageUrl(area.getImageUrl());
        return table;
    }

    private void seedVoucher(String code, int discountPercent) {
        if (voucherRepository.findByCode(code).isPresent()) {
            return;
        }
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setDiscountPercent(discountPercent);
        voucher.setIsUsed(false);
        voucher.setCreateDate(new Date());
        voucherRepository.save(voucher);
    }

    private boolean hasBrokenImage(String image) {
        if (image == null || image.isBlank()) {
            return true;
        }
        String normalized = image.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("placeholder")
                || normalized.contains("placehold.co")
                || normalized.contains("via.placeholder")
                || normalized.contains("example.com")
                || normalized.startsWith("/images/")
                || normalized.startsWith("file:")
                || (!normalized.startsWith("http://") && !normalized.startsWith("https://"));
    }

    private String resolveFoodImage(Product product) {
        String name = product.getName() == null ? "" : product.getName().toLowerCase(Locale.ROOT);
        if (name.contains("phở") || name.contains("bún") || name.contains("mì")) {
            return "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=600&q=80";
        }
        if (name.contains("coca") || name.contains("trà") || name.contains("nước") || name.contains("soda") || name.contains("cà phê")) {
            return "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=80";
        }
        if (name.contains("cơm")) {
            return "https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=600&q=80";
        }

        String categoryName = product.getCategory() == null || product.getCategory().getName() == null
                ? ""
                : product.getCategory().getName().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : CATEGORY_IMAGES.entrySet()) {
            if (categoryName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_FOOD_IMAGE;
    }
}
