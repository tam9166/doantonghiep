package poly.edu.quanlynhahang.service;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;

@Service
public class StaffOperationsAssistantService {
    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final ProductRepository productRepository;

    public StaffOperationsAssistantService(IngredientRepository ingredientRepository,
                                           IngredientBatchRepository ingredientBatchRepository,
                                           ProductRepository productRepository) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.productRepository = productRepository;
    }

    public String answer(String message) {
        String query = normalize(message);
        if (query.contains("han") || query.contains("su dung") || query.contains("het han")) {
            return shelfLifeReply(query);
        }
        if (query.contains("mon") || query.contains("thuc don") || query.contains("kha dung")
                || query.contains("con hang") || query.contains("phuc vu")) {
            return availableProductsReply(query);
        }
        return inventoryReply(query);
    }

    private String inventoryReply(String query) {
        List<Ingredient> matches = matchingIngredients(query);
        if (!matches.isEmpty()) {
            return formatIngredients(matches, false);
        }
        List<Ingredient> lowStock = ingredientRepository.findAll().stream()
                .filter(this::isLowStock)
                .sorted(Comparator.comparing(Ingredient::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(8)
                .toList();
        if (lowStock.isEmpty()) {
            return "Tồn kho hiện không có nguyên liệu nào dưới mức tối thiểu.";
        }
        return "Nguyên liệu sắp hết: " + formatIngredients(lowStock, false);
    }

    private String shelfLifeReply(String query) {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, 7);

        List<IngredientBatch> batches = ingredientBatchRepository.findExpiringBatches(calendar.getTime()).stream()
                .filter(batch -> batch.getExpirationDate() != null)
                .filter(batch -> isGenericShelfLifeQuery(query)
                        || matches(query, batch.getIngredient() == null ? null : batch.getIngredient().getName()))
                .limit(8)
                .toList();
        if (batches.isEmpty()) {
            return "Không có lô nguyên liệu nào hết hạn hoặc sắp hết hạn trong 7 ngày tới.";
        }
        String reply = batches.stream().map(batch -> formatBatchExpiry(batch, now))
                .reduce((left, right) -> left + "; " + right).orElse("");
        return "Lô nguyên liệu cần ưu tiên xử lý: " + reply + ".";
    }

    private boolean isGenericShelfLifeQuery(String query) {
        return query.matches(".*(han|su dung|het han|sap het).*" );
    }

    private String formatBatchExpiry(IngredientBatch batch, Date now) {
        String name = batch.getIngredient() == null || batch.getIngredient().getName() == null
                ? "Nguyên liệu chưa xác định" : batch.getIngredient().getName();
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(batch.getExpirationDate());
        String status = batch.getExpirationDate().before(now) ? "đã hết hạn" : "hết hạn";
        String quantity = String.format(Locale.ROOT, "%.2f", batch.getQuantity() == null ? 0D : batch.getQuantity());
        String unit = batch.getIngredient() == null || batch.getIngredient().getUnit() == null
                ? "" : " " + batch.getIngredient().getUnit();
        return name + " (" + quantity + unit + ", " + status + " " + date + ")";
    }

    private String availableProductsReply(String query) {
        List<Product> available = productRepository.findAll().stream()
                .filter(product -> Boolean.TRUE.equals(product.getStatus()) && Boolean.TRUE.equals(product.getAvailable()))
                .filter(product -> isGenericMenuQuery(query)
                        || matches(query, product.getName(), product.getNameVi(), product.getNameEn()))
                .sorted(Comparator.comparing(Product::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(12)
                .toList();
        if (available.isEmpty()) {
            return "Không tìm thấy món đang khả dụng phù hợp. Hãy thử nhập tên món khác.";
        }
        String names = available.stream().map(Product::getName).reduce((left, right) -> left + ", " + right).orElse("");
        return "Món đang khả dụng (" + available.size() + "): " + names + ".";
    }

    private List<Ingredient> matchingIngredients(String query) {
        return ingredientRepository.findAll().stream()
                .filter(ingredient -> matches(query, ingredient.getName()))
                .limit(8)
                .toList();
    }

    private boolean matches(String query, String... values) {
        String[] words = query.replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+");
        for (String value : values) {
            String candidate = normalize(value);
            for (String word : words) {
                if (word.length() >= 3 && candidate.contains(word)) return true;
            }
        }
        return false;
    }

    private boolean isGenericMenuQuery(String query) {
        return query.matches(".*(mon|thuc don|kha dung|con hang|phuc vu).*" );
    }

    private String formatIngredients(List<Ingredient> ingredients, boolean includeShelfLife) {
        return ingredients.stream().map(ingredient -> {
            String quantity = String.format(Locale.ROOT, "%.2f", ingredient.getQuantity() == null ? 0D : ingredient.getQuantity());
            String text = ingredient.getName() + " " + quantity + " "
                    + (ingredient.getUnit() == null ? "" : ingredient.getUnit());
            return includeShelfLife ? text + " (" + (ingredient.getShelfLifeDays() == null ? "chưa cấu hình" : ingredient.getShelfLifeDays() + " ngày") + ")" : text;
        }).reduce((left, right) -> left + "; " + right).orElse("");
    }

    private boolean isLowStock(Ingredient ingredient) {
        return ingredient.getQuantity() != null && ingredient.getMinStock() != null
                && ingredient.getQuantity() <= ingredient.getMinStock();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
