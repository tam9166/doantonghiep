package poly.edu.quanlynhahang.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TableImageStorageService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "webp", Set.of("image/webp"));

    private final Path tableImageDirectory;

    public TableImageStorageService(@Value("${app.upload.root:uploads}") String uploadRoot) {
        this.tableImageDirectory = Path.of(uploadRoot).toAbsolutePath().normalize().resolve("table-images").normalize();
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh bàn.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Ảnh bàn tối đa 5MB.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        Set<String> acceptedTypes = ALLOWED_TYPES.get(extension);
        if (acceptedTypes == null || file.getContentType() == null
                || !acceptedTypes.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.");
        }

        Files.createDirectories(tableImageDirectory);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = tableImageDirectory.resolve(filename).normalize();
        if (!target.startsWith(tableImageDirectory)) {
            throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ.");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/table-images/" + filename;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int extensionIndex = filename.lastIndexOf('.');
        return extensionIndex < 1 ? "" : filename.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }
}
