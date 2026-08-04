package poly.edu.quanlynhahang.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.DepositPolicy;
import poly.edu.quanlynhahang.entity.Post;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.DepositPolicyRepository;
import poly.edu.quanlynhahang.repository.PostRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Profile("repair")
@Order(45)
public class TextEncodingRepairSeeder implements CommandLineRunner {
    private final TableAreaRepository tableAreaRepository;
    private final RestaurantTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final AccountRepository accountRepository;
    private final DepositPolicyRepository depositPolicyRepository;

    public TextEncodingRepairSeeder(TableAreaRepository tableAreaRepository,
                                    RestaurantTableRepository tableRepository,
                                    ProductRepository productRepository,
                                    CategoryRepository categoryRepository,
                                    PostRepository postRepository,
                                    AccountRepository accountRepository,
                                    DepositPolicyRepository depositPolicyRepository) {
        this.tableAreaRepository = tableAreaRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.accountRepository = accountRepository;
        this.depositPolicyRepository = depositPolicyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int fixed = 0;
        fixed += repairAreas();
        fixed += repairTables();
        fixed += repairCategories();
        fixed += repairProducts();
        fixed += repairPosts();
        fixed += repairDemoAccounts();
        fixed += repairDepositPolicies();

        if (fixed > 0) {
            System.out.println("TextEncodingRepairSeeder repaired " + fixed + " records with mojibake text.");
        }
    }

    private int repairAreas() {
        List<TableArea> areas = tableAreaRepository.findAll();
        int fixed = 0;
        for (TableArea area : areas) {
            boolean changed = false;
            changed |= setFixedNameVi(area);
            String nameEn = fixMojibake(area.getNameEn());
            if (!Objects.equals(nameEn, area.getNameEn())) {
                area.setNameEn(nameEn);
                changed = true;
            }
            String descriptionVi = fixMojibake(area.getDescriptionVi());
            if (!Objects.equals(descriptionVi, area.getDescriptionVi())) {
                area.setDescriptionVi(descriptionVi);
                changed = true;
            }
            String descriptionEn = fixMojibake(area.getDescriptionEn());
            if (!Objects.equals(descriptionEn, area.getDescriptionEn())) {
                area.setDescriptionEn(descriptionEn);
                changed = true;
            }
            if (changed) {
                area.setUpdatedAt(new Date());
                fixed++;
            }
        }
        if (fixed > 0) {
            tableAreaRepository.saveAll(areas);
        }
        return fixed;
    }

    private boolean setFixedNameVi(TableArea area) {
        String fixed = fixMojibake(area.getNameVi());
        if (!Objects.equals(fixed, area.getNameVi())) {
            area.setNameVi(fixed);
            return true;
        }
        return false;
    }

    private int repairTables() {
        List<RestaurantTable> tables = tableRepository.findAll();
        int fixed = 0;
        for (RestaurantTable table : tables) {
            boolean changed = false;
            String name = fixMojibake(table.getName());
            if (!Objects.equals(name, table.getName())) {
                table.setName(name);
                changed = true;
            }
            String floor = fixMojibake(table.getFloor());
            if (!Objects.equals(floor, table.getFloor())) {
                table.setFloor(floor);
                changed = true;
            }
            String viewType = fixMojibake(table.getViewType());
            if (!Objects.equals(viewType, table.getViewType())) {
                table.setViewType(viewType);
                changed = true;
            }
            String positionDescription = fixMojibake(table.getPositionDescription());
            if (!Objects.equals(positionDescription, table.getPositionDescription())) {
                table.setPositionDescription(positionDescription);
                changed = true;
            }
            if (changed) {
                fixed++;
            }
        }
        if (fixed > 0) {
            tableRepository.saveAll(tables);
        }
        return fixed;
    }

    private int repairCategories() {
        List<Category> categories = categoryRepository.findAll();
        int fixed = 0;
        for (Category category : categories) {
            String name = fixMojibake(category.getName());
            if (!Objects.equals(name, category.getName())) {
                category.setName(name);
                fixed++;
            }
        }
        if (fixed > 0) {
            categoryRepository.saveAll(categories);
        }
        return fixed;
    }

