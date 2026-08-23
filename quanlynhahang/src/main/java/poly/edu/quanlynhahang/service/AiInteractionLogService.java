package poly.edu.quanlynhahang.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.AiInteractionLog;
import poly.edu.quanlynhahang.repository.AiInteractionLogRepository;

@Service
public class AiInteractionLogService {
    private static final Logger log = LoggerFactory.getLogger(AiInteractionLogService.class);
    private final AiInteractionLogRepository repository;

    public AiInteractionLogService(AiInteractionLogRepository repository) {
        this.repository = repository;
    }

    public Long log(String sessionId, String type, String question, String response, String source) {
        try {
            AiInteractionLog entry = new AiInteractionLog();
            entry.setSessionId(limit(sessionId, 80));
            entry.setRequestType(limit(type, 40));
            entry.setQuestion(redact(limit(question, 4000)));
            entry.setResponse(redact(limit(response, 12000)));
            entry.setSource(limit(source, 40));
            return repository.save(entry).getId();
        } catch (RuntimeException exception) {
            log.error("Unable to persist AI interaction audit event", exception);
            return null;
        }
    }

    public List<AiInteractionLog> recent() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }

    public void feedback(Long id, String sessionId, boolean helpful, String comment) {
        if (id == null || sessionId == null) throw new IllegalArgumentException("Phản hồi AI không hợp lệ");
        AiInteractionLog entry = repository.findByIdAndSessionId(id, sessionId).orElseThrow();
        entry.setHelpful(helpful);
        entry.setFeedbackComment(redact(limit(comment, 1000)));
        repository.save(entry);
    }

    private String redact(String value) {
        return value == null ? null : value
                .replaceAll("(?i)[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}", "[EMAIL]")
                .replaceAll("(?<!\\d)(?:\\+?84|0)\\d{8,10}(?!\\d)", "[PHONE]");
    }

    private String limit(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(max, value.length()));
    }
}
