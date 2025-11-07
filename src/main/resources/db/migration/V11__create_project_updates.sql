-- Create project_updates table
CREATE TABLE IF NOT EXISTS project_updates (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    message VARCHAR(2000) NOT NULL,
    completed_tasks INTEGER,
    total_tasks INTEGER,
    additional_cost DECIMAL(10, 2),
    additional_cost_reason VARCHAR(500),
    estimated_completion_date DATE,
    update_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_update_project FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    CONSTRAINT fk_project_update_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE
);

-- Create index for faster queries
CREATE INDEX idx_project_updates_project_id ON project_updates(project_id);
CREATE INDEX idx_project_updates_employee_id ON project_updates(employee_id);
CREATE INDEX idx_project_updates_created_at ON project_updates(created_at DESC);