    private int repairProducts() {
        List<Product> products = productRepository.findAll();
        int fixed = 0;
        for (Product product : products) {
            boolean changed = false;
            String name = fixMojibake(product.getName());
            if (!Objects.equals(name, product.getName())) {
                product.setName(name);
                changed = true;
            }
            String nameVi = fixMojibake(product.getNameVi());
            if (!Objects.equals(nameVi, product.getNameVi())) {
                product.setNameVi(nameVi);
                changed = true;
            }
            String nameEn = fixMojibake(product.getNameEn());
            if (!Objects.equals(nameEn, product.getNameEn())) {
                product.setNameEn(nameEn);
                changed = true;
            }
            String description = fixMojibake(product.getDescription());
            if (!Objects.equals(description, product.getDescription())) {
                product.setDescription(description);
                changed = true;
            }
            String descriptionVi = fixMojibake(product.getDescriptionVi());
            if (!Objects.equals(descriptionVi, product.getDescriptionVi())) {
                product.setDescriptionVi(descriptionVi);
                changed = true;
            }
            String descriptionEn = fixMojibake(product.getDescriptionEn());
            if (!Objects.equals(descriptionEn, product.getDescriptionEn())) {
                product.setDescriptionEn(descriptionEn);
                changed = true;
            }
            if (changed) {
                fixed++;
            }
        }
        if (fixed > 0) {
            productRepository.saveAll(products);
        }
        return fixed;
    }

    private int repairPosts() {
        List<Post> posts = postRepository.findAll();
        int fixed = 0;
        for (Post post : posts) {
            boolean changed = false;
            String title = fixMojibake(post.getTitle());
            if (!Objects.equals(title, post.getTitle())) {
                post.setTitle(title);
                changed = true;
            }
            String content = fixMojibake(post.getContent());
            if (!Objects.equals(content, post.getContent())) {
                post.setContent(content);
                changed = true;
            }
            if (changed) {
                fixed++;
            }
        }
        if (fixed > 0) {
            postRepository.saveAll(posts);
        }
        return fixed;
    }

    private int repairDemoAccounts() {
        Map<String, DemoAccountText> expectedValues = Map.of(
                "admin", new DemoAccountText("Quản trị hệ thống", "Ca sáng", "Toàn bộ"),
                "manager", new DemoAccountText("Quản lý nhà hàng", "Ca sáng", "Toàn bộ"),
                "waiter", new DemoAccountText("Nhân viên phục vụ", "Ca sáng", "Sảnh"),
                "kitchen", new DemoAccountText("Nhân viên bếp", "Ca sáng", "Bếp"),
                "cashier", new DemoAccountText("Nhân viên thu ngân", "Ca sáng", "Quầy thu ngân")
        );

        List<Account> changedAccounts = new java.util.ArrayList<>();
        expectedValues.forEach((username, expected) -> accountRepository.findById(username).ifPresent(account -> {
            boolean changed = false;
            if (isDamagedDemoText(account.getFullname())) {
                account.setFullname(expected.fullname());
                changed = true;
            }
            if (isDamagedDemoText(account.getShift())) {
                account.setShift(expected.shift());
                changed = true;
            }
            if (isDamagedDemoText(account.getAssignedArea())) {
                account.setAssignedArea(expected.assignedArea());
                changed = true;
            }
            if (changed) {
                changedAccounts.add(account);
            }
        }));

        if (!changedAccounts.isEmpty()) {
            accountRepository.saveAll(changedAccounts);
        }
        return changedAccounts.size();
    }

    private int repairDepositPolicies() {
        List<DepositPolicy> policies = depositPolicyRepository.findAll();
        int fixed = 0;
        for (DepositPolicy policy : policies) {
            boolean changed = false;
            String nameVi = fixMojibake(policy.getNameVi());
            if (!Objects.equals(nameVi, policy.getNameVi())) {
                policy.setNameVi(nameVi);
                changed = true;
            }
            String nameEn = fixMojibake(policy.getNameEn());
            if (!Objects.equals(nameEn, policy.getNameEn())) {
                policy.setNameEn(nameEn);
                changed = true;
            }
            if (changed) {
                policy.setUpdatedAt(new Date());
                fixed++;
            }
        }
        if (fixed > 0) {
            depositPolicyRepository.saveAll(policies);
        }
        return fixed;
    }

