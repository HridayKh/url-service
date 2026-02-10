package in.hridaykh.url_service.model.tables;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "oauth_providers")
public class OauthProviders {
	public OauthProviders() {
	}

	public OauthProviders(Users user, OauthProviderNames providerName, long providerUserId, String providerPfp) {
		this.user = user;
		this.providerName = providerName;
		this.provider_user_id = providerUserId;
		this.provider_pfp = providerPfp;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_name")
	private OauthProviderNames providerName;

	@Column(nullable = false)
	private long provider_user_id;

	@Column
	private String provider_pfp;

	@CreationTimestamp
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	public long getId() {
		return id;
	}
}
