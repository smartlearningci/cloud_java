-- Phase-5: PostgreSQL schema for tasks-service (idempotent, inside JAR)

CREATE TABLE IF NOT EXISTS task (
  id           BIGSERIAL PRIMARY KEY,
  title        VARCHAR(140) NOT NULL,
  description  TEXT,
  status       VARCHAR(32) NOT NULL DEFAULT 'OPEN', -- OPEN | IN_PROGRESS | DONE
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Índices úteis (idempotentes)
CREATE INDEX IF NOT EXISTS idx_task_status      ON task (status);
CREATE INDEX IF NOT EXISTS idx_task_created_at  ON task (created_at DESC);

-- (Opcional) trigger para updated_at; deixar comentado se não precisares
-- DO $$ BEGIN
--   IF NOT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'task_set_updated_at') THEN
--     CREATE OR REPLACE FUNCTION task_set_updated_at()
--     RETURNS TRIGGER AS $$
--     BEGIN
--       NEW.updated_at = NOW();
--       RETURN NEW;
--     END;
--     $$ LANGUAGE plpgsql;
--   END IF;
-- END $$;
--
-- DO $$ BEGIN
--   IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_task_updated_at') THEN
--     CREATE TRIGGER trg_task_updated_at
--     BEFORE UPDATE ON task
--     FOR EACH ROW
--     EXECUTE FUNCTION task_set_updated_at();
--   END IF;
-- END $$;

-- Phase-5: Seed data for tasks-service (idempotent, inside JAR)
