package poly.edu.quanlynhahang.controller;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.AiInteractionLog;
import poly.edu.quanlynhahang.service.AiInteractionLogService;
import java.util.*;
@RestController
public class AiFeedbackController {
 private final AiInteractionLogService service; public AiFeedbackController(AiInteractionLogService service){this.service=service;}
 @PostMapping("/api/ai/feedback") public Map<String,Boolean> feedback(@RequestBody FeedbackRequest r){service.feedback(r.interactionId(),r.sessionId(),r.helpful(),r.comment());return Map.of("saved",true);}
 @GetMapping("/api/admin/ai/logs") public List<AiInteractionLog> logs(){return service.recent();}
 public record FeedbackRequest(Long interactionId,String sessionId,boolean helpful,String comment){}
}
