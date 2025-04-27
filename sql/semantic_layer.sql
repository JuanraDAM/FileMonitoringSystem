DROP TABLE IF EXISTS semantic_layer;

-- Tabla semántica
CREATE TABLE semantic_layer (
    id                    SERIAL         PRIMARY KEY,
    field_position        INTEGER        NOT NULL,
    field_name            VARCHAR(255)   NOT NULL,
    field_description     VARCHAR(255),
    pk                    BOOLEAN        NOT NULL DEFAULT FALSE,
    data_type             VARCHAR(50)    NOT NULL,
    length                VARCHAR(50),
    nullable              BOOLEAN        NOT NULL DEFAULT TRUE,
    decimal_symbol        VARCHAR(5)     NOT NULL DEFAULT ',',
    CONSTRAINT uq_semantic_layer UNIQUE (field_position)
);


-- Ejemplo de inserciones en semantic_layer para 'bank_accounts.csv' (file_config_id = 1)
INSERT INTO semantic_layer (
    file_config_id,
    field_position,
    field_name,
    field_description,
    pk,
    data_type,
    length,
    nullable
) VALUES
  (1,  'account_number',            'Identificador único de la cuenta bancaria',          TRUE,  'CHAR',     '10',    FALSE),
  (2,  'first_name',                'Nombre de pila del titular',                         FALSE, 'VARCHAR',  '50',    FALSE),
  (3,  'last_name',                 'Apellido del titular',                              FALSE, 'VARCHAR',  '50',    FALSE),
  (4,  'date_of_birth',             'Fecha de nacimiento del cliente',                    FALSE, 'DATE',     NULL,    FALSE),
  (5,  'gender',                    'Género o autodefinición de género del cliente',      FALSE, 'VARCHAR',  '6',     FALSE),
  (6,  'email',                     'Correo electrónico de contacto',                     FALSE, 'VARCHAR',  '100',   FALSE),
  (7,  'phone',                     'Teléfono con prefijo internacional y 9 dígitos',     FALSE, 'VARCHAR',  '13',    FALSE),
  (8,  'address',                   'Dirección postal (calle y número)',                  FALSE, 'VARCHAR',  '100',   FALSE),
  (9,  'city',                      'Ciudad de la dirección postal',                      FALSE, 'VARCHAR',  '50',    FALSE),
  (10,  'state',                     'Comunidad/Provincia de la dirección postal',         FALSE, 'VARCHAR',  '50',    FALSE),
  (11,  'postal_code',               'Código postal (5 dígitos)',                          FALSE, 'CHAR',     '5',     FALSE),
  (12,  'country',                   'País de residencia o de la sucursal',                FALSE, 'VARCHAR',  '50',    FALSE),
  (13,  'account_type',              'Tipo de producto bancario (Checking/Savings/etc.)',  FALSE, 'VARCHAR',  '10',    FALSE),
  (14,  'currency',                  'Moneda de la cuenta (ISO 4217)',                     FALSE, 'CHAR',     '3',     FALSE),
  (15,  'balance',                   'Saldo disponible en la cuenta',                      FALSE, 'DECIMAL',  '12,2',  FALSE),
  (16,  'created_date',              'Fecha de apertura de la cuenta',                     FALSE, 'DATE',     NULL,    FALSE),
  (17,  'last_login',                'Último acceso al canal online',                      FALSE, 'TIMESTAMP',NULL,    FALSE),
  (18,  'last_transaction_date',     'Fecha y hora de la última operación',                FALSE, 'TIMESTAMP',NULL,    FALSE),
  (19,  'status',                    'Estado operativo de la cuenta',                      FALSE, 'VARCHAR',  '10',    FALSE),
  (20,  'kyc_status',                'Estado del proceso KYC',                             FALSE, 'VARCHAR',  '8',     FALSE),
  (21,  'interest_rate',             'Tasa de interés anual (%)',                          FALSE, 'DECIMAL',  '4,2',   FALSE),
  (22,  'overdraft_limit',           'Límite de descubierto autorizado',                   FALSE, 'DECIMAL',  '8,2',   FALSE),
  (23,  'credit_score',              'Puntuación crediticia (300–850)',                    FALSE, 'INT',      NULL,    FALSE),
  (24,  'risk_score',                'Índice interno de riesgo (0–100)',                   FALSE, 'DECIMAL',  '5,2',   FALSE),
  (25,  'account_manager_id',        'ID interno del gestor asignado',                     FALSE, 'CHAR',     '6',     FALSE),
  (26,  'branch',                    'Código de la sucursal',                              FALSE, 'VARCHAR',  '5',     FALSE),
  (27,  'branch_city',               'Ciudad de la sucursal',                              FALSE, 'VARCHAR',  '50',    FALSE),
  (28,  'is_joint_account',          'Indica si la cuenta es mancomunada (Yes/No)',        FALSE, 'VARCHAR',  '3',     FALSE),
  (29,  'num_transactions',          'Número total de transacciones realizadas',           FALSE, 'INT',      NULL,    FALSE),
  (30,  'avg_transaction_amount',    'Importe medio de las transacciones',                 FALSE, 'DECIMAL',  '8,2',   FALSE);