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
	@Column(name = "provider_name")
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