    private boolean isDamagedDemoText(String value) {
        return value == null || value.isBlank() || value.indexOf('?') >= 0 || looksMojibake(value);
    }

    private record DemoAccountText(String fullname, String shift, String assignedArea) {
    }

    private String fixMojibake(String value) {
        if (value == null || value.isBlank() || !looksMojibake(value)) {
            return value;
        }

        String best = value;
        int bestScore = mojibakeScore(value);
        String current = value;

        for (int i = 0; i < 3; i++) {
            String candidate = bestDecodedCandidate(current, bestScore);
            if (candidate == null) {
                break;
            }
            best = candidate;
            bestScore = mojibakeScore(candidate);
            current = candidate;
        }
        return best;
    }

    private String bestDecodedCandidate(String value, int currentScore) {
        String bestCandidate = null;
        int bestCandidateScore = currentScore;

        byte[][] encodings = {toWindows1252Bytes(value), toWindows1258Bytes(value)};
        for (byte[] bytes : encodings) {
            if (bytes == null) {
                continue;
            }
            String decoded = decodeUtf8Strict(bytes);
            if (decoded == null) {
                continue;
            }
            int decodedScore = mojibakeScore(decoded);
            if (decodedScore < bestCandidateScore) {
                bestCandidate = decoded;
                bestCandidateScore = decodedScore;
            }
        }
        return bestCandidate;
    }

    private boolean looksMojibake(String value) {
        return mojibakeScore(value) > 0;
    }

    private int mojibakeScore(String value) {
        int score = 0;
        String[] markers = {"Ã", "Â", "Ä", "Æ", "áº", "á»", "â€", "ðŸ"};
        for (String marker : markers) {
            score += countOccurrences(value, marker);
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == 0x0102 && i + 1 < value.length() && value.charAt(i + 1) >= 0x00A0) {
                score += 2;
            }
            if (ch >= 0x80 && ch <= 0x9F) {
                score += 4;
            }
        }
        return score;
    }

    private int countOccurrences(String value, String marker) {
        int count = 0;
        int index = value.indexOf(marker);
        while (index >= 0) {
            count++;
            index = value.indexOf(marker, index + marker.length());
        }
        return count;
    }

    private String decodeUtf8Strict(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] toWindows1252Bytes(String value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            Integer mapped = windows1252Byte(value.charAt(i));
            if (mapped == null) {
                return null;
            }
            output.write(mapped);
        }
        return output.toByteArray();
    }

    private byte[] toWindows1258Bytes(String value) {
        Charset windows1258 = Charset.forName("windows-1258");
        byte[] bytes = value.getBytes(windows1258);
        String roundTripped = new String(bytes, windows1258);
        return roundTripped.equals(value) ? bytes : null;
    }

    private Integer windows1252Byte(char ch) {
        if (ch <= 0xFF) {
            return (int) ch;
        }
        return switch (ch) {
            case '€' -> 0x80;
            case '‚' -> 0x82;
            case 'ƒ' -> 0x83;
            case '„' -> 0x84;
            case '…' -> 0x85;
            case '†' -> 0x86;
            case '‡' -> 0x87;
            case 'ˆ' -> 0x88;
            case '‰' -> 0x89;
            case 'Š' -> 0x8A;
            case '‹' -> 0x8B;
            case 'Œ' -> 0x8C;
            case 'Ž' -> 0x8E;
            case '‘' -> 0x91;
            case '’' -> 0x92;
            case '“' -> 0x93;
            case '”' -> 0x94;
            case '•' -> 0x95;
            case '–' -> 0x96;
            case '—' -> 0x97;
            case '˜' -> 0x98;
            case '™' -> 0x99;
            case 'š' -> 0x9A;
            case '›' -> 0x9B;
            case 'œ' -> 0x9C;
            case 'ž' -> 0x9E;
            case 'Ÿ' -> 0x9F;
            default -> null;
        };
    }
}
