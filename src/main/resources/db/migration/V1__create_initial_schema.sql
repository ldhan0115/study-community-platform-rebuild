-- =========================================
-- 1. Member
-- =========================================

CREATE TABLE member (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_member PRIMARY KEY (member_id),
    CONSTRAINT uq_member_login_id UNIQUE (login_id),
    CONSTRAINT uq_member_email UNIQUE (email),
    CONSTRAINT uq_member_nickname UNIQUE (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================
-- 2. Study
-- =========================================

CREATE TABLE study (
    study_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,

    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    method VARCHAR(10) NOT NULL,
    region VARCHAR(100) NULL,
    capacity INT NOT NULL,
    status VARCHAR(10) NOT NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    CONSTRAINT pk_study PRIMARY KEY (study_id),

    CONSTRAINT fk_study_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    CONSTRAINT chk_study_method
        CHECK (method IN ('ONLINE', 'OFFLINE')),

    CONSTRAINT chk_study_status
        CHECK (status IN ('OPEN', 'CLOSED')),

    CONSTRAINT chk_study_capacity
        CHECK (capacity BETWEEN 1 AND 30),

    CONSTRAINT chk_study_region
        CHECK (
            (method = 'ONLINE' AND region IS NULL)
            OR
            (
                method = 'OFFLINE'
                AND region IS NOT NULL
                AND CHAR_LENGTH(TRIM(region)) > 0
            )
        ),

    INDEX idx_study_member_created (member_id, created_at),
    INDEX idx_study_deleted_created (deleted_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================
-- 3. Application
-- =========================================

CREATE TABLE application (
    application_id BIGINT NOT NULL AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,

    message VARCHAR(255) NOT NULL,
    status VARCHAR(12) NOT NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_application PRIMARY KEY (application_id),

    CONSTRAINT fk_application_study
        FOREIGN KEY (study_id)
        REFERENCES study(study_id),

    CONSTRAINT fk_application_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    CONSTRAINT uq_application_study_member
        UNIQUE (study_id, member_id),

    CONSTRAINT chk_application_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'CANCELLED'
            )
        ),

    INDEX idx_application_study_status (study_id, status),
    INDEX idx_application_member_created (member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================
-- 4. Comment
-- =========================================

CREATE TABLE comment (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,

    content TEXT NOT NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    CONSTRAINT pk_comment PRIMARY KEY (comment_id),

    CONSTRAINT fk_comment_study
        FOREIGN KEY (study_id)
        REFERENCES study(study_id),

    CONSTRAINT fk_comment_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    INDEX idx_comment_study_deleted_created (
        study_id,
        deleted_at,
        created_at
    ),

    INDEX idx_comment_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;