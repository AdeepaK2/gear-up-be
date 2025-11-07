-- Create notifications table for SSE-based notification system
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for optimized queries
CREATE INDEX IF NOT EXISTS idx_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_user_read ON notifications(user_id, is_read);

-- Add comments for documentation
COMMENT ON TABLE notifications IS 'Stores user notifications for real-time SSE delivery';
COMMENT ON COLUMN notifications.user_id IS 'ID of the user who will receive the notification';
COMMENT ON COLUMN notifications.type IS 'Notification type (e.g., APPOINTMENT, PROJECT_UPDATE, TASK_ASSIGNED)';
COMMENT ON COLUMN notifications.is_read IS 'Whether the notification has been read by the user';
