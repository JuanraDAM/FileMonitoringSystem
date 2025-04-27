-- Tabla de logs de validación disparados por triggers
DROP TABLE IF EXISTS trigger_control;

CREATE TABLE trigger_control (
    id                SERIAL PRIMARY KEY,
    -- FK al fichero configurado en file_configuration
    file_config_id    INTEGER NOT NULL
                         REFERENCES file_configuration(id),
    -- Nombre físico del fichero validado
    file_name         VARCHAR(255) NOT NULL,
    -- Nombre de la columna validada (si aplica)
    field_name        VARCHAR(255),
    -- Entorno de ejecución (dev, test, prod, etc.)
    environment       VARCHAR(50)  NOT NULL,
    -- Flag que indica la regla o paso en que falla o pasa
    validation_flag   VARCHAR(50)  NOT NULL,
    -- Descripción detallada del error (si aplica)
    error_message     TEXT,
    -- Marca temporal de cuándo se registró este log
    logged_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
