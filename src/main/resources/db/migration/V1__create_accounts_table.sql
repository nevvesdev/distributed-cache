CREATE TABLE accounts (
    id           UUID                     PRIMARY KEY,
    balance      NUMERIC(19, 2)           NOT NULL,
    credit_limit NUMERIC(19, 2)           NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_accounts_credit_limit_non_negative CHECK (credit_limit >= 0)
);
