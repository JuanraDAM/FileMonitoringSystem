DROP TABLE IF EXISTS file_configuration;

-- Tabla de configuración de ficheros para Spark
CREATE TABLE file_configuration (
    id                    SERIAL         PRIMARY KEY,
    file_format           VARCHAR(50)    NOT NULL,
    path                  VARCHAR(500)   NOT NULL,
    file_name             VARCHAR(255)   NOT NULL,
    has_header            BOOLEAN        NOT NULL DEFAULT TRUE,
    delimiter             CHAR(1)        NOT NULL DEFAULT ',',
    quote_char            CHAR(1)        NOT NULL DEFAULT '"',
    escape_char           CHAR(1)        NOT NULL DEFAULT '\\',
    date_format           VARCHAR(50)    DEFAULT 'yyyy-MM-dd',
    timestamp_format      VARCHAR(50)    DEFAULT 'yyyy-MM-dd HH:mm:ss',
    partition_columns     VARCHAR(255)
);

-- Ejemplo de inserción en file_configuration
INSERT INTO file_configuration (
    file_format,
    path,
    file_name,
    has_header,
    delimiter,
    quote_char,
    escape_char,
    date_format,
    timestamp_format,
    partition_columns
) VALUES (
    'csv',
    '/data/bank_accounts/',
    'bank_accounts.csv',
    TRUE,
    ',',
    '"',
    '\\',
    'yyyy-MM-dd',
    'yyyy-MM-dd HH:mm:ss',
    NULL
);