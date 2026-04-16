package com.back.together02be.global.idempotency;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

	private final IdempotencyKeyRepository idempotencyKeyRepository;

	/**
	 * 키 등록 시도.
	 * REQUIRES_NEW: 외부 트랜잭션과 독립 실행 — UNIQUE 위반 시 이 트랜잭션만 롤백
	 *
	 * @return true  → 처음 요청 (처리 허용)
	 *         false → 중복 요청 (차단)
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean registerIfAbsent(String key, Long userId) {
		try {
			idempotencyKeyRepository.save(new IdempotencyKey(key, userId));
			return true;
		} catch (DataIntegrityViolationException e) {
			return false;
		}
	}

	/**
	 * 처리 실패 시 키 반납 — 클라이언트 재시도 허용.
	 * 삭제 실패 시 예외 없이 로그만 남김.
	 */
	@Transactional
	public void remove(String key) {
		try {
			idempotencyKeyRepository.deleteByIdempotencyKey(key);
		} catch (Exception e) {
			log.warn("멱등성 키 삭제 실패: {}", key, e);
		}
	}

	/**
	 * 매일 자정 — 5분 이상 지난 키 정리.
	 */
	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void cleanupExpiredKeys() {
		LocalDateTime expiry = LocalDateTime.now().minusDays(1);
		idempotencyKeyRepository.deleteByCreatedAtBefore(expiry);
		log.info("멱등성 키 정리 완료 (1일 이전 키 삭제)");
	}
}
