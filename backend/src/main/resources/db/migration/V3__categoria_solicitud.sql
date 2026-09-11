-- Categoría de gasto de la solicitud.
ALTER TABLE solicitud_presupuesto
    ADD COLUMN categoria VARCHAR(20) NOT NULL DEFAULT 'OTRO';

ALTER TABLE solicitud_presupuesto
    ADD CONSTRAINT categoria_valida CHECK (
        categoria IN ('VIATICOS', 'SOFTWARE', 'HERRAMIENTAS', 'MARKETING', 'OTRO')
    );
