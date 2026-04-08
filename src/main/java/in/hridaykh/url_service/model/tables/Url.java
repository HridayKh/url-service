package in.hridaykh.url_service.model.tables;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import in.hridaykh.url_service.dtos.urls.DeletedUrlsList;
import in.hridaykh.url_service.dtos.UrlRedirDTO;
import in.hridaykh.url_service.dtos.urls.UrlEditDTO;
import in.hridaykh.url_service.dtos.urls.UrlsList;
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

	// ---------- CREATE ----------

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

	// ---------- READ ----------

	public boolean isUsable() {
		return !isDeleted && isActive;
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

	public boolean verifyUserOwnership(long userId) {
		return user != null && userId == user.getId();
	}

	// ---------- UPDATE ----------

	public void incrementClicksCount(LocalDateTime now) {
		this.clickCount++;
		this.lastClickedAt = now;
	}

	public void update(User user, String originalUrl, String shortUrl, String passHash) {
		this.user = user;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.passwordHash = passHash;
	}

	// ---------- DELETE ----------

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

	public void markAsDeleted(LocalDateTime now, DeleteReason deleteReason) {
		this.isDeleted = true;
		this.isActive = false;
		this.deletedAt = now;
		this.deleteReason = deleteReason;
	}

	// ---------- SUB-CLASS: EXPIRY ----------

	public UrlExpiry UrlExpiry() {
		return new UrlExpiry(this);
	}

	public class UrlExpiry {
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

	// ---------- SUB-CLASS: DTOs ----------

	public AsDTO AsDTO() {
		return new AsDTO(this);
	}

	public class AsDTO {
		Url URL;

		public AsDTO(Url URL_) {
			this.URL = URL_;
		}

		public UrlEditDTO urlEditDTO() {
			boolean hasPassword = URL.passwordHash != null;
			String expiryType = URL.expiryType != null ? URL.expiryType.name() : ExpiryType.NONE.name();
			String expiryTime = URL.expiryTime != null ? String.valueOf(URL.expiryTime) : null;
			Integer expiryMaxClicks = URL.expiryTime != null ? URL.expiryMaxClicks : null;

			Long expInactiveDurDays = null;
			if (URL.expiryInactivityDurationSeconds != null)
				expInactiveDurDays = Duration.ofSeconds(URL.expiryInactivityDurationSeconds).toDays();

			return new UrlEditDTO(URL.id, URL.originalUrl, URL.shortUrl, hasPassword, expiryType,
					expiryTime, expiryMaxClicks, expInactiveDurDays);
		}

		public UrlRedirDTO urlRedirDTO() {
			if (URL.passwordHash != null && URL.passwordHash.isBlank())
				URL.passwordHash = null;
			return new UrlRedirDTO(URL.passwordHash != null, URL.originalUrl);
		}

		public UrlsList urlList(String domain) {
			String id = String.valueOf(URL.id);
			String displayUrl = domain + URL.shortUrl;
			String fullLink = "https://" + domain + URL.shortUrl;
			String originalUrl = URL.originalUrl;
			String lastClicked = URL.lastClickedAt != null ? String.valueOf(URL.lastClickedAt) : "Never";
			int clickCount = URL.clickCount;
			return new UrlsList(id, displayUrl, fullLink, originalUrl, lastClicked, clickCount);
		}

		public DeletedUrlsList deletedUrlList(String domain) {
			String id = String.valueOf(URL.id);
			String displayUrl = domain + URL.shortUrl;
			String fullLink = "https://" + domain + URL.shortUrl;
			String originalUrl = URL.originalUrl;
			String deleteReason = URL.deleteReason != null ? URL.deleteReason.name() : "Unknown";
			String deletedAt = URL.deletedAt != null ? String.valueOf(URL.deletedAt) : "Unknown";
			return new DeletedUrlsList(id, displayUrl, fullLink, originalUrl, deleteReason, deletedAt);
		}

	}

	public boolean verifyPassword(String passHash) {
		return MessageDigest.isEqual(passHash.getBytes(), this.passwordHash.getBytes());
	}

}
