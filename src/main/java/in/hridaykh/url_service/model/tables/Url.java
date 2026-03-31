package in.hridaykh.url_service.model.tables;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import in.hridaykh.url_service.dtos.DeletedUrlsList;
import in.hridaykh.url_service.dtos.UrlEditDTO;
import in.hridaykh.url_service.dtos.UrlsList;
import in.hridaykh.url_service.model.enums.DeleteReason;
import in.hridaykh.url_service.model.enums.ExpiryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "urls", indexes = {
		@Index(name = "idx_short_url", columnList = "short_url", unique = true)
})
public class Url {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String originalUrl;

	@Column(unique = true, nullable = false)
	private String shortUrl;

	@Column
	private String passwordHash;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "json")
	private Map<String, Object> qrMetadata;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ExpiryType expiryType;

	@Column
	private LocalDateTime expiryTime;

	@Column
	private Integer expiryMaxClicks;

	@Column
	private Long expiryInactivityDurationSeconds;

	@Column(nullable = false)
	private int clickCount = 0;

	@Column
	private LocalDateTime lastClickedAt;

	@Column(nullable = false)
	private boolean isActive = true;

	@Column(nullable = false)
	private boolean isDeleted = false;

	@Column
	private LocalDateTime deletedAt;

	@Enumerated(EnumType.STRING)
	private DeleteReason deleteReason;

	@CreationTimestamp
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	public String originalUrl() {
		return originalUrl;
	}

	public void incrementClicksCount(LocalDateTime now) {
		this.clickCount++;
		this.lastClickedAt = now;
	}

	public boolean isExpired(LocalDateTime now) {
		switch (expiryType) {
			case TIME:
				return expiryTime != null && now.isAfter(expiryTime);
			case USAGE:
				return expiryMaxClicks != null && clickCount >= expiryMaxClicks;
			case INACTIVITY: {
				LocalDateTime lastClickedAt = this.lastClickedAt;
				if (lastClickedAt == null)
					lastClickedAt = createdAt;

				return expiryInactivityDurationSeconds != null
						&& lastClickedAt.plusSeconds(expiryInactivityDurationSeconds)
								.isBefore(now);
			}
			default:
				return false;
		}
	}

	public void markAsDeleted(LocalDateTime now, DeleteReason deleteReason) {
		this.isDeleted = true;
		this.isActive = false;
		this.deletedAt = now;
		this.deleteReason = deleteReason;
	}

	public boolean isUsable() {
		return !isDeleted && isActive;
	}

	public void createAnonUrl(String originalUrl, String shortUrlCode) {
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrlCode;
		this.expiryType = ExpiryType.INACTIVITY;
		this.expiryInactivityDurationSeconds = Duration.ofDays(365).getSeconds();

	}

	public void createUserUrl(User user, String originalUrl, String shortUrlCode,
			String passwordHash) {
		this.user = user;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrlCode;
		this.passwordHash = passwordHash;
	}

	public UrlsList toUrlList(String domain) {
		String id = String.valueOf(this.id);
		String displayUrl = domain + this.shortUrl;
		String fullLink = "https://" + domain + this.shortUrl;
		String originalUrl = this.originalUrl;
		String lastClicked = this.lastClickedAt != null ? String.valueOf(this.lastClickedAt) : "Never";
		int clickCount = this.clickCount;
		return new UrlsList(id, displayUrl, fullLink, originalUrl, lastClicked, clickCount);
	}

	public DeletedUrlsList toDeletedUrlList(String domain) {
		String id = String.valueOf(this.id);
		String displayUrl = domain + this.shortUrl;
		String fullLink = "https://" + domain + this.shortUrl;
		String originalUrl = this.originalUrl;
		String deleteReason = this.deleteReason != null ? this.deleteReason.name() : "Unknown";
		String deletedAt = this.deletedAt != null ? String.valueOf(this.deletedAt) : "Unknown";
		return new DeletedUrlsList(id, displayUrl, fullLink, originalUrl, deleteReason, deletedAt);
	}

	public void markAsRestored(LocalDateTime now) {
		this.isDeleted = false;
		this.isActive = true;
		this.deletedAt = null;
		this.deleteReason = null;
		this.expiryType = ExpiryType.NONE;
		this.expiryTime = null;
		this.expiryMaxClicks = null;
		this.expiryInactivityDurationSeconds = null;
	}

	public boolean verifyUserOwnership(long userId) {
		return user != null && userId == user.getId();
	}

	public UrlExpiry UrlExpiry() {
		return new UrlExpiry(this);
	}

	public static class UrlExpiry {
		Url URL;

		public UrlExpiry(Url URL_) {
			this.URL = URL_;
		}

		public void none() {
			URL.expiryType = ExpiryType.NONE;
			URL.expiryTime = null;
			URL.expiryMaxClicks = null;
			URL.expiryInactivityDurationSeconds = null;
		}

		public void time(LocalDateTime expiryTime) {
			if (expiryTime == null)
				throw new IllegalArgumentException("Expiry time must be provided for TIME expiry type");
			URL.expiryType = ExpiryType.TIME;
			URL.expiryTime = expiryTime;
			URL.expiryMaxClicks = null;
			URL.expiryInactivityDurationSeconds = null;
		}

		public void usage(Integer expiryMaxClicks) {
			if (expiryMaxClicks == null)
				throw new IllegalArgumentException(
						"Expiry max clicks must be provided for USAGE expiry type");
			URL.expiryType = ExpiryType.USAGE;
			URL.expiryMaxClicks = expiryMaxClicks;
			URL.expiryTime = null;
			URL.expiryInactivityDurationSeconds = null;
		}

		public void inactivitySeconds(Long expiryInactivityDurationSeconds) {
			if (expiryInactivityDurationSeconds == null)
				throw new IllegalArgumentException(
						"Expiry inactivity duration must be provided for INACTIVITY expiry type");
			URL.expiryType = ExpiryType.INACTIVITY;
			URL.expiryInactivityDurationSeconds = expiryInactivityDurationSeconds;
			URL.expiryTime = null;
			URL.expiryMaxClicks = null;
		}

		public void inactivityDays(Long expiryInactivityDurationDays) {
			if (expiryInactivityDurationDays == null)
				throw new IllegalArgumentException(
						"Expiry inactivity duration days must be provided for INACTIVITY expiry type");
			this.inactivitySeconds(Duration.ofDays(expiryInactivityDurationDays).getSeconds());
		}
	}

	public UrlEditDTO toUrlEditDTO() {
		Long id = this.id;
		String originalUrl = this.originalUrl;
		String shortUrl = this.shortUrl;
		boolean hasPassword = this.passwordHash != null;
		String expiryType = this.expiryType != null ? this.expiryType.name() : ExpiryType.NONE.name();
		String expiryTime = this.expiryTime != null ? String.valueOf(this.expiryTime) : null;
		Integer expiryMaxClicks = this.expiryTime != null ? this.expiryMaxClicks : null;

		Long expiryInactivityDurationDays = this.expiryInactivityDurationSeconds != null
				? Duration.ofSeconds(this.expiryInactivityDurationSeconds).toDays()
				: null;

		return new UrlEditDTO(id, originalUrl, shortUrl, hasPassword, expiryType, expiryTime, expiryMaxClicks,
				expiryInactivityDurationDays);
	}

	public void getAndUpdate(User user, Long id, String originalUrl, String shortUrl, String passHash) {
		this.user = user;
		this.id = id;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.passwordHash = passHash;
	}

}
