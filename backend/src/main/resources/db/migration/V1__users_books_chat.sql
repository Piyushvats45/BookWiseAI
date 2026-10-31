CREATE TABLE users (
    id            uuid PRIMARY KEY,
    email         varchar(255) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    display_name  varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL
);

CREATE TABLE books (
    id                uuid PRIMARY KEY,
    user_id           uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title             varchar(300) NOT NULL,
    original_filename varchar(300) NOT NULL,
    file_path         varchar(600) NOT NULL,
    size_bytes        bigint       NOT NULL,
    sha256            varchar(64)  NOT NULL,
    page_count        integer      NOT NULL DEFAULT 0,
    chunk_count       integer      NOT NULL DEFAULT 0,
    status            varchar(20)  NOT NULL,
    error_message     varchar(500),
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    CONSTRAINT uq_books_user_sha UNIQUE (user_id, sha256)
);

CREATE INDEX idx_books_user ON books (user_id, created_at DESC);

CREATE TABLE chat_messages (
    id         uuid PRIMARY KEY,
    book_id    uuid        NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       varchar(12) NOT NULL,
    content    text        NOT NULL,
    citations  text,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_chat_book_user ON chat_messages (book_id, user_id, created_at DESC);
