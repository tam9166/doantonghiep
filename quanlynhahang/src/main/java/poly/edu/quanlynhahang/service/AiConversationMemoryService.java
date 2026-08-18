package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

@Service
public class AiConversationMemoryService {
    private static final Duration TTL = Duration.ofHours(2);
    private static final Pattern GUESTS = Pattern.compile("(?i)(\\d{1,3})\\s*(khách|người|nguoi)");
    private static final Pattern TIME = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])[:h]([0-5]\\d)?");
    private final Map<String, Memory> sessions = new ConcurrentHashMap<>();

    public Memory remember(String requestedSessionId, String message) {
        purge();
        String id = requestedSessionId != null && requestedSessionId.matches("[A-Za-z0-9_-]{8,80}") ? requestedSessionId : UUID.randomUUID().toString();
        Memory previous = sessions.getOrDefault(id, new Memory(id, null, null, null, null, Instant.now()));
        Integer guests = matchInt(GUESTS, message, previous.guestCount());
        LocalTime time = matchTime(message, previous.time());
        LocalDate date = matchDate(message, previous.date());
        String area = matchArea(message, previous.area());
        Memory updated = new Memory(id, guests, date, time, area, Instant.now()); sessions.put(id, updated); return updated;
    }
    private Integer matchInt(Pattern pattern,String text,Integer fallback){ Matcher m=pattern.matcher(Optional.ofNullable(text).orElse("")); return m.find()?Integer.parseInt(m.group(1)):fallback; }
    private LocalTime matchTime(String text,LocalTime fallback){ Matcher m=TIME.matcher(Optional.ofNullable(text).orElse("")); if(!m.find())return fallback; return LocalTime.of(Integer.parseInt(m.group(1)),m.group(2)==null?0:Integer.parseInt(m.group(2))); }
    private LocalDate matchDate(String text,LocalDate fallback){ String s=normalize(text); LocalDate today=LocalDate.now(); if(s.contains("ngay mai")||s.contains("toi mai"))return today.plusDays(1); if(s.contains("hom nay")||s.contains("toi nay"))return today; Matcher m=Pattern.compile("(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{4}))?").matcher(s); if(m.find())try{return LocalDate.of(m.group(3)==null?today.getYear():Integer.parseInt(m.group(3)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(1)));}catch(DateTimeException ignored){return fallback;} return fallback; }
    private String matchArea(String text,String fallback){ String s=normalize(text); for(String area:List.of("san vuon","phong vip","san thuong","sanh su kien"))if(s.contains(area))return area; return fallback; }
    private String normalize(String s){return java.text.Normalizer.normalize(Optional.ofNullable(s).orElse("").toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");}
    private void purge(){Instant cutoff=Instant.now().minus(TTL); sessions.entrySet().removeIf(e->e.getValue().updatedAt().isBefore(cutoff)); if(sessions.size()>5000)sessions.clear();}
    public record Memory(String sessionId,Integer guestCount,LocalDate date,LocalTime time,String area,Instant updatedAt) {}
}
