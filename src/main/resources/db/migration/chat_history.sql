-- SQL Migration Script for ChatHistory Table
-- This will be auto-created by JPA, but you can run this manually if needed

-- Create chat_history table
CREATE TABLE IF NOT EXISTS chat_history (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    user_id BIGINT,
    from_cache BOOLEAN,
    processing_time_ms BIGINT,
    confidence_score DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user"(user_id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_chat_history_session_id ON chat_history(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_user_id ON chat_history(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_created_at ON chat_history(created_at DESC);

-- Add comments
COMMENT ON TABLE chat_history IS 'Stores chat conversation history for AI chatbot';
COMMENT ON COLUMN chat_history.session_id IS 'Unique session identifier for conversation';
COMMENT ON COLUMN chat_history.question IS 'User question sent to chatbot';
COMMENT ON COLUMN chat_history.answer IS 'AI-generated response';
COMMENT ON COLUMN chat_history.user_id IS 'Reference to user who sent the message';
COMMENT ON COLUMN chat_history.from_cache IS 'Whether response came from Redis cache';
COMMENT ON COLUMN chat_history.processing_time_ms IS 'Time taken to process request in milliseconds';
COMMENT ON COLUMN chat_history.confidence_score IS 'AI confidence score for the response (0-1)';
