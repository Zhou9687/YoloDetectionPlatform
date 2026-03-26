CREATE TABLE IF NOT EXISTS training_jobs (
    job_id VARCHAR(64) PRIMARY KEY,
    dataset_path VARCHAR(1024) NOT NULL,
    model_path VARCHAR(1024) NULL,
    epochs INT NOT NULL,
    batch_size INT NOT NULL,
    conf DOUBLE NULL,
    progress INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6) NULL,
    message LONGTEXT NULL,
    best_model_path VARCHAR(1024) NULL
);

CREATE TABLE IF NOT EXISTS image_annotations (
    image_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(128) NOT NULL,
    relative_path VARCHAR(1024) NULL,
    original_file_name VARCHAR(1024) NULL,
    stored_path VARCHAR(2048) NOT NULL,
    label_path VARCHAR(2048) NULL,
    annotated TINYINT(1) NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMP(6) NOT NULL,
    boxes_json LONGTEXT NULL,
    INDEX idx_image_annotations_batch_uploaded (batch_id, uploaded_at)
);

CREATE TABLE IF NOT EXISTS dataset_build_records (
    record_id VARCHAR(64) PRIMARY KEY,
    dataset_name VARCHAR(255) NOT NULL,
    dataset_path VARCHAR(2048) NOT NULL,
    output_preset VARCHAR(64) NULL,
    train_count INT NOT NULL,
    val_count INT NOT NULL,
    test_count INT NOT NULL,
    val_ratio DOUBLE NOT NULL,
    test_ratio DOUBLE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_dataset_build_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS prediction_records (
    result_id VARCHAR(64) PRIMARY KEY,
    file_name VARCHAR(1024) NULL,
    relative_path VARCHAR(2048) NULL,
    result_image_path VARCHAR(2048) NOT NULL,
    model_path VARCHAR(2048) NULL,
    conf DOUBLE NULL,
    iou DOUBLE NULL,
    device VARCHAR(32) NULL,
    imgsz INT NULL,
    augment TINYINT(1) NOT NULL DEFAULT 0,
    max_det INT NULL,
    boxes_json LONGTEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_prediction_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_users_username (username)
);
