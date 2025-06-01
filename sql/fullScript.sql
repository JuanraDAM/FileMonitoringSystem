

-- ELIMINAR TABLAS EXISTENTES EN ORDEN DEPENDIENTE
DROP TABLE IF EXISTS negative_flag_logs;
DROP TABLE IF EXISTS trigger_control;
DROP TABLE IF EXISTS semantic_layer;
DROP TABLE IF EXISTS file_configuration;

-- TABLA DE CONFIGURACIÓN DE FICHEROS
CREATE TABLE file_configuration (
    id                  SERIAL       PRIMARY KEY,
    file_format         VARCHAR(50)  NOT NULL,
    path                VARCHAR(500) NOT NULL,
    file_name           VARCHAR(255) NOT NULL,
    has_header          BOOLEAN      NOT NULL DEFAULT TRUE,
    delimiter           CHAR(1)      NOT NULL DEFAULT ',',        -- OK: un solo carácter
    quote_char          CHAR(1)      NOT NULL DEFAULT '"',        -- OK: un solo carácter
    escape_char         CHAR(1)      NOT NULL DEFAULT E'\\',      -- OJO: E'\\' es UN carácter de backslash
    date_format         VARCHAR(50)  DEFAULT 'yyyy-MM-dd',
    timestamp_format    VARCHAR(50)  DEFAULT 'yyyy-MM-dd HH:mm:ss',
    partition_columns   VARCHAR(255)
);

-- EJEMPLO DE INSERCIÓN EN file_configuration (con E'\\' para escape_char)
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
) VALUES
      (
          'csv',
          '/data/bank_accounts/',
          'bank_accounts.csv',
          TRUE,
          ',',
          '"',
          E'\\',
          'yyyy-MM-dd',
          'yyyy-MM-dd HH:mm:ss',
          NULL
      ),
      (
          'csv',
          '/data/bank_accounts/',
          'typological_errors.csv',
          TRUE,
          ',',
          '"',
          E'\\',
          'yyyy-MM-dd',
          'yyyy-MM-dd HH:mm:ss',
          NULL
      ),
      (
          'csv',
          '/data/bank_accounts/',
          'structural_errors.csv',
          TRUE,
          ',',
          '"',
          E'\\',
          'yyyy-MM-dd',
          'yyyy-MM-dd HH:mm:ss',
          NULL
      ),
      (
          'csv',
          '/data/bank_accounts/',
          'referential_errors.csv',
          TRUE,
          ',',
          '"',
          E'\\',
          'yyyy-MM-dd',
          'yyyy-MM-dd HH:mm:ss',
          NULL
      ),
      (
          'csv',
          '/data/bank_accounts/',
          'functional_errors.csv',
          TRUE,
          ',',
          '"',
          E'\\',
          'yyyy-MM-dd',
          'yyyy-MM-dd HH:mm:ss',
          NULL
      );



-- TABLA SEMÁNTICA
CREATE TABLE semantic_layer (
    id                SERIAL       PRIMARY KEY,
    field_position    INTEGER      NOT NULL,
    field_name        VARCHAR(255) NOT NULL,
    field_description VARCHAR(255),
    pk                BOOLEAN      NOT NULL DEFAULT FALSE,
    data_type         VARCHAR(50)  NOT NULL,
    length            VARCHAR(50),
    nullable          BOOLEAN      NOT NULL DEFAULT TRUE,
    decimal_symbol    VARCHAR(5)   NOT NULL DEFAULT ',',
    CONSTRAINT uq_semantic_layer UNIQUE (field_position)
);

