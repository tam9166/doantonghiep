package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class CvFileStorageServiceTest {
    @TempDir Path tempDirectory;

    @Test
    void storesValidatedPdfUnderAServerGeneratedName() throws Exception {
        CvFileStorageService service = new CvFileStorageService(tempDirectory);
        MockMultipartFile file = file("cv.pdf", "application/pdf", "%PDF-1.7\n1 0 obj\n%%EOF".getBytes(StandardCharsets.US_ASCII));

        String stored = service.store(file);

        assertTrue(stored.matches("^[a-f0-9-]{36}\\.pdf$"));
        assertNotNull(service.load(stored));
    }

    @Test
    void rejectsTraversalSpoofedMimeAndActivePdf() {
        CvFileStorageService service = new CvFileStorageService(tempDirectory);
        assertThrows(IllegalArgumentException.class,
                () -> service.store(file("../cv.pdf", "application/pdf", "%PDF-1.7".getBytes())));
        assertThrows(IllegalArgumentException.class,
                () -> service.store(file("cv.pdf", "image/png", "%PDF-1.7".getBytes())));
        assertThrows(IllegalArgumentException.class,
                () -> service.store(file("cv.pdf", "application/pdf", "%PDF-1.7 /JavaScript".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> service.load("../../secret.pdf"));
    }

    @Test
    void validatesDocxStructureAndRejectsExecutableParts() throws Exception {
        CvFileStorageService service = new CvFileStorageService(tempDirectory);
        byte[] valid = docx(Map.of(
                "[Content_Types].xml", "<Types/>",
                "_rels/.rels", "<Relationships/>",
                "word/document.xml", "<document><body>CV</body></document>"));
        String stored = service.store(file("cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", valid));
        assertTrue(Files.exists(tempDirectory.resolve(stored)));

        byte[] macro = docx(Map.of(
                "[Content_Types].xml", "<Types/>",
                "_rels/.rels", "<Relationships/>",
                "word/document.xml", "<document/>",
                "word/vbaProject.bin", "macro"));
        assertThrows(IllegalArgumentException.class, () -> service.store(file("macro.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", macro)));
    }

    @Test
    void rejectsInvalidDocxAndCompressionBomb() throws Exception {
        CvFileStorageService service = new CvFileStorageService(tempDirectory);
        byte[] missingStructure = docx(Map.of("word/document.xml", "<document/>"));
        assertThrows(IllegalArgumentException.class, () -> service.store(file("bad.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", missingStructure)));

        byte[] bomb = docx(Map.of(
                "[Content_Types].xml", "<Types/>",
                "_rels/.rels", "<Relationships/>",
                "word/document.xml", "a".repeat(2_000_000)));
        assertThrows(IllegalArgumentException.class, () -> service.store(file("bomb.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bomb)));
    }

    @Test
    void rejectsOversizedFilesBeforeWriting() {
        CvFileStorageService service = new CvFileStorageService(tempDirectory);
        byte[] oversized = new byte[(int) CvFileStorageService.MAX_FILE_BYTES + 1];
        assertThrows(IllegalArgumentException.class,
                () -> service.store(file("large.pdf", "application/pdf", oversized)));
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private byte[] docx(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
