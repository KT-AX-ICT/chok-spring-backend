-- prod(MySQL) 마이그레이션: log_analysis.log_id 1:1(UNIQUE) 전환.
-- 배경: log_analysis ↔ bgl_log가 N:1이라 scheduler/startup 동시 분석 시 같은 로그가 중복 적재됨.
--       엔티티를 @OneToOne(unique)로 바꿔도 prod는 ddl-auto=validate라 제약이 자동 생성되지 않으므로 수동 적용한다.
-- 순서: (1) dedupe → (2) ADD UNIQUE. dedupe 없이 ADD UNIQUE 하면 중복 때문에 실패한다.
-- 멱등성 주의: dedupe는 1회성. UNIQUE 적용 후엔 중복이 다시 생기지 않는다(이게 이 마이그레이션의 목적).

-- (1) dedupe: 각 log_id에서 최신 analyzed_at(동률이면 큰 id) 한 건만 남기고 나머지 제거.
DELETE a1 FROM log_analysis a1
JOIN log_analysis a2
  ON a1.log_id = a2.log_id
 AND (a1.analyzed_at < a2.analyzed_at
   OR (a1.analyzed_at = a2.analyzed_at AND a1.id < a2.id));

-- (2) UNIQUE 제약 추가. 이후 중복 INSERT는 제약위반으로 차단된다(멱등성 안전망).
ALTER TABLE log_analysis
  ADD CONSTRAINT uq_log_analysis_log_id UNIQUE (log_id);

-- 검증: 중복이 0건이어야 한다.
SELECT log_id, COUNT(*) AS cnt
FROM log_analysis
GROUP BY log_id
HAVING COUNT(*) > 1;
