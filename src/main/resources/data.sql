-- 테스트용 시드 데이터 (H2 dev 환경 전용)

INSERT INTO users (username, password, nickname, created_at, modified_at)
VALUES ('testuser', 'password', '테스트유저', NOW(), NOW());

INSERT INTO stock (stock_code, stock_name, market, created_at, modified_at)
VALUES ('005930', '삼성전자', 'KOSPI', NOW(), NOW());

-- users_id=1 유저에게 5천만원 시드머니
INSERT INTO user_account (users_id, total_purchase, deposit, created_at, modified_at)
VALUES (1, 0, 50000000, NOW(), NOW());
