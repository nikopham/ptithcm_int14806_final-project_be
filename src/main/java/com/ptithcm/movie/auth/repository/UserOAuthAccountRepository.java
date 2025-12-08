package com.ptithcm.movie.auth.repository;

import com.ptithcm.movie.auth.entity.UserOauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOAuthAccountRepository extends JpaRepository<UserOauthAccount, UUID> {


    Optional<UserOauthAccount> findByProvider_IdAndProviderUserId(Integer providerId, String providerUserId);

}
