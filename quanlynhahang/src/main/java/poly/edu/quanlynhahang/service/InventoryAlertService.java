package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@Service
public class InventoryAlertService {
    private static final int CONSUMPTION_WINDOW_DAYS = 7;
    private static final int FORECAST_DAYS = 7;
    private static final int SHORTAGE_WARNING_DAYS = 5;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final OrderRepository orderRepository;
    private final RecipeRepository recipeRepository;

    public InventoryAlertService(IngredientRepository ingredientRepository,
                                 IngredientBatchRepository ingredientBatchRepository,
                                 OrderRepository orderRepository,
                                 RecipeRepository recipeRepository) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.orderRepository = orderRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public Analysis analyze(int expiringDays) {
        Date now = new Date();
        Date expiringAt = Date.from(now.toInstant().plusSeconds(Math.max(1, expiringDays) * 86_400L));
        List<Ingredient> ingredients = ingredientRepository.findAll();
        Map<Long, List<IngredientBatch>> batchesByIngredient = new HashMap<>();
        for (IngredientBatch batch : ingredientBatchRepository.findPositiveBatchesWithIngredient()) {
            if (batch.getIngredient() != null && batch.getIngredient().getId() != null) {
                batchesByIngredient.computeIfAbsent(batch.getIngredient().getId(), ignored -> new ArrayList<>())
                        .add(batch);
            }
        }

        Map<Long, BigDecimal> consumption = calculateConsumption(now);
        List<Item> alerts = new ArrayList<>();
        long lowStock = 0;
        long outOfStock = 0;
        long expiredBatchCount = 0;
        long expiringBatchCount = 0;

        for (Ingredient ingredient : ingredients) {
            List<Batch> expired = new ArrayList<>();
            List<Batch> expiring = new ArrayList<>();
            BigDecimal usableStock = BigDecimal.ZERO;
            for (IngredientBatch batch : batchesByIngredient.getOrDefault(ingredient.getId(), List.of())) {
                BigDecimal quantity = zeroIfNull(batch.getQuantity());
                Date expiry = batch.getExpirationDate();
                if (expiry != null && expiry.before(now)) {
                    expired.add(toBatch(batch, now));
                    continue;
                }
                usableStock = usableStock.add(quantity);
                if (expiry != null && !expiry.after(expiringAt)) {
                    expiring.add(toBatch(batch, now));
                }
            }

            BigDecimal minStock = zeroIfNull(ingredient.getMinStock());
            BigDecimal dailyConsumption = consumption.getOrDefault(ingredient.getId(), BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(CONSUMPTION_WINDOW_DAYS), 4, RoundingMode.HALF_UP);
            double daysLeft = calculateDaysLeft(usableStock, dailyConsumption);
            boolean isOut = usableStock.signum() <= 0;
            boolean isLow = usableStock.compareTo(minStock) <= 0;
            boolean expiryRisk = !expired.isEmpty() || !expiring.isEmpty();
            boolean purchaseRisk = isOut || isLow || daysLeft <= SHORTAGE_WARNING_DAYS;

            if (isOut) outOfStock++;
            if (isLow) lowStock++;
            expiredBatchCount += expired.size();
            expiringBatchCount += expiring.size();

            if (!expiryRisk && !purchaseRisk) continue;

            BigDecimal suggestedAmount = expiryRisk
                    ? BigDecimal.ZERO
                    : minStock.multiply(BigDecimal.valueOf(2))
                            .max(dailyConsumption.multiply(BigDecimal.valueOf(FORECAST_DAYS)))
                            .subtract(usableStock)
                            .max(BigDecimal.ZERO)
                            .setScale(1, RoundingMode.HALF_UP);
            BigDecimal estimatedCost = suggestedAmount.multiply(zeroIfNull(ingredient.getUnitPrice()))
                    .setScale(0, RoundingMode.HALF_UP);
            AlertText alertText = describe(expired, expiring, isOut, isLow, daysLeft, suggestedAmount);
            alerts.add(new Item(
                    ingredient.getId(), ingredient.getName(), ingredient.getUnit(), ingredient.getImage(),
                    usableStock, minStock, dailyConsumption.setScale(2, RoundingMode.HALF_UP),
                    roundOneDecimal(daysLeft), suggestedAmount, estimatedCost,
                    alertText.urgency(), alertText.label(), alertText.reason(), alertText.action(),
                    suggestedAmount.signum() > 0, expired, expiring));
        }

        Map<String, Integer> priority = Map.of("expired", 0, "expiring", 1, "critical", 2, "warning", 3, "info", 4);
        alerts.sort((left, right) -> Integer.compare(
                priority.getOrDefault(left.urgency(), 9), priority.getOrDefault(right.urgency(), 9)));

        long purchaseCount = alerts.stream().filter(Item::needsPurchase).count();
        long handlingCount = alerts.size() - purchaseCount;
        BigDecimal totalEstimatedCost = alerts.stream().map(Item::estimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long criticalCount = outOfStock;
        long warningCount = Math.max(0, lowStock - outOfStock);

        return new Analysis(alerts, ingredients.size(), lowStock, outOfStock,
                expiredBatchCount, expiringBatchCount, purchaseCount, handlingCount,
                criticalCount, warningCount, totalEstimatedCost);
    }

    private Map<Long, BigDecimal> calculateConsumption(Date now) {
        Date start = new Date(now.getTime() - CONSUMPTION_WINDOW_DAYS * 86_400_000L);
        List<Order> recentOrders = orderRepository.findByStatusSinceWithDetails(OrderStatus.COMPLETED.code(), start);
        List<Integer> productIds = recentOrders.stream()
                .filter(order -> order.getOrderDetails() != null)
                .flatMap(order -> order.getOrderDetails().stream())
                .filter(detail -> detail.getProduct() != null && detail.getProduct().getId() != null)
                .map(detail -> detail.getProduct().getId()).distinct().toList();
        List<Recipe> recipes = productIds.isEmpty() ? List.of()
                : recipeRepository.findByProductIdsWithIngredient(productIds);
        Map<Integer, List<Recipe>> recipeMap = new HashMap<>();
        for (Recipe recipe : recipes) {
            if (recipe.getProduct() != null && recipe.getProduct().getId() != null) {
                recipeMap.computeIfAbsent(recipe.getProduct().getId(), ignored -> new ArrayList<>()).add(recipe);
            }
        }
        Map<Long, BigDecimal> consumption = new HashMap<>();
        for (Order order : recentOrders) {
            if (order.getOrderDetails() == null) continue;
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct() == null || detail.getProduct().getId() == null) continue;
                BigDecimal orderQuantity = BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity());
                for (Recipe recipe : recipeMap.getOrDefault(detail.getProduct().getId(), List.of())) {
                    if (recipe.getIngredient() == null || recipe.getIngredient().getId() == null) continue;
                    BigDecimal used = zeroIfNull(recipe.getAmountRequired()).multiply(orderQuantity);
                    consumption.merge(recipe.getIngredient().getId(), used, BigDecimal::add);
                }
            }
        }
        return consumption;
    }

    private Batch toBatch(IngredientBatch batch, Date now) {
        long daysRemaining = batch.getExpirationDate() == null ? Long.MAX_VALUE
                : ChronoUnit.DAYS.between(LocalDate.now(BUSINESS_ZONE),
                        batch.getExpirationDate().toInstant().atZone(BUSINESS_ZONE).toLocalDate());
        return new Batch(batch.getId(), zeroIfNull(batch.getQuantity()), batch.getImportDate(),
                batch.getExpirationDate(), daysRemaining);
    }

    private AlertText describe(List<Batch> expired, List<Batch> expiring, boolean outOfStock,
                               boolean lowStock, double daysLeft, BigDecimal suggestedAmount) {
        if (!expired.isEmpty()) {
            return new AlertText("expired", "Có lô hết hạn",
                    expired.size() + " lô đã hết hạn, không được tính vào tồn dùng được.",
                    "Cách ly/loại bỏ lô hết hạn; ưu tiên các lô còn hạn và tạm không nhập thêm.");
        }
        if (!expiring.isEmpty()) {
            long nearest = expiring.stream().mapToLong(Batch::daysRemaining).min().orElse(0);
            return new AlertText("expiring", "Sắp hết hạn",
                    expiring.size() + " lô sẽ hết hạn, gần nhất còn " + Math.max(0, nearest) + " ngày.",
                    "Ưu tiên sử dụng lô sắp hết hạn và tạm không nhập thêm.");
        }
        if (outOfStock) {
            return new AlertText("critical", "Hết hàng", "Không còn tồn kho dùng được.",
                    "Nhập " + suggestedAmount + " theo định mức và tốc độ tiêu thụ.");
        }
        if (lowStock) {
            return new AlertText("warning", "Tồn thấp", "Tồn dùng được đã chạm mức tối thiểu.",
                    "Nhập " + suggestedAmount + " theo định mức và tốc độ tiêu thụ.");
        }
        return new AlertText("info", "Sắp thiếu (" + Math.round(daysLeft) + " ngày)",
                "Với tốc độ tiêu thụ hiện tại, tồn kho chỉ còn khoảng " + roundOneDecimal(daysLeft) + " ngày.",
                "Nhập " + suggestedAmount + " để đủ dùng cho giai đoạn dự báo.");
    }

    private double calculateDaysLeft(BigDecimal stock, BigDecimal dailyConsumption) {
        if (stock.signum() <= 0) return 0;
        if (dailyConsumption.signum() <= 0) return 999;
        return stock.divide(dailyConsumption, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private double roundOneDecimal(double value) {
        return value >= 999 ? 999 : Math.round(value * 10.0) / 10.0;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record AlertText(String urgency, String label, String reason, String action) {}

    public record Batch(Long batchId, BigDecimal quantity, Date importDate, Date expiryDate, long daysRemaining) {}

    public record Item(Long ingredientId, String name, String unit, String image,
                       BigDecimal currentStock, BigDecimal minStock, BigDecimal dailyConsumption,
                       double daysLeft, BigDecimal suggestedAmount, BigDecimal estimatedCost,
                       String urgency, String urgencyLabel, String reason, String action,
                       boolean needsPurchase, List<Batch> expiredBatches, List<Batch> expiringBatches) {}

    public record Analysis(List<Item> suggestions, long total, long lowStock, long outOfStock,
                           long expiredBatchesCount, long expiringBatchesCount,
                           long totalItems, long handlingCount, long criticalCount, long warningCount,
                           BigDecimal totalEstimatedCost) {
        public String toAiContext() {
            StringBuilder result = new StringBuilder("INVENTORY_CANONICAL_ANALYSIS\n")
                    .append("expiredBatches=").append(expiredBatchesCount)
                    .append(", expiringBatches=").append(expiringBatchesCount)
                    .append(", lowStockIngredients=").append(lowStock)
                    .append(", outOfStockIngredients=").append(outOfStock)
                    .append(", purchaseItems=").append(totalItems)
                    .append(", handlingItems=").append(handlingCount).append('\n');
            suggestions.stream().limit(100).forEach(item -> result
                    .append("- ").append(item.name()).append(": stock=").append(item.currentStock()).append(item.unit())
                    .append(", consumption/day=").append(item.dailyConsumption()).append(item.unit())
                    .append(", daysLeft=").append(item.daysLeft())
                    .append(", suggestedPurchase=").append(item.suggestedAmount()).append(item.unit())
                    .append(", urgency=").append(item.urgencyLabel())
                    .append(", expiredBatches=").append(item.expiredBatches().size())
                    .append(", expiringBatches=").append(item.expiringBatches().size())
                    .append(", reason=").append(item.reason())
                    .append(", action=").append(item.action()).append('\n'));
            return result.toString();
        }
    }
}
