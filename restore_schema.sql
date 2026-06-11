-- Restauración de Tablas basada en el código Java

-- 1. Tabla VENDEDORES
CREATE TABLE IF NOT EXISTS vendedores (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

-- Datos de prueba para vendedores (Necesarios para que el ComboBox funcione)
INSERT INTO vendedores (nombre, activo) VALUES 
('Juan Pérez', TRUE),
('Ana García', TRUE),
('Carlos López', TRUE),
('Oficina Central', TRUE);

-- 2. Tabla PEDIDOS
CREATE TABLE IF NOT EXISTS pedidos (
    id SERIAL PRIMARY KEY,
    cliente_nombre VARCHAR(255),
    vendedor VARCHAR(100),
    codigo_pedido VARCHAR(50),
    tipo_prenda VARCHAR(50),
    genero_corte VARCHAR(50),
    tipo_manga VARCHAR(50),
    codigo_cuello VARCHAR(50),
    -- Booleans
    tiene_malla BOOLEAN DEFAULT FALSE,
    tiene_medias BOOLEAN DEFAULT FALSE,
    puno_camiseta BOOLEAN DEFAULT FALSE,
    puno_short BOOLEAN DEFAULT FALSE,
    -- Estado y Fecha
    estado VARCHAR(50) DEFAULT 'Pendiente',
    fecha_creacion TIMESTAMP DEFAULT NOW()
);

-- 3. Tabla DETALLE_NOMBRES (Los jugadores del pedido)
CREATE TABLE IF NOT EXISTS detalle_nombres (
    id SERIAL PRIMARY KEY,
    pedido_id INTEGER REFERENCES pedidos(id) ON DELETE CASCADE,
    nombre VARCHAR(100),
    numero VARCHAR(10),
    talla VARCHAR(10)
);
