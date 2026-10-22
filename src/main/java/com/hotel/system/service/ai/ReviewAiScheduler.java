package com.hotel.system.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewAiScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewAiScheduler.class);

    private final ReviewAiExportService reviewAiExportService;

    public ReviewAiScheduler(ReviewAiExportService reviewAiExportService) {
        this.reviewAiExportService = reviewAiExportService;
    }

    @Scheduled(
            fixedDelayString = "${ai.review.export.fixed-delay-ms:604800000}",
            initialDelayString = "${ai.review.export.initial-delay-ms:60000}"
    )
    public void exportReviewAnalysisPeriodically() {
        ReviewAiExportService.ReviewAiExportSummary summary = reviewAiExportService.exportLatestReviews();
        log.info("[review-ai-export] totalDb={}, inserted={}, updated={}, skipped={}, totalFile={}, file={}",
                summary.totalReviewsInDb(),
                summary.inserted(),
                summary.updated(),
                summary.skipped(),
                summary.totalRecordsInFile(),
                summary.outputFile());
    }
}
