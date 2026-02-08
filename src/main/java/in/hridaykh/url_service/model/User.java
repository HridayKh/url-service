package in.hridaykh.url_service.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String email;

	private String profilePicture; // No @Column needed if name/defaults match

	private boolean isDeleted = false;

	private LocalDateTime deletedAt;

	@CreationTimestamp
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

}