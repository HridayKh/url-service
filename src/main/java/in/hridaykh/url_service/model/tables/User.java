package in.hridaykh.url_service.model.tables;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
	public User() {
	}

	public User(String email, String profilePicture) {
		this.email = email;
		this.profilePicture = profilePicture;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	@Column
	private String profilePicture;

	@Column
	private boolean isDeleted = false;

	@Column
	private LocalDateTime deletedAt;

	@CreationTimestamp
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	public long getId() {
		return id;
	}

	public String getProfilePicture() {
		return this.profilePicture;
	}

	public @Nullable String getEmail() {
		return this.email;
	}
}