CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table layout expected by Spring AI's PgVectorStore (default table name "vector_store").
-- Each row is one chunk; metadata carries user_id, book_id, book_title, page_number, file_name.
-- NOTE: the vector dimension must match the embedding model (text-embedding-3-small = 1536).
CREATE TABLE vector_store (
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1536)
);

CREATE INDEX idx_vector_store_embedding
    ON vector_store USING hnsw (embedding vector_cosine_ops);
