package poly.edu.quanlynhahang.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Application;
import poly.edu.quanlynhahang.repository.ApplicationRepository;

@Service
public class CvRetentionService {
    private static final Logger log = LoggerFactory.getLogger(CvRetentionService.class);
    private final ApplicationRepository applicationRepository;
    private final CvFileStorageService storageService;
    private final int retentionDays;
    private final Clock clock;

    @Autowired
    public CvRetentionService(ApplicationRepository applicationRepository,
                              CvFileStorageService storageService,
                              @Value("${app.upload.cv-retention-days:180}") int retentionDays) {
        this(applicationRepository, storageService, retentionDays, Clock.systemUTC());
    }

    CvRetentionService(ApplicationRepository applicationRepository,
                       CvFileStorageService storageService,
                       int retentionDays,
                       Clock clock) {
        this.applicationRepository = applicationRepository;
        this.storageService = storageService;
        this.retentionDays = Math.max(1, retentionDays);
        this.clock = clock;
    }

    @Scheduled(cron = "${app.upload.cv-retention-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpiredCvFiles() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(retentionDays));
        for (Application application : applicationRepository
                .findByCvFileIsNotNullAndCreateDateBefore(Date.from(cutoff))) {
            String filename = filename(application.getCvFile());
            if (filename == null) continue;
            try {
                storageService.delete(filename);
                application.setCvFile(null);
                applicationRepository.save(application);
            } catch (IOException | IllegalArgumentException exception) {
                log.warn("Unable to purge expired CV for application {}", application.getId(), exception);
            }
        }
    }

    private String filename(String url) {
        String prefix = "/api/applications/cv/";
        return url != null && url.startsWith(prefix) ? url.substring(prefix.length()) : null;
    }
}
