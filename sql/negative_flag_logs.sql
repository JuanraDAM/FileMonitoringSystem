-- Reconstruir la tabla de errores negativos incluyendo todos los campos relevantes
DROP TABLE IF EXISTS negative_flag_logs;

CREATE TABLE negative_flag_logs (
    id                SERIAL         PRIMARY KEY,
    -- FK al registro original en trigger_control
    trigger_id        INTEGER        NOT NULL
                                REFERENCES trigger_control(id)
                                ON DELETE CASCADE,
    -- Repetimos aquí todos los campos clave para facilitar consultas directas
    file_config_id    INTEGER        NOT NULL,
    file_name         VARCHAR(255)   NOT NULL,
    field_name        VARCHAR(255),
    environment       VARCHAR(50)    NOT NULL,
    validation_flag   VARCHAR(50)    NOT NULL,
    error_message     TEXT,
    logged_at         TIMESTAMP      NOT NULL
);

-- Función que inyecta en negative_flag_logs todos los detalles del registro
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

-- Trigger que dispara la función tras cada inserción en trigger_control
DROP TRIGGER IF EXISTS trg_capture_negative_flag ON trigger_control;
CREATE TRIGGER trg_capture_negative_flag
AFTER INSERT ON trigger_control
FOR EACH ROW
EXECUTE FUNCTION fn_capture_negative_flag();
