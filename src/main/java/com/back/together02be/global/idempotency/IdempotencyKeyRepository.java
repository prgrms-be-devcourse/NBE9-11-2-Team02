package com.back.together02be.global.idempotency;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

	boolean existsByIdempotencyKey(String idempotencyKey);

	void deleteByIdempotencyKey(String idempotencyKey);

	void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
