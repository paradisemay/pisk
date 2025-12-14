CREATE TABLE IF NOT EXISTS audit_log(
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(32) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    occurred_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_metadata(
    audit_id BIGINT NOT NULL REFERENCES audit_log(id) ON DELETE CASCADE,
    meta_key VARCHAR(255) NOT NULL,
    meta_value VARCHAR(255),
    PRIMARY KEY (audit_id, meta_key)
);
