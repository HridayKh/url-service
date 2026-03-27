package in.hridaykh.url_service.model.tables;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import in.hridaykh.url_service.model.enums.DeleteReason;
import in.hridaykh.url_service.model.enums.ExpiryType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

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
		}

		public void time(LocalDateTime expiryTime) {
			if (expiryTime == null)
				throw new IllegalArgumentException("Expiry time must be provided for TIME expiry type");
			URL.expiryType = ExpiryType.TIME;
			URL.expiryTime = expiryTime;
		}

		public void usage(Integer expiryMaxClicks) {
			if (expiryMaxClicks == null)
				throw new IllegalArgumentException(
						"Expiry max clicks must be provided for USAGE expiry type");
			URL.expiryType = ExpiryType.USAGE;
			URL.expiryMaxClicks = expiryMaxClicks;
		}

		public void inactivity(Long expiryInactivityDurationSeconds) {
			if (expiryInactivityDurationSeconds == null)
				throw new IllegalArgumentException(
						"Expiry inactivity duration must be provided for INACTIVITY expiry type");
			URL.expiryType = ExpiryType.INACTIVITY;
			URL.expiryInactivityDurationSeconds = expiryInactivityDurationSeconds;
		}

		public void inactivityDays(Long expiryInactivityDurationDays) {
			if (expiryInactivityDurationDays == null)
				throw new IllegalArgumentException(
						"Expiry inactivity duration days must be provided for INACTIVITY expiry type");
			this.inactivity(Duration.ofDays(expiryInactivityDurationDays).getSeconds());
		}
	}

}
