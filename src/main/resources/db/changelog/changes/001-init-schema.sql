--liquibase formatted sql

--changeset polina:001-init-schema
CREATE TABLE departments (
    id                   VARCHAR PRIMARY KEY,
    parent_department_id VARCHAR REFERENCES departments (id),
    name                 VARCHAR NOT NULL
);

CREATE TABLE employees (
    id            VARCHAR PRIMARY KEY,
    department_id VARCHAR NOT NULL REFERENCES departments (id),
    name          VARCHAR NOT NULL
);

CREATE TABLE goal_statuses (
    id   VARCHAR PRIMARY KEY,
    name VARCHAR NOT NULL
);

CREATE TABLE goals (
    id                   VARCHAR PRIMARY KEY,
    parent_goal_id       VARCHAR REFERENCES goals (id),
    assignee_employee_id VARCHAR REFERENCES employees (id),
    status_id            VARCHAR NOT NULL REFERENCES goal_statuses (id),
    end_date             DATE
);

CREATE TABLE key_results (
    id           VARCHAR PRIMARY KEY,
    goal_id      VARCHAR NOT NULL REFERENCES goals (id),
    target_value NUMERIC,
    fact_value   NUMERIC,
    weight       NUMERIC,
    updated_date DATE
);

CREATE TABLE rules (
    id       VARCHAR PRIMARY KEY,
    code     VARCHAR NOT NULL,
    severity VARCHAR NOT NULL
);

CREATE TABLE runs (
    id            VARCHAR PRIMARY KEY,
    status        VARCHAR NOT NULL,
    snapshot_date DATE    NOT NULL
);

CREATE TABLE run_problems (
    id      VARCHAR PRIMARY KEY,
    run_id  VARCHAR NOT NULL REFERENCES runs (id),
    rule_id VARCHAR NOT NULL REFERENCES rules (id),
    goal_id VARCHAR NOT NULL REFERENCES goals (id)
);

CREATE INDEX idx_run_problems_run_id ON run_problems (run_id);
--rollback DROP INDEX idx_run_problems_run_id;
--rollback DROP TABLE run_problems;
--rollback DROP TABLE runs;
--rollback DROP TABLE rules;
--rollback DROP TABLE key_results;
--rollback DROP TABLE goals;
--rollback DROP TABLE goal_statuses;
--rollback DROP TABLE employees;
--rollback DROP TABLE departments;
