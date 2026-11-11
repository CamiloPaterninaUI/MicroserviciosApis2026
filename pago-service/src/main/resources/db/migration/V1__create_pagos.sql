CREATE TABLE IF NOT EXISTS pago (
    id BIGSERIAL PRIMARY KEY,
    pedido_id VARCHAR(255) NOT NULL,
    monto NUMERIC(12, 2) NOT NULL,
    metodo_pago VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pago_pedido_id ON pago(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pago_estado ON pago(estado);
