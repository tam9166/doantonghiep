package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import java.util.*;

@Service
public class AiDynamicToolService {
    private final RestaurantCapacityService capacityService;
    private final TableAreaRepository areas;
    public AiDynamicToolService(RestaurantCapacityService capacityService, TableAreaRepository areas) { this.capacityService=capacityService; this.areas=areas; }

    public Optional<String> answerAvailabilityQuestion(String message, AiConversationMemoryService.Memory memory) {
        String normalized = normalize(message);
        boolean availabilityQuestion = List.of("con cho", "con ban", "suc chua", "dat duoc", "tiep nhan", "available")
                .stream().anyMatch(normalized::contains);
        if (!availabilityQuestion) return Optional.empty();
        List<String> missing = new ArrayList<>();
        if (memory.guestCount()==null) missing.add("số khách"); if(memory.date()==null)missing.add("ngày"); if(memory.time()==null)missing.add("giờ");
        if(!missing.isEmpty()) return Optional.of("Để kiểm tra dữ liệu thực tế, Quý khách vui lòng cho Nhà hàng biết " + String.join(", ",missing) + ".");
        var snapshot=capacityService.checkCapacity(memory.date(),memory.time(),120,memory.guestCount());
        if(!snapshot.available()) return Optional.of("Khung giờ " + memory.time() + " ngày " + memory.date() + " không còn đủ sức chứa cho " + memory.guestCount() + " khách. Nhà hàng hiện chỉ còn khả năng tiếp nhận tối đa " + snapshot.remainingCapacity() + " khách.");
        String suitable=areas.findByStatusOrderByNameViAsc("ACTIVE").stream()
                .filter(a -> (a.getMinGuestCount()==null || a.getMinGuestCount()<=memory.guestCount()) && (a.getMaxGuestCount()==null || a.getMaxGuestCount()>=memory.guestCount()))
                .map(a->a.getNameVi()).filter(Objects::nonNull).limit(4).reduce((a,b)->a+", "+b).orElse("khu vực đang hoạt động");
        return Optional.of("Theo dữ liệu đặt bàn hiện tại, khung giờ " + memory.time() + " ngày " + memory.date() + " còn " + snapshot.remainingCapacity() + " chỗ, đủ tiếp nhận đoàn " + memory.guestCount() + " khách. Khu vực phù hợp: " + suitable + ". Kết quả chỉ được giữ chỗ sau khi Quý khách hoàn tất đặt bàn.");
    }
    private String normalize(String value){return java.text.Normalizer.normalize(Optional.ofNullable(value).orElse("").toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");}
}
