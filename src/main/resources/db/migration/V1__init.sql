CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(2000),
    personal BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE team_memberships (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_team_member UNIQUE (team_id, user_id)
);

CREATE INDEX idx_membership_user ON team_memberships (user_id);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    due_date DATE,
    status VARCHAR(20) NOT NULL,
    creator_id UUID NOT NULL REFERENCES users (id),
    assignee_id UUID REFERENCES users (id),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tasks_team ON tasks (team_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);
CREATE INDEX idx_tasks_status ON tasks (status);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users (id),
    body VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_comments_task ON comments (task_id);

CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    uploader_id UUID NOT NULL REFERENCES users (id),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_attachments_task ON attachments (task_id);
