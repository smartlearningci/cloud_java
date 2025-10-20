-- PostgreSQL schema (docker/prod)
CREATE TABLE IF NOT EXISTS task (
  id           BIGSERIAL PRIMARY KEY,
  title        VARCHAR(140) NOT NULL,
  description  TEXT,
  status       VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  assignee     VARCHAR(140),      -- NOVO
  project_id   BIGINT,            -- NOVO
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_task_status      ON task (status);
CREATE INDEX IF NOT EXISTS idx_task_created_at  ON task (created_at DESC);
