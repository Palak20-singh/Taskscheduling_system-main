-- ProManage DB Schema
-- Run this on promanage_db database

-- Drop tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS schedule_entries CASCADE;
DROP TABLE IF EXISTS schedules CASCADE;
DROP TABLE IF EXISTS projects CASCADE;

-- =====================================================
-- Table: projects
-- Stores all project records with AI predictions
-- =====================================================
CREATE TABLE projects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    deadline DATE NOT NULL,
    complexity INTEGER CHECK (complexity BETWEEN 1 AND 10),
    
    -- Original fields from input (4 fields)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- AI-predicted fields (11 fields)
    delay_rate DECIMAL(5,2),  -- Predicted delay probability as percentage
    on_time BOOLEAN,           -- Will project finish on time?
    final_revenue DECIMAL(12,2), -- Predicted final revenue
    priority_score DECIMAL(3,2), -- AI-calculated priority (0-10 scale)
    risk_level VARCHAR(20),      -- Low/Medium/High
    estimated_duration INTEGER,  -- Days estimated to complete
    resource_allocation JSONB,   -- Resource requirements as JSON
    team_size INTEGER,           -- Recommended team size
    skill_requirements TEXT[],   -- Array of required skills
    milestone_dates JSONB,       -- Predicted milestone dates
    confidence_score DECIMAL(3,2), -- AI confidence in predictions
    
    -- Metadata
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Table: schedules
-- Stores each generated weekly schedule header
-- =====================================================
CREATE TABLE schedules (
    id SERIAL PRIMARY KEY,
    generation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    algorithm_used VARCHAR(50) DEFAULT 'Greedy',
    total_projects INTEGER,
    total_revenue DECIMAL(12,2),
    on_time_count INTEGER,
    delayed_count INTEGER,
    week_start_date DATE,
    week_end_date DATE,
    is_active BOOLEAN DEFAULT true
);

-- =====================================================
-- Table: schedule_entries
-- Stores which project is assigned to which day
-- =====================================================
CREATE TABLE schedule_entries (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER REFERENCES schedules(id) ON DELETE CASCADE,
    project_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
    day_of_week INTEGER CHECK (day_of_week BETWEEN 1 AND 5), -- 1=Monday, 5=Friday
    day_name VARCHAR(10),
    position INTEGER, -- Order within the day
    notes TEXT,
    UNIQUE(schedule_id, day_of_week, position)
);

-- Create indexes for better performance
CREATE INDEX idx_projects_deadline ON projects(deadline);
CREATE INDEX idx_projects_complexity ON projects(complexity);
CREATE INDEX idx_schedule_entries_schedule ON schedule_entries(schedule_id);
CREATE INDEX idx_schedule_entries_project ON schedule_entries(project_id);

-- Add a view for easy schedule visualization
CREATE OR REPLACE VIEW weekly_schedule_view AS
SELECT 
    s.id as schedule_id,
    s.week_start_date,
    s.week_end_date,
    se.day_of_week,
    se.day_name,
    se.position,
    p.id as project_id,
    p.title,
    p.type,
    p.deadline,
    p.complexity
FROM schedules s
JOIN schedule_entries se ON s.id = se.schedule_id
JOIN projects p ON se.project_id = p.id
ORDER BY s.id, se.day_of_week, se.position;

-- Success message
SELECT 'ProManage DB schema created successfully' as message;
