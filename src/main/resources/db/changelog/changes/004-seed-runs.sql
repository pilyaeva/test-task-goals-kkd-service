--liquibase formatted sql

--changeset polina:004-seed-runs context:seed
INSERT INTO runs (id, status, snapshot_date) VALUES
    ('RUN-001', 'COMPLETED', '2026-10-19'),
    ('RUN-002', 'COMPLETED', '2026-10-26');

-- RUN-001: 10 проблем (без сравнения)
-- RUN-002: 10 проблем; относительно RUN-001: новых 3, сохранившихся 7, исправленных 3
--   новые:        GOAL_NO_OWNER G-11, KR_WEIGHTS_SUM G-10, KR_STALE_PROGRESS G-13
--   исправленные: GOAL_NO_KR G-12, KR_WEIGHTS_SUM G-22, KR_STALE_PROGRESS G-24
INSERT INTO run_problems (id, run_id, rule_id, goal_id) VALUES
    ('PRB-001', 'RUN-001', 'RULE-001', 'G-04'),
    ('PRB-002', 'RUN-001', 'RULE-001', 'G-19'),
    ('PRB-003', 'RUN-001', 'RULE-002', 'G-05'),
    ('PRB-004', 'RUN-001', 'RULE-002', 'G-12'),
    ('PRB-005', 'RUN-001', 'RULE-002', 'G-19'),
    ('PRB-006', 'RUN-001', 'RULE-003', 'G-03'),
    ('PRB-007', 'RUN-001', 'RULE-003', 'G-18'),
    ('PRB-008', 'RUN-001', 'RULE-003', 'G-22'),
    ('PRB-009', 'RUN-001', 'RULE-004', 'G-20'),
    ('PRB-010', 'RUN-001', 'RULE-004', 'G-24'),
    ('PRB-011', 'RUN-002', 'RULE-001', 'G-04'),
    ('PRB-012', 'RUN-002', 'RULE-001', 'G-11'),
    ('PRB-013', 'RUN-002', 'RULE-001', 'G-19'),
    ('PRB-014', 'RUN-002', 'RULE-002', 'G-05'),
    ('PRB-015', 'RUN-002', 'RULE-002', 'G-19'),
    ('PRB-016', 'RUN-002', 'RULE-003', 'G-03'),
    ('PRB-017', 'RUN-002', 'RULE-003', 'G-10'),
    ('PRB-018', 'RUN-002', 'RULE-003', 'G-18'),
    ('PRB-019', 'RUN-002', 'RULE-004', 'G-13'),
    ('PRB-020', 'RUN-002', 'RULE-004', 'G-20');
--rollback DELETE FROM run_problems;
--rollback DELETE FROM runs;
