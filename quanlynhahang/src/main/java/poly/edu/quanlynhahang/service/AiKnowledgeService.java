package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.entity.AiKnowledgeSource;
import poly.edu.quanlynhahang.repository.AiKnowledgeSourceRepository;
import poly.edu.quanlynhahang.repository.AiBrandProfileRepository;
import poly.edu.quanlynhahang.repository.AiFaqExampleRepository;
import poly.edu.quanlynhahang.entity.AiBrandProfile;
import poly.edu.quanlynhahang.entity.AiFaqExample;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeService {
    private static final Set<String> STOP_WORDS = Set.of("cho", "toi", "hoi", "nha", "hang", "khach", "quy", "cua", "the", "nao", "khong", "duoc", "voi", "mot", "cac", "hay", "can", "biet");
    private final AiKnowledgeSourceRepository repository;
    private final AiBrandProfileRepository brandRepository;
    private final AiFaqExampleRepository faqRepository;
    public AiKnowledgeService(AiKnowledgeSourceRepository repository, AiBrandProfileRepository brandRepository, AiFaqExampleRepository faqRepository) {
        this.repository = repository; this.brandRepository = brandRepository; this.faqRepository = faqRepository;
    }
    public List<AiKnowledgeSource> all() { return repository.findAll(); }
    public AiKnowledgeSource save(AiKnowledgeSource source) {
        source.setId(null); source.setTitle(clean(source.getTitle(), 200));
        source.setContent(clean(source.getContent(), 50000));
        if (source.getTitle().isBlank() || source.getContent().isBlank()) throw new IllegalArgumentException("Tiêu đề và nội dung không được để trống");
        return repository.save(source);
    }
    public AiKnowledgeSource update(Long id, AiKnowledgeSource input) {
        AiKnowledgeSource source = repository.findById(id).orElseThrow();
        source.setTitle(clean(input.getTitle(), 200)); source.setContent(clean(input.getContent(), 50000));
        source.setType(clean(input.getType(), 30)); source.setEnabled(!Boolean.FALSE.equals(input.getEnabled()));
        return repository.save(source);
    }
    public void delete(Long id) { repository.deleteById(id); }
    public AiKnowledgeSource upload(MultipartFile file, String title) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Tệp rỗng");
        if (file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Tệp không được vượt quá 5 MB");
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("document");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!Set.of("txt", "md", "pdf", "docx").contains(ext)) throw new IllegalArgumentException("Chỉ hỗ trợ TXT, Markdown, PDF và DOCX");
        byte[] bytes = file.getBytes();
        String content = switch (ext) {
            case "txt", "md" -> new String(bytes, StandardCharsets.UTF_8);
            case "pdf" -> extractPdf(bytes);
            case "docx" -> extractDocx(bytes);
            default -> "";
        };
        if (content.isBlank()) throw new IllegalArgumentException("Không đọc được nội dung văn bản trong tệp");
        AiKnowledgeSource source = new AiKnowledgeSource();
        source.setTitle(title == null || title.isBlank() ? original : title);
        source.setType(ext.toUpperCase(Locale.ROOT)); source.setOriginalFilename(original.replaceAll("[^\\p{L}\\p{N}._ -]", "_"));
        source.setMimeType(file.getContentType()); source.setProcessingStatus("READY"); source.setContent(content);
        return save(source);
    }
    public AiBrandProfile brand() { return brandRepository.findById(1).orElseGet(() -> brandRepository.save(new AiBrandProfile())); }
    public AiBrandProfile saveBrand(AiBrandProfile profile) { profile.setId(1); return brandRepository.save(profile); }
    public List<AiFaqExample> faqs() { return faqRepository.findAll(); }
    public AiFaqExample saveFaq(AiFaqExample faq) { faq.setId(null); return faqRepository.save(faq); }
    public AiFaqExample updateFaq(Long id, AiFaqExample input) { input.setId(id); return faqRepository.save(input); }
    public void deleteFaq(Long id) { faqRepository.deleteById(id); }
    public String retrieve(String query) {
        Set<String> words = Arrays.stream(normalize(query).split("\\s+")).filter(w -> w.length() > 2 && !STOP_WORDS.contains(w)).collect(Collectors.toSet());
        String sources = repository.findByEnabledTrueOrderByUpdatedAtDesc().stream()
                .map(s -> Map.entry(s, words.stream().filter(w -> normalize(s.getTitle() + " " + s.getContent()).contains(w)).count()))
                .filter(e -> e.getValue() > 0).sorted(Map.Entry.<AiKnowledgeSource,Long>comparingByValue().reversed())
                .limit(3).map(e -> "[" + e.getKey().getTitle() + "]\n" + e.getKey().getContent()).collect(Collectors.joining("\n---\n"));
        String examples = faqRepository.findByEnabledTrue().stream().filter(f -> words.stream().anyMatch(w -> normalize(f.getQuestion()).contains(w)))
                .limit(2).map(f -> "[FAQ]\nHỏi: " + f.getQuestion() + "\nTrả lời mẫu: " + f.getIdealAnswer()).collect(Collectors.joining("\n---\n"));
        return String.join("\n---\n", List.of(sources, examples).stream().filter(s -> !s.isBlank()).toList());
    }
    public String brandPrompt() {
        AiBrandProfile b = brand();
        return "THƯƠNG HIỆU: " + clean(b.getBrandName(),150) + "; XƯNG HÔ: " + clean(b.getAddressing(),300)
                + "; GIỌNG ĐIỆU: " + clean(b.getToneOfVoice(),500) + "; QUY TẮC KHÔNG BIẾT: " + clean(b.getUnknownAnswerRule(),1000)
                + "; KHÔNG BỊA: " + clean(b.getNoFabricationRule(),1000) + "; CHUYỂN NHÂN VIÊN: " + clean(b.getHandoffRule(),1000);
    }
    private String extractPdf(byte[] bytes) throws IOException {
        try (var doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            return text != null ? text : "";
        }
    }
    private String extractDocx(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) if ("word/document.xml".equals(entry.getName())) {
                String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                return xml.replace("</w:p>", "\n").replace("</w:tab>", "\t").replaceAll("<[^>]+>", " ")
                        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
            }
        }
        return "";
    }
    private String clean(String value, int max) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        return cleaned.substring(0, Math.min(max, cleaned.length()));
    }
    private String normalize(String value) { return Normalizer.normalize(Optional.ofNullable(value).orElse("").toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", ""); }
}
