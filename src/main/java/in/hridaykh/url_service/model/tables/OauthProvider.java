package in.hridaykh.url_service.model.tables;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import in.hridaykh.url_service.model.enums.OauthProviderNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "oauth_providers", schema = "urldb")
public class OauthProvider {
	public OauthProvider() {
	}

	public OauthProvider(User user, OauthProviderNames providerName, String providerUserId, String providerPfp) {
		this.user = user;
		this.providerName = providerName;
		this.providerUserId = providerUserId;
		this.providerPfp = providerPfp;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_name", columnDefinition = "urldb.oauth_providers_provider_name")
	// @ColumnTransformer(write = "?::urldb.oauth_providers_provider_name")
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	private OauthProviderNames providerName;

	@Column(nullable = false)
	private String providerUserId;

	@Column
	private String providerPfp;

	@CreationTimestamp
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	public long getId() {
		return id;
	}
}
