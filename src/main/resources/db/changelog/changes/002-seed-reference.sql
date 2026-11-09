--liquibase formatted sql

--changeset polina:002-seed-reference context:seed
-- Справочники: статусы целей и правила ККД (реализованы 4 правила из 6)
INSERT INTO goal_statuses (id, name) VALUES
    ('ACTIVE',    'Активна'),
    ('DRAFT',     'Черновик'),
    ('DONE',      'Завершена'),
    ('CANCELLED', 'Отменена');

INSERT INTO rules (id, code, severity) VALUES
    ('RULE-001', 'GOAL_NO_OWNER',      'critical'),
    ('RULE-002', 'GOAL_NO_KR',         'critical'),
    ('RULE-003', 'KR_WEIGHTS_SUM',     'major'),
    ('RULE-004', 'KR_STALE_PROGRESS',  'minor');
--rollback DELETE FROM rules;
--rollback DELETE FROM goal_statuses;
