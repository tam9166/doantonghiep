package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

@Service
public class AiSafeQueryToolService {
    private static final Pattern CODE=Pattern.compile("(?i)MV-\\d{8}-\\d{4}");
    private static final Pattern PHONE=Pattern.compile("(?<!\\d)(?:\\+?84|0)\\d{9,10}(?!\\d)");
    private static final Set<String> STOP=Set.of("mon","nay","nao","cho","toi","gia","bao","nhieu","co","khong","tim","muon","an","xem","nha","hang");
    private final ProductRepository products; private final HotMenuItemService hotItems;
    private final ReservationRepository reservations; private final AiKnowledgeService knowledge;
    public AiSafeQueryToolService(ProductRepository products,HotMenuItemService hotItems,ReservationRepository reservations,AiKnowledgeService knowledge){this.products=products;this.hotItems=hotItems;this.reservations=reservations;this.knowledge=knowledge;}

    public Optional<ToolAnswer> answer(String message){
        String q=normalize(message);
        if(q.matches(".*mon\\s+(nao\\s+)?hot.*")||q.contains("ban chay")||q.contains("pho bien nhat")){
            var items=hotItems.getHotMenuItems(5); String reply=items.isEmpty()?"Nhà hàng chưa có đủ dữ liệu bán hàng để xếp hạng món hot.":"Món bán chạy theo dữ liệu 7/30/90 ngày: "+items.stream().map(i->i.name()+" ("+money(i.price())+")").reduce((a,b)->a+", "+b).orElse("")+".";
            return Optional.of(new ToolAnswer(reply,"HOT_ITEMS_TOOL"));
        }
        Matcher code=CODE.matcher(Optional.ofNullable(message).orElse(""));
        if(code.find()){
            Matcher phone=PHONE.matcher(Optional.ofNullable(message).orElse(""));
            if(!phone.find())return Optional.of(new ToolAnswer("Để bảo vệ thông tin đặt bàn, Quý khách vui lòng cung cấp số điện thoại đã dùng cùng mã "+code.group().toUpperCase(Locale.ROOT)+".","RESERVATION_TOOL"));
            String normalizedPhone=normalizePhone(phone.group());
            return Optional.of(reservations.findByReservationCodeAndCustomerPhone(code.group().toUpperCase(Locale.ROOT),normalizedPhone)
                    .map(r->new ToolAnswer("Booking "+r.getReservationCode()+" hiện ở trạng thái "+r.getReservationStatus()+", ngày "+r.getReservationDate()+" lúc "+r.getArrivalTime()+", "+r.getGuestCount()+" khách, khu vực "+(r.getArea()==null?"đang bố trí":r.getArea().getNameVi())+".","RESERVATION_TOOL"))
                    .orElse(new ToolAnswer("Không tìm thấy booking khớp đồng thời mã đặt chỗ và số điện thoại đã cung cấp.","RESERVATION_TOOL")));
        }
        if(q.contains("chinh sach")||q.contains("thu cung")||q.contains("policy")){
            String result=knowledge.retrieve(message); if(!result.isBlank())return Optional.of(new ToolAnswer(result.replaceAll("(?m)^\\[[^]]+]\\s*","").trim(),"POLICY_TOOL"));
        }
        if(q.contains("co mon")||q.contains("tim mon")||q.contains("gia mon")||q.startsWith("mon ")){
            Set<String> terms=new LinkedHashSet<>(Arrays.asList(q.split("\\s+")));terms.removeIf(w->w.length()<2||STOP.contains(w));
            List<Product> matches=products.findByAvailableTrueAndStatusTrue().stream().filter(p->terms.isEmpty()||terms.stream().anyMatch(t->normalize(searchable(p)).contains(t))).limit(5).toList();
            String reply=matches.isEmpty()?"Nhà hàng chưa tìm thấy món đang phục vụ phù hợp với yêu cầu này.":"Các món đang phục vụ phù hợp: "+matches.stream().map(p->displayName(p)+" ("+money(p.getPrice())+")").reduce((a,b)->a+", "+b).orElse("")+".";
            return Optional.of(new ToolAnswer(reply,"MENU_SEARCH_TOOL"));
        }
        return Optional.empty();
    }
    private String searchable(Product p){return displayName(p)+" "+Optional.ofNullable(p.getDescriptionVi()).orElse("")+" "+Optional.ofNullable(p.getDescription()).orElse("");}
    private String displayName(Product p){return p.getNameVi()!=null&&!p.getNameVi().isBlank()?p.getNameVi():p.getName();}
    private String money(java.math.BigDecimal v){return String.format(Locale.forLanguageTag("vi-VN"),"%,.0f đ",v==null?java.math.BigDecimal.ZERO:v);}
    private String normalizePhone(String p){String digits=p.replaceAll("\\D","");return digits.startsWith("84")?"0"+digits.substring(2):digits;}
    private String normalize(String s){return Normalizer.normalize(Optional.ofNullable(s).orElse("").toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
    public record ToolAnswer(String reply,String source){}
}
