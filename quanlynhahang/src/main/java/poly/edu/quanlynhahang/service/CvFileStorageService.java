package poly.edu.quanlynhahang.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvFileStorageService {
    static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    static final long MAX_DOCX_UNCOMPRESSED_BYTES = 20L * 1024 * 1024;
    static final int MAX_DOCX_ENTRIES = 512;
    private static final Set<String> EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Map<String, String> MIME_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final Set<String> DOCX_REQUIRED = Set.of(
            "[Content_Types].xml", "_rels/.rels", "word/document.xml");

    private final Path cvDirectory;

    @Autowired
    public CvFileStorageService(@Value("${app.upload.private-root:private-uploads}") String privateRoot) {
        this(Path.of(privateRoot).toAbsolutePath().normalize().resolve("cvs").normalize());
    }

    CvFileStorageService(Path cvDirectory) {
        this.cvDirectory = cvDirectory.toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) throws IOException {
        ValidatedFile validated = validate(file);
        Files.createDirectories(cvDirectory);
        String filename = UUID.randomUUID() + "." + validated.extension;
        Path destination = resolveFilename(filename);
        Files.write(destination, validated.content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return filename;
    }

    public Resource load(String filename) throws IOException {
        Path path = resolveFilename(filename);
        if (!Files.isRegularFile(path)) return null;
        return new UrlResource(path.toUri());
    }

    public void delete(String filename) throws IOException {
        Files.deleteIfExists(resolveFilename(filename));
    }

    private ValidatedFile validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File CV không được để trống.");
        if (file.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException("File CV tối đa 5MB.");
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank() || originalName.indexOf('\0') >= 0
                || originalName.contains("/") || originalName.contains("\\") || originalName.contains("..")) {
            throw new IllegalArgumentException("Tên file CV không hợp lệ.");
        }
        int dot = originalName.lastIndexOf('.');
        String extension = dot < 1 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)) throw new IllegalArgumentException("CV chỉ chấp nhận PDF, DOC hoặc DOCX.");
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!MIME_TYPES.get(extension).equals(mime)) throw new IllegalArgumentException("Kiểu file CV không hợp lệ.");

        byte[] bytes = file.getBytes();
        if (bytes.length == 0 || bytes.length > MAX_FILE_BYTES) throw new IllegalArgumentException("Kích thước file CV không hợp lệ.");
        switch (extension) {
            case "pdf" -> validatePdf(bytes);
            case "doc" -> validateLegacyDoc(bytes);
            case "docx" -> validateDocx(bytes);
            default -> throw new IllegalArgumentException("Định dạng file CV không hợp lệ.");
        }
        return new ValidatedFile(extension, bytes);
    }

    private void validatePdf(byte[] bytes) {
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D'
                || bytes[3] != 'F' || bytes[4] != '-') {
            throw new IllegalArgumentException("Nội dung PDF không hợp lệ.");
        }
        String content = new String(bytes, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        if (content.contains("/javascript") || content.contains("/launch")
                || content.contains("/embeddedfile") || content.contains("/openaction")) {
            throw new IllegalArgumentException("PDF chứa nội dung chủ động không được phép.");
        }
    }

    private void validateLegacyDoc(byte[] bytes) {
        byte[] magic = {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1};
        if (bytes.length < magic.length) throw new IllegalArgumentException("Nội dung DOC không hợp lệ.");
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) throw new IllegalArgumentException("Nội dung DOC không hợp lệ.");
        }
        String content = new String(bytes, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        if (content.contains("vba") || content.contains("_vba_project") || content.contains("macros")) {
            throw new IllegalArgumentException("DOC chứa macro không được phép.");
        }
    }

    private void validateDocx(byte[] bytes) throws IOException {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new IllegalArgumentException("Nội dung DOCX không hợp lệ.");
        }
        Set<String> entries = new HashSet<>();
        long totalUncompressed = 0;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entryCount > MAX_DOCX_ENTRIES) throw new IllegalArgumentException("DOCX có quá nhiều thành phần.");
                String name = entry.getName();
                if (name.startsWith("/") || name.startsWith("\\") || name.contains("..")
                        || name.contains("\\") || name.contains(":")) {
                    throw new IllegalArgumentException("DOCX chứa đường dẫn không an toàn.");
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.endsWith("vbaproject.bin") || lower.startsWith("word/embeddings/")
                        || lower.startsWith("word/activex/")) {
                    throw new IllegalArgumentException("DOCX chứa nội dung thực thi không được phép.");
                }
                entries.add(name);
                ByteArrayOutputStream entryContent = new ByteArrayOutputStream();
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    totalUncompressed += read;
                    if (totalUncompressed > MAX_DOCX_UNCOMPRESSED_BYTES
                            || totalUncompressed > Math.max(1L, bytes.length) * 100L) {
                        throw new IllegalArgumentException("DOCX có tỷ lệ nén không an toàn.");
                    }
                    if (lower.endsWith(".xml") || lower.endsWith(".rels")) entryContent.write(buffer, 0, read);
                }
                if (entryContent.size() > 0) {
                    String xml = entryContent.toString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                    if (xml.contains("targetmode=\"external\"") || xml.contains("<!doctype") || xml.contains("<!entity")) {
                        throw new IllegalArgumentException("DOCX chứa liên kết hoặc thực thể ngoài không được phép.");
                    }
                }
            }
        } catch (java.util.zip.ZipException exception) {
            throw new IllegalArgumentException("Cấu trúc DOCX không hợp lệ.");
        }
        if (!entries.containsAll(DOCX_REQUIRED)) throw new IllegalArgumentException("Thiếu cấu trúc DOCX bắt buộc.");
    }

    private Path resolveFilename(String filename) {
        if (filename == null || !filename.matches("^[a-fA-F0-9-]{36}\\.(pdf|doc|docx)$")) {
            throw new IllegalArgumentException("Tên file CV không hợp lệ.");
        }
        Path path = cvDirectory.resolve(filename).normalize();
        if (!path.startsWith(cvDirectory)) throw new IllegalArgumentException("Tên file CV không hợp lệ.");
        return path;
    }

    private record ValidatedFile(String extension, byte[] content) {
    }
}
