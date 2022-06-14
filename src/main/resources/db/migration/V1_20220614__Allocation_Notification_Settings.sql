CREATE TABLE allocation_notification_settings (
    id UUID NOT NULL,
    allocation_id UUID NOT NULL,
    low_balance BOOLEAN NOT NULL DEFAULT false,
    low_balance_level_amount NUMERIC NOT NULL DEFAULT 0,
    low_balance_level_currency VARCHAR(10) NOT NULL,
    recipients UUID[] NOT NULL DEFAULT ARRAY[]::UUID[],
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (allocation_id) REFERENCES allocation (id),
    UNIQUE (allocation_id)
);