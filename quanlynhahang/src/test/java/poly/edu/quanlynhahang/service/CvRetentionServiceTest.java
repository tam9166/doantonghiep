package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import poly.edu.quanlynhahang.entity.Application;
import poly.edu.quanlynhahang.repository.ApplicationRepository;

class CvRetentionServiceTest {
    @TempDir Path tempDirectory;

    @Test
    void removesExpiredPrivateFileAndClearsDatabaseReference() throws Exception {
        ApplicationRepository repository = mock(ApplicationRepository.class);
        CvFileStorageService storage = new CvFileStorageService(tempDirectory);
        String filename = storage.store(new MockMultipartFile("file", "cv.pdf", "application/pdf",
                "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII)));
        Application application = new Application();
        application.setId(7);
        application.setCreateDate(Date.from(Instant.parse("2025-01-01T00:00:00Z")));
        application.setCvFile("/api/applications/cv/" + filename);
        when(repository.findByCvFileIsNotNullAndCreateDateBefore(any())).thenReturn(List.of(application));
        CvRetentionService retention = new CvRetentionService(repository, storage, 180,
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));

        retention.purgeExpiredCvFiles();

        assertNull(application.getCvFile());
        verify(repository).save(application);
    }
}
