package poly.edu.quanlynhahang.service;
import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.entity.AiInteractionLog;
import poly.edu.quanlynhahang.repository.AiInteractionLogRepository;
import java.util.List;
@Service
public class AiInteractionLogService {
 private final AiInteractionLogRepository repository;
 public AiInteractionLogService(AiInteractionLogRepository repository){this.repository=repository;}
 public Long log(String sessionId,String type,String question,String response,String source){try{AiInteractionLog l=new AiInteractionLog();l.setSessionId(limit(sessionId,80));l.setRequestType(limit(type,40));l.setQuestion(redact(limit(question,4000)));l.setResponse(redact(limit(response,12000)));l.setSource(limit(source,40));return repository.save(l).getId();}catch(RuntimeException ignored){return null;}}
 public List<AiInteractionLog> recent(){return repository.findTop200ByOrderByCreatedAtDesc();}
 public void feedback(Long id,String sessionId,boolean helpful,String comment){if(id==null||sessionId==null)throw new IllegalArgumentException("Phản hồi AI không hợp lệ");AiInteractionLog l=repository.findByIdAndSessionId(id,sessionId).orElseThrow();l.setHelpful(helpful);l.setFeedbackComment(redact(limit(comment,1000)));repository.save(l);}
 private String redact(String s){return s==null?null:s.replaceAll("(?i)[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}","[EMAIL]").replaceAll("(?<!\\d)(?:\\+?84|0)\\d{8,10}(?!\\d)","[PHONE]");}
 private String limit(String s,int max){return s==null?null:s.substring(0,Math.min(max,s.length()));}
}
