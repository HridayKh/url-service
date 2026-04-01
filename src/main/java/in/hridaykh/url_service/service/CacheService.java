package in.hridaykh.url_service.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import in.hridaykh.url_service.dtos.DeletedUrlsList;
import in.hridaykh.url_service.dtos.UrlRedirDTO;
import in.hridaykh.url_service.dtos.UrlsList;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.repository.ShortUrlRepository;

@Service
public class CacheService {
	private final Cache<String, UrlRedirDTO> urlRedirCache = Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.maximumSize(100)
			.build();

	private final Cache<Long, UrlsList[]> urlListCache = Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.maximumSize(100)
			.build();

	private final Cache<Long, DeletedUrlsList[]> deletedUrlList = Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.maximumSize(100)
			.build();

	private final ConcurrentHashMap<String, Integer> clickBuffer = new ConcurrentHashMap<>();

	private final UrlService urlService;
	private final ShortUrlRepository urlRepository;

	public CacheService(UrlService urlService, ShortUrlRepository urlRepository) {
		this.urlService = urlService;
		this.urlRepository = urlRepository;
	}

	public UrlRedirDTO getUrlForRedir(String shortCode) {
		// System.out.println("HANDLING CACHE FOR " + shortCode);
		UrlRedirDTO dto = urlRedirCache.get(shortCode, urlService::getUrlForRedir);
		if (!dto.hasPassword()) {
			// System.out.println("NO PASS");
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			// !!!!! WARN: PREPEND OR IT WONT RETURN AND WONT UPDATE THE COUNT !!!!!
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			clickBuffer.merge(shortCode, 1, (count, _) -> ++count);
		}
		// System.out.println("YES PASS");
		return dto;
	}

	@Scheduled(fixedDelay = 10000)
	public void updateClicksInDatabase() {
		// System.out.println("Flushing click buffer to database. Buffer size: " +
		// clickBuffer.size());
		if (clickBuffer.isEmpty())
			return;
		Map<String, Integer> snapshot = new HashMap<>(clickBuffer);
		snapshot.keySet().forEach(clickBuffer::remove);
		snapshot.forEach((code, count) -> {
			try {
				// System.out.println("Updating clicks for " + code + " with count " + count);
				urlRepository.incrementClicksByCode(code, count);
			} catch (Exception e) {
				System.err.println("Failed to update clicks for " + code);
			}
		});
	}

	public void invalidateUserCaches(long key) {
		urlListCache.invalidate(key);
		deletedUrlList.invalidate(key);
	}

	public void invalidateUrlRedir(String key) {
		urlRedirCache.invalidate(key);
	}

	public @Nullable Object getUserUrls(UserJwtPayload jwt) {
		return urlListCache.get(jwt.sub(), _ -> urlService.getUserUrls(jwt));
	}

	public DeletedUrlsList[] getDeletedUrls(UserJwtPayload jwt) {
		return deletedUrlList.get(jwt.sub(), _ -> urlService.getDeletedUrls(jwt));
	}
}
