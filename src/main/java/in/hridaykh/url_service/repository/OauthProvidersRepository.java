package in.hridaykh.url_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.tables.OauthProviders;

@Repository
public interface OauthProvidersRepository extends JpaRepository<OauthProviders, Long> {

	OauthProviders findByUser_IdAndProviderName(Long id, OauthProviderNames providerName);

}
