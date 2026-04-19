CREATE TABLE weekly_session_behavior_warning (
    uuid UUID PRIMARY KEY,
    weekly_session_user_uuid UUID NOT NULL UNIQUE,
    warning_type VARCHAR(20) NOT NULL,
    comment TEXT NOT NULL,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_weekly_session_behavior_warning_session_user
        FOREIGN KEY (weekly_session_user_uuid) REFERENCES weekly_session_user(uuid),
    CONSTRAINT fk_weekly_session_behavior_warning_created_by
        FOREIGN KEY (created_by_user_uuid) REFERENCES user_salle(uuid),
    CONSTRAINT chk_weekly_session_behavior_warning_type
        CHECK (warning_type IN ('YELLOW', 'RED'))
);

CREATE INDEX idx_weekly_session_behavior_warning_session_user
    ON weekly_session_behavior_warning(weekly_session_user_uuid);
CREATE INDEX idx_weekly_session_behavior_warning_deleted_at
    ON weekly_session_behavior_warning(deleted_at);

CREATE TABLE app_notification (
    uuid UUID PRIMARY KEY,
    recipient_user_uuid UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_app_notification_recipient
        FOREIGN KEY (recipient_user_uuid) REFERENCES user_salle(uuid)
);

CREATE INDEX idx_app_notification_recipient_created_at
    ON app_notification(recipient_user_uuid, created_at DESC);
CREATE INDEX idx_app_notification_recipient_unread
    ON app_notification(recipient_user_uuid, read_at)
    WHERE deleted_at IS NULL;
