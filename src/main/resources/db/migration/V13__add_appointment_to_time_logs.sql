-- Add appointment_id column to time_logs table to support logging work for appointments
ALTER TABLE time_logs
ADD COLUMN appointment_id BIGINT,
ADD CONSTRAINT fk_time_logs_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id);

-- Make task_id and project_id nullable since time logs can now be for appointments instead
ALTER TABLE time_logs
ALTER COLUMN task_id DROP NOT NULL,
ALTER COLUMN project_id DROP NOT NULL;

-- Add check constraint to ensure either (task_id AND project_id) OR appointment_id is set
ALTER TABLE time_logs
ADD CONSTRAINT check_time_log_reference 
CHECK (
    (task_id IS NOT NULL AND project_id IS NOT NULL AND appointment_id IS NULL) 
    OR 
    (task_id IS NULL AND project_id IS NULL AND appointment_id IS NOT NULL)
);
