package in.hridaykh.url_service.model;

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

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "urls", indexes = {
		@Index(name = "idx_short_url", columnList = "short_url", unique = true)
})
public class ShortUrl {

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

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public void setShortUrl(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public void setExpiryType(ExpiryType expiryType) {
		this.expiryType = expiryType;
	}

	public void setExpiryInactivityDurationSeconds(long expiryInactivityDurationSeconds) {
		this.expiryInactivityDurationSeconds = expiryInactivityDurationSeconds;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public void setLastClickedAt(LocalDateTime now) {
		this.lastClickedAt = now;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void incrementClicksCount() {
		this.clickCount++;
	}

}
