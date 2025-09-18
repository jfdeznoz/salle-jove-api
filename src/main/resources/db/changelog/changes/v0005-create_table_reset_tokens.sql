CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(200) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

-- índices recomendados
CREATE INDEX idx_password_reset_token_email ON password_reset_tokens (email);
CREATE INDEX idx_password_reset_token_expires_at ON password_reset_tokens (expires_at);