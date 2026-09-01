package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import poly.edu.quanlynhahang.dto.KitchenProposalRequest;
import poly.edu.quanlynhahang.dto.KitchenProposalReviewRequest;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.KitchenProposal;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.KitchenProposalRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@Service
@RequiredArgsConstructor
public class KitchenProposalService {
    private static final List<String> TYPES = List.of("INGREDIENT", "DISH", "RECIPE");
    private final KitchenProposalRepository proposalRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final ActivityLogService activityLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public KitchenProposal submit(KitchenProposalRequest request, Authentication authentication) {
        String type = request.proposalType().trim().toUpperCase();
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Loại đề xuất không hợp lệ");
        KitchenProposal proposal = new KitchenProposal();
        proposal.setProposalType(type);
        proposal.setPayload(request.payload().trim());
        proposal.setReason(request.reason().trim());
        proposal.setProposedBy(authentication.getName());
        proposal.setProposerRole(authentication.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_KITCHEN"));
        KitchenProposal saved = proposalRepository.save(proposal);
        activityLogService.log("SUBMIT_KITCHEN_PROPOSAL", "KitchenProposal", String.valueOf(saved.getId()), type + " proposal submitted");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<KitchenProposal> list(Authentication authentication, boolean all) {
        return all ? proposalRepository.findAllByOrderByCreatedAtDesc()
                : proposalRepository.findByProposedByOrderByCreatedAtDesc(authentication.getName());
    }

    @Transactional
    public KitchenProposal approve(Long id, KitchenProposalReviewRequest request, Authentication authentication) {
        KitchenProposal proposal = proposalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề xuất"));
        if (!"PENDING".equals(proposal.getStatus())) throw new IllegalStateException("Đề xuất đã được xử lý");
        Map<String, Object> payload = parsePayload(proposal.getPayload());
        String type = proposal.getProposalType();
        if ("INGREDIENT".equals(type)) {
            Ingredient ingredient = new Ingredient();
            ingredient.setName(required(payload, "name"));
            ingredient.setUnit(required(payload, "unit"));
            ingredient.setQuantity(decimal(payload.get("initialQuantity")));
            ingredient.setMinStock(decimalOr(payload.get("minStock"), new BigDecimal("5")));
            ingredient.setShelfLifeDays(integerOr(payload.get("shelfLifeDays"), 30));
            Ingredient saved = ingredientRepository.save(ingredient);
            proposal.setCreatedEntityType("Ingredient");
            proposal.setCreatedEntityId(String.valueOf(saved.getId()));
        } else {
            Product product = new Product();
            product.setName(required(payload, "name"));
            product.setDescription(required(payload, "description"));
            product.setPrice(BigDecimal.ZERO);
            product.setAvailable(false);
            product.setStatus(false);
            Product saved = productRepository.save(product);
            proposal.setCreatedEntityType("Product");
            proposal.setCreatedEntityId(String.valueOf(saved.getId()));
            if ("RECIPE".equals(type) && payload.get("ingredients") instanceof List<?> lines) {
                for (Object line : lines) {
                    if (!(line instanceof Map<?, ?> raw)) continue;
                    Long ingredientId = longOr(raw.get("ingredientId"));
                    BigDecimal amount = decimal(raw.get("amount"));
                    if (ingredientId != null && amount != null) {
                        Ingredient ingredient = ingredientRepository.findById(ingredientId).orElseThrow(() -> new IllegalArgumentException("Nguyên liệu không tồn tại"));
                        Recipe recipe = new Recipe(); recipe.setProduct(saved); recipe.setIngredient(ingredient); recipe.setAmountRequired(amount); recipeRepository.save(recipe);
                    }
                }
                proposal.setCreatedEntityType("Product+Recipe");
            }
        }
        proposal.setStatus("APPROVED"); proposal.setReviewedBy(authentication.getName()); proposal.setReviewedAt(LocalDateTime.now()); proposal.setReviewNote(request == null ? null : request.note());
        KitchenProposal result = proposalRepository.save(proposal);
        activityLogService.log("APPROVE_KITCHEN_PROPOSAL", "KitchenProposal", String.valueOf(id), "Approved " + type);
        return result;
    }

    @Transactional
    public KitchenProposal reject(Long id, KitchenProposalReviewRequest request, Authentication authentication) {
        if (request == null || request.note() == null || request.note().isBlank()) throw new IllegalArgumentException("Từ chối phải có lý do");
        KitchenProposal proposal = proposalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề xuất"));
        if (!"PENDING".equals(proposal.getStatus())) throw new IllegalStateException("Đề xuất đã được xử lý");
        proposal.setStatus("REJECTED"); proposal.setReviewNote(request.note().trim()); proposal.setReviewedBy(authentication.getName()); proposal.setReviewedAt(LocalDateTime.now());
        KitchenProposal result = proposalRepository.save(proposal);
        activityLogService.log("REJECT_KITCHEN_PROPOSAL", "KitchenProposal", String.valueOf(id), "Rejected proposal");
        return result;
    }

    private Map<String, Object> parsePayload(String payload) {
        try { return objectMapper.readValue(payload, Map.class); }
        catch (Exception e) { throw new IllegalArgumentException("Payload đề xuất phải là JSON hợp lệ"); }
    }
    private String required(Map<?, ?> map, String key) { Object value = map.get(key); if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("Thiếu trường " + key); return value.toString().trim(); }
    private BigDecimal decimal(Object value) { try { return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); } catch (Exception e) { throw new IllegalArgumentException("Số lượng không hợp lệ"); } }
    private BigDecimal decimalOr(Object value, BigDecimal fallback) { return value == null ? fallback : decimal(value); }
    private Integer integerOr(Object value, int fallback) { try { return value == null ? fallback : Integer.valueOf(value.toString()); } catch (Exception e) { return fallback; } }
    private Long longOr(Object value) { try { return value == null ? null : Long.valueOf(value.toString()); } catch (Exception e) { return null; } }
}