-- INSERCIONES EN semantic_layer PARA bank_accounts.csv
INSERT INTO semantic_layer (
    field_position, field_name,                field_description,                                      pk,    data_type,  length,    nullable
) VALUES
  ( 1, 'account_number',         'Identificador único de la cuenta bancaria',                TRUE,  'CHAR',     '10',      FALSE),
  ( 2, 'first_name',             'Nombre de pila del titular',                               FALSE, 'VARCHAR',  '50',      FALSE),
  ( 3, 'last_name',              'Apellido del titular',                                     FALSE, 'VARCHAR',  '50',      FALSE),
  ( 4, 'date_of_birth',          'Fecha de nacimiento del cliente',                          FALSE, 'DATE',     NULL,      FALSE),
  ( 5, 'gender',                 'Género o autodefinición de género del cliente',            FALSE, 'VARCHAR',  '6',       FALSE),
  ( 6, 'email',                  'Correo electrónico de contacto',                          FALSE, 'VARCHAR',  '100',     FALSE),
  ( 7, 'phone',                  'Teléfono con prefijo internacional y 9 dígitos',           FALSE, 'VARCHAR',  '13',      FALSE),
  ( 8, 'address',                'Dirección postal (calle y número)',                       FALSE, 'VARCHAR',  '100',     FALSE),
  ( 9, 'city',                   'Ciudad de la dirección postal',                           FALSE, 'VARCHAR',  '50',      FALSE),
 (10, 'state',                  'Comunidad/Provincia de la dirección postal',              FALSE, 'VARCHAR',  '50',      FALSE),
 (11, 'postal_code',            'Código postal (5 dígitos)',                               FALSE, 'CHAR',     '5',       FALSE),
 (12, 'country',                'País de residencia o de la sucursal',                     FALSE, 'VARCHAR',  '50',      FALSE),
 (13, 'account_type',           'Tipo de producto bancario (Checking/Savings/etc.)',       FALSE, 'VARCHAR',  '10',      FALSE),
 (14, 'currency',               'Moneda de la cuenta (ISO 4217)',                          FALSE, 'CHAR',     '3',       FALSE),
 (15, 'balance',                'Saldo disponible en la cuenta',                           FALSE, 'DECIMAL',  '12,2',    FALSE),
 (16, 'created_date',           'Fecha de apertura de la cuenta',                          FALSE, 'DATE',     NULL,      FALSE),
 (17, 'last_login',             'Último acceso al canal online',                           FALSE, 'TIMESTAMP',NULL,      FALSE),
 (18, 'last_transaction_date',  'Fecha y hora de la última operación',                     FALSE, 'TIMESTAMP',NULL,      FALSE),
 (19, 'status',                 'Estado operativo de la cuenta',                           FALSE, 'VARCHAR',  '10',      FALSE),
 (20, 'kyc_status',             'Estado del proceso KYC',                                  FALSE, 'VARCHAR',  '8',       FALSE),
 (21, 'interest_rate',          'Tasa de interés anual (%)',                               FALSE, 'DECIMAL',  '4,2',     FALSE),
 (22, 'overdraft_limit',        'Límite de descubierto autorizado',                        FALSE, 'DECIMAL',  '8,2',     FALSE),
 (23, 'credit_score',           'Puntuación crediticia (300–850)',                         FALSE, 'INT',      NULL,      FALSE),
 (24, 'risk_score',             'Índice interno de riesgo (0–100)',                        FALSE, 'DECIMAL',  '5,2',     FALSE),
 (25, 'account_manager_id',     'ID interno del gestor asignado',                          FALSE, 'CHAR',     '6',       FALSE),
 (26, 'branch',                 'Código de la sucursal',                                   FALSE, 'VARCHAR',  '5',       FALSE),
 (27, 'branch_city',            'Ciudad de la sucursal',                                   FALSE, 'VARCHAR',  '50',      FALSE),
 (28, 'is_joint_account',       'Indica si la cuenta es mancomunada (Yes/No)',             FALSE, 'VARCHAR',  '3',       FALSE),
 (29, 'num_transactions',       'Número total de transacciones realizadas',                FALSE, 'INT',      NULL,      FALSE),
 (30, 'avg_transaction_amount', 'Importe medio de las transacciones',                      FALSE, 'DECIMAL',  '8,2',     FALSE)
;


-- TABLA DE LOGS DE VALIDACIÓN TRIGGER_CONTROL
CREATE TABLE trigger_control (
    id                SERIAL       PRIMARY KEY,
    file_config_id    INTEGER      NOT NULL REFERENCES file_configuration(id),
    file_name         VARCHAR(255) NOT NULL,
    field_name        VARCHAR(255),
    environment       VARCHAR(50)  NOT NULL,
    validation_flag   VARCHAR(50)  NOT NULL,
    error_message     TEXT,
    logged_at         TIMESTAMP    NOT NULL DEFAULT now()
);


-- TABLA DE ERRORES “NEGATIVOS”
CREATE TABLE negative_flag_logs (
    id                SERIAL       PRIMARY KEY,
    trigger_id        INTEGER      NOT NULL REFERENCES trigger_control(id) ON DELETE CASCADE,
    file_config_id    INTEGER      NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    field_name        VARCHAR(255),
    environment       VARCHAR(50)  NOT NULL,
    validation_flag   VARCHAR(50)  NOT NULL,
    error_message     TEXT,
    logged_at         TIMESTAMP    NOT NULL
);

-- FUNCIÓN QUE CAPTURA FLAGS >= 30
CREATE OR REPLACE FUNCTION fn_capture_negative_flag()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.validation_flag::INT >= 30) THEN
        INSERT INTO negative_flag_logs (
            trigger_id,
            file_config_id,
            file_name,
            field_name,
            environment,
            validation_flag,
            error_message,
            logged_at
        ) VALUES (
            NEW.id,
            NEW.file_config_id,
            NEW.file_name,
            NEW.field_name,
            NEW.environment,
            NEW.validation_flag,
            NEW.error_message,
            NEW.logged_at
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TRIGGER QUE LLAMA A LA FUNCIÓN TRAS CADA INSERT EN trigger_control
CREATE TRIGGER trg_capture_negative_flag
AFTER INSERT ON trigger_control
FOR EACH ROW
EXECUTE FUNCTION fn_capture_negative_flag();
