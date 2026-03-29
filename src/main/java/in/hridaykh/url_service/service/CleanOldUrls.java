package in.hridaykh.url_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import in.hridaykh.url_service.repository.ShortUrlRepository;

@Component
public class CleanOldUrls {

	private final ShortUrlRepository shortUrlRepository;

	public CleanOldUrls(ShortUrlRepository shortUrlRepository) {
		this.shortUrlRepository = shortUrlRepository;
	}

	@Scheduled(fixedDelay = 1 * 60 * 60 * 1000, initialDelay = 10 * 1000)
	public void cleanUp() {
		System.out.println("Running scheduled task to clean up old URLs...");
		shortUrlRepository.softDeleteExpiredUrls();
		shortUrlRepository.hardDeleteOldUrls();
		System.out.println("Finished cleaning up old URLs.");
	}

}
