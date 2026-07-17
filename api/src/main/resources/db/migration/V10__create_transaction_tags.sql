CREATE TABLE transaction_tags (
    transaction_id UUID NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    tag            VARCHAR(40) NOT NULL,
    PRIMARY KEY (transaction_id, tag)
);

CREATE INDEX idx_transaction_tags_tag ON transaction_tags (tag);
