
INSERT INTO task (title, description, status)
SELECT 'Initial setup', 'Initialize project and repository structure', 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM task WHERE title = 'Initial setup');

INSERT INTO task (title, description, status)
SELECT 'Containerization', 'Dockerize services and run via Docker Compose', 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM task WHERE title = 'Containerization');

INSERT INTO task (title, description, status)
SELECT 'Service discovery', 'Ensure services register and resolve via Eureka', 'IN_PROGRESS'
WHERE NOT EXISTS (SELECT 1 FROM task WHERE title = 'Service discovery');

INSERT INTO task (title, description, status)
SELECT 'Gateway routing', 'Expose API via Gateway and route to tasks-service', 'DONE'
WHERE NOT EXISTS (SELECT 1 FROM task WHERE title = 'Gateway routing');

