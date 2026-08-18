package poly.edu.quanlynhahang.service;
import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.dto.HotMenuItemResponse;
import poly.edu.quanlynhahang.entity.*;
import poly.edu.quanlynhahang.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AiSafeQueryToolServiceTest {
 private final ProductRepository products=mock(ProductRepository.class); private final HotMenuItemService hot=mock(HotMenuItemService.class); private final ReservationRepository reservations=mock(ReservationRepository.class); private final AiKnowledgeService knowledge=mock(AiKnowledgeService.class);
 private final AiSafeQueryToolService service=new AiSafeQueryToolService(products,hot,reservations,knowledge);
 @Test void hotItemsComeOnlyFromSalesRanking(){when(hot.getHotMenuItems(5)).thenReturn(List.of(new HotMenuItemResponse(1,"Lẩu cá",null,BigDecimal.valueOf(250000),4,20,60,12d)));var answer=service.answer("Món nào hot?").orElseThrow();assertEquals("HOT_ITEMS_TOOL",answer.source());assertTrue(answer.reply().contains("Lẩu cá"));}
 @Test void menuSearchUsesOnlyActiveAvailableProducts(){Product p=new Product();p.setId(2);p.setNameVi("Nấm nướng");p.setDescriptionVi("Món chay");p.setPrice(BigDecimal.valueOf(90000));when(products.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(p));var answer=service.answer("Có món nấm nào?").orElseThrow();assertEquals("MENU_SEARCH_TOOL",answer.source());assertTrue(answer.reply().contains("Nấm nướng"));}
 @Test void reservationLookupRequiresPhoneAndMatchesBothValues(){assertTrue(service.answer("Kiểm tra MV-20260820-0001").orElseThrow().reply().contains("số điện thoại"));Reservation r=new Reservation();r.setReservationCode("MV-20260820-0001");r.setReservationStatus(ReservationStatus.CONFIRMED);r.setReservationDate(LocalDate.of(2026,8,20));r.setArrivalTime(LocalTime.of(19,0));r.setGuestCount(4);when(reservations.findByReservationCodeAndCustomerPhone("MV-20260820-0001","0912345678")).thenReturn(Optional.of(r));var answer=service.answer("MV-20260820-0001 số 0912345678").orElseThrow();assertEquals("RESERVATION_TOOL",answer.source());assertTrue(answer.reply().contains("CONFIRMED"));}
 @Test void policyUsesApprovedKnowledge(){when(knowledge.retrieve("Chính sách thú cưng?")).thenReturn("[Policy]\nChỉ khu sân vườn cho phép.");var answer=service.answer("Chính sách thú cưng?").orElseThrow();assertEquals("POLICY_TOOL",answer.source());assertTrue(answer.reply().contains("sân vườn"));}
}
