--  Task Scheduling System — Database Schema
--
--  HOW TO RUN:
--    1. Open psql or pgAdmin connected to your PostgreSQL
--    2. Create database if not exists:
--         CREATE DATABASE promanage_db;
--    3. Connect to it and run this file:
--         \i schema.sql
--
--  This is safe to re-run — it drops and recreates all tables.
-- ============================================================

-- Drop in reverse dependency order
DROP TABLE IF EXISTS schedule_entries CASCADE;
DROP TABLE IF EXISTS schedules        CASCADE;
DROP TABLE IF EXISTS projects         CASCADE;
DROP SEQUENCE IF EXISTS project_id_seq;

-- ──────────────────────────────────────────────────────────────
--  SEQUENCE  —  generates PRJ-0001, PRJ-0002, PRJ-0003 …
--  Used by Java ProjectDAO.save() to create unique project IDs
-- ──────────────────────────────────────────────────────────────
CREATE SEQUENCE project_id_seq
    START WITH 1
    INCREMENT BY 1;

-- ──────────────────────────────────────────────────────────────
--  TABLE: projects
--
--  Columns 1–4  : entered by the project manager via the web form
--  Columns 5–13 : filled by the Python ML /predict API
--                 (Java calls Python, gets predictions, stores them here)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE projects (
    -- Manager input
    project_id                VARCHAR(10)    PRIMARY KEY,      -- PRJ-0001, PRJ-0002 …
    title                     VARCHAR(200)   NOT NULL,
    project_type              VARCHAR(50)    NOT NULL,         -- Web Development, Mobile App, etc.
    deadline                  INT            NOT NULL
                                CHECK (deadline BETWEEN 1 AND 5),  -- Day 1=Mon … Day 5=Fri
    base_revenue              NUMERIC(15,2)  NOT NULL
                                CHECK (base_revenue > 0),

    -- ML predictions from Python FastAPI /predict
    complexity_level          VARCHAR(20),                     -- Low / Medium / High
    client_priority           VARCHAR(20),                     -- Low / Medium / High
    team_experience_level     VARCHAR(20),                     -- Junior / Mid / Senior
    predicted_delay_rate      VARCHAR(10),                     -- e.g. "30%" (stored as string)
    predicted_completion_days NUMERIC(10,2),                   -- e.g. 7.50 days  ← FIXED: was INT, predictor returns decimals
    delay_days                VARCHAR(20),                     -- "No Delay" or "3 days"
    completed_on_time         VARCHAR(5),                      -- "Yes" or "No"
    final_revenue_realized    NUMERIC(15,2),                   -- base_revenue minus delay penalty

    created_at                TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- ──────────────────────────────────────────────────────────────
--  TABLE: schedules
--  One row per weekly schedule generation.
--  Stores the total revenue achieved by that schedule.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE schedules (
    schedule_id    SERIAL         PRIMARY KEY,
    generated_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    total_revenue  NUMERIC(15,2)  NOT NULL
);

-- ──────────────────────────────────────────────────────────────
--  TABLE: schedule_entries
--  One row per day slot in a schedule (Mon–Fri = up to 5 rows).
--  Links a schedule → day → project.
-- ──────────────────────────────────────────────────────────────
CREATE TABLE schedule_entries (
    entry_id      SERIAL         PRIMARY KEY,
    schedule_id   INT            NOT NULL
                    REFERENCES schedules(schedule_id) ON DELETE CASCADE,
    day_number    INT            NOT NULL CHECK (day_number BETWEEN 1 AND 5),
    day_name      VARCHAR(10)    NOT NULL,     -- Monday, Tuesday, etc.
    project_id    VARCHAR(10)    NOT NULL
                    REFERENCES projects(project_id),
    revenue       NUMERIC(15,2)  NOT NULL      -- final_revenue_realized for this project
);

-- ──────────────────────────────────────────────────────────────
--  VIEW: v_latest_schedule
--  Convenience view for checking the latest schedule in pgAdmin.
-- ──────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_latest_schedule AS
SELECT
    se.day_number,
    se.day_name,
    se.project_id,
    p.title,
    p.project_type,
    p.deadline,
    p.complexity_level,
    p.predicted_delay_rate,
    p.predicted_completion_days,
    p.completed_on_time,
    se.revenue AS final_revenue_realized
FROM schedule_entries se
JOIN projects p ON p.project_id = se.project_id
WHERE se.schedule_id = (SELECT MAX(schedule_id) FROM schedules)
ORDER BY se.day_number;

SELECT 'ProManage DB schema created successfully' AS status;