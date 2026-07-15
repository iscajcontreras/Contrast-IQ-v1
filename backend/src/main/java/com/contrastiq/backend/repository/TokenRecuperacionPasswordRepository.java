package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.TokenRecuperacionPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenRecuperacionPasswordRepository extends JpaRepository<TokenRecuperacionPassword, Long> {
    Optional<TokenRecuperacionPassword> findByToken(String token);
}
