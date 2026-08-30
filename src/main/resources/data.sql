INSERT INTO rol (nombre) VALUES
    ('ADMIN'),
    ('USER')
ON CONFLICT (nombre) DO NOTHING;
