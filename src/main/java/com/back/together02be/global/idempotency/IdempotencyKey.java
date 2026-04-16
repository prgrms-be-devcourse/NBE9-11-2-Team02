package com.back.together02be.global.idempotency;

import com.back.together02be.global.entity.BaseEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
// 키는 생성 후 수정하지 않으므로 modifiedAt을 updatable=false 처리
@AttributeOverride(name = "modifiedAt", column = @Column(name = "modified_at", insertable = false, updatable = false))
public class IdempotencyKey extends BaseEntity {

	@Column(nullable = false, unique = true, length = 36)
	private String idempotencyKey;

	@Column(nullable = false)
	private Long userId;

	public IdempotencyKey(String idempotencyKey, Long userId) {
		this.idempotencyKey = idempotencyKey;
		this.userId = userId;
	}
}
