package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class TableImageStorageServiceTest {

    @TempDir
    Path temporaryUploadRoot;

    @Test
    void storesAnAllowedImageUnderThePublicTableImagesPath() throws Exception {
        TableImageStorageService service = new TableImageStorageService(temporaryUploadRoot.toString());
        MockMultipartFile image = new MockMultipartFile("file", "table.png", "image/png", new byte[] {1, 2, 3});

        String imageUrl = service.store(image);

        assertTrue(imageUrl.matches("/uploads/table-images/[a-f0-9-]{36}\\.png"));
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        assertTrue(Files.exists(temporaryUploadRoot.resolve("table-images").resolve(filename)));
    }

    @Test
    void rejectsNonImageFiles() {
        TableImageStorageService service = new TableImageStorageService(temporaryUploadRoot.toString());
        MockMultipartFile document = new MockMultipartFile("file", "table.pdf", "application/pdf", new byte[] {1});

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.store(document));

        assertEquals("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.", exception.getMessage());
    }
}
