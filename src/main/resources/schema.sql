-- 创建上传文件表
CREATE TABLE IF NOT EXISTS upload_file (
    id INTEGER PRIMARY KEY,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_size INTEGER,
    upload_time TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    error_message TEXT,
    created_by TEXT
);

-- 创建文件数据表
CREATE TABLE IF NOT EXISTS file_data (
    id INTEGER PRIMARY KEY,
    file_id INTEGER NOT NULL,
    column1 TEXT,
    column2 TEXT,
    column3 TEXT,
    column4 TEXT,
    data_content TEXT,
    row_index INTEGER
);

-- 创建分析结果表
CREATE TABLE IF NOT EXISTS analysis_result (
    id INTEGER PRIMARY KEY,
    file_id INTEGER NOT NULL,
    file_data_id INTEGER,
    result_content TEXT NOT NULL,
    analysis_time TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    log_info TEXT,
    code TEXT
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_file_data_file_id ON file_data(file_id);
CREATE INDEX IF NOT EXISTS idx_analysis_result_file_id ON analysis_result(file_id);
CREATE INDEX IF NOT EXISTS idx_analysis_result_file_data_id ON analysis_result(file_data_id);
