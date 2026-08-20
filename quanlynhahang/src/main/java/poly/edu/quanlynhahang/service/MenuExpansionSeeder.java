package poly.edu.quanlynhahang.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("demo")
@Order(20)
public class MenuExpansionSeeder implements CommandLineRunner {

    private static final String MENU_EXPANSION_RESOURCE = "data/menu-expansion.json";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public MenuExpansionSeeder(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        ClassPathResource resource = new ClassPathResource(MENU_EXPANSION_RESOURCE);
        if (!resource.exists()) {
            return;
        }

        ensureProductCostPriceColumn();

        List<MenuExpansionItem> items;
        try (InputStream inputStream = resource.getInputStream()) {
            items = Arrays.asList(objectMapper.readValue(inputStream, MenuExpansionItem[].class));
        }

        List<Product> newProducts = new ArrayList<>();
        List<Product> repairedProducts = new ArrayList<>();
        for (MenuExpansionItem item : items) {
            Product existingProduct = productRepository.findByNameIgnoreCase(item.name()).orElse(null);
            if (existingProduct != null) {
                boolean changed = false;
                if (hasBrokenImage(existingProduct.getImage())) {
                    existingProduct.setImage(item.image());
                    changed = true;
                }
                if ((existingProduct.getDescription() == null || existingProduct.getDescription().isBlank())
                        && item.description() != null && !item.description().isBlank()) {
                    existingProduct.setDescription(item.description());
                    changed = true;
                }
                if (existingProduct.getCostPrice() == null || existingProduct.getCostPrice().signum() <= 0) {
                    existingProduct.setCostPrice(item.costPrice());
                    changed = true;
                }
                if (changed) {
                    repairedProducts.add(existingProduct);
                }
                continue;
            }

            List<Category> matchingCategories = categoryRepository.findByNameIgnoreCaseOrderByIdAsc(item.category());
            Category category = matchingCategories.isEmpty()
                    ? null
                    : matchingCategories.getFirst();
            if (category == null) {
                        Category newCategory = new Category();
                        newCategory.setName(item.category());
                category = categoryRepository.save(newCategory);
            }

            Product product = new Product();
            product.setName(item.name());
            product.setPrice(item.price());
            product.setCostPrice(item.costPrice());
        product.setTaxRate(new BigDecimal("8.00"));
            product.setImage(item.image());
            product.setDescription(item.description());
            product.setAvailable(true);
            product.setStatus(true);
            product.setCategory(category);
            newProducts.add(product);
        }

        if (!newProducts.isEmpty()) {
            productRepository.saveAll(newProducts);
            System.out.println(">> Da bo sung " + newProducts.size() + " mon an demo tu " + MENU_EXPANSION_RESOURCE);
        }

        if (!repairedProducts.isEmpty()) {
            productRepository.saveAll(repairedProducts);
            System.out.println(">> Da cap nhat anh/mo ta/gia von cho " + repairedProducts.size() + " mon an demo da ton tai.");
        }
    }

    private boolean hasBrokenImage(String image) {
        if (image == null || image.isBlank()) {
            return true;
        }
        String normalized = image.trim().toLowerCase();
        return normalized.contains("placeholder")
                || normalized.contains("placehold.co")
                || normalized.contains("via.placeholder")
                || normalized.contains("example.com")
                || normalized.startsWith("/images/")
                || normalized.startsWith("file:")
                || (!normalized.startsWith("http://") && !normalized.startsWith("https://"));
    }

    private void ensureProductCostPriceColumn() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'dbo'
                  AND TABLE_NAME = 'Products'
                  AND COLUMN_NAME = 'cost_price'
                """, Integer.class);

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE dbo.Products
                    ADD cost_price DECIMAL(18,2) NOT NULL
                        CONSTRAINT DF_Products_cost_price DEFAULT (0)
                    """);
            System.out.println(">> Da them cot dbo.Products.cost_price de ho tro du lieu gia von.");
        }
    }

    private record MenuExpansionItem(
            String category,
            String name,
            BigDecimal price,
            BigDecimal costPrice,
            String image,
            String description
    ) {
    }
}
