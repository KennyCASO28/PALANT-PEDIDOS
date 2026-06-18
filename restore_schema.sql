-- ============================================================
-- SCRIPT DDL - SISTEMA PALANT-PEDIDOS (NORMALIZADO)
-- Base de Datos: PostgreSQL
-- ============================================================

-- 1. Tabla CLIENTES
CREATE TABLE IF NOT EXISTS clientes (
    id SERIAL PRIMARY KEY,
    nombre_institucion VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    ciudad VARCHAR(50) NOT NULL
);

-- 2. Tabla USUARIOS / VENDEDORES
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(80) NOT NULL UNIQUE,
    contrasena_encriptada VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('Vendedor', 'Diseñador', 'Jefe')),
    nombre_completo VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

-- Datos iniciales para pruebas en USUARIOS
INSERT INTO usuarios (nombre_usuario, contrasena_encriptada, rol, nombre_completo)
SELECT u.nombre_usuario, u.contrasena_encriptada, u.rol, u.nombre_completo
FROM (
    VALUES 
    ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Jefe', 'Administrador Principal'),
    ('juan.perez', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Vendedor', 'Juan Pérez')
) AS u(nombre_usuario, contrasena_encriptada, rol, nombre_completo)
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE usuarios.nombre_usuario = u.nombre_usuario
);

-- Tabla compatible para VENDEDORES (Retrocompatibilidad en Comboboxes de UI)
CREATE TABLE IF NOT EXISTS vendedores (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

INSERT INTO vendedores (nombre, activo)
SELECT v.nombre, v.activo
FROM (
    VALUES 
    ('Juan Pérez', TRUE),
    ('Ana García', TRUE),
    ('Carlos López', TRUE),
    ('Oficina Central', TRUE)
) AS v(nombre, activo)
WHERE NOT EXISTS (
    SELECT 1 FROM vendedores WHERE vendedores.nombre = v.nombre
);

-- 3. Tabla PEDIDOS
CREATE TABLE IF NOT EXISTS pedidos (
    id SERIAL PRIMARY KEY,
    cliente_nombre VARCHAR(255), -- Retrocompatibilidad
    vendedor VARCHAR(100),       -- Retrocompatibilidad
    cliente_id INTEGER REFERENCES clientes(id) ON DELETE SET NULL, -- FK Relacional
    vendedor_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL, -- FK Relacional
    codigo_pedido VARCHAR(50) UNIQUE,
    tipo_prenda VARCHAR(50),
    genero_corte VARCHAR(50),
    tipo_manga VARCHAR(50),
    codigo_cuello VARCHAR(50),
    -- Booleans
    tiene_malla BOOLEAN DEFAULT FALSE,
    tiene_medias BOOLEAN DEFAULT FALSE,
    puno_camiseta BOOLEAN DEFAULT FALSE,
    puno_short BOOLEAN DEFAULT FALSE,
    -- Estado, Fechas y Prioridad
    estado VARCHAR(50) DEFAULT 'Pendiente',
    fecha_creacion TIMESTAMP DEFAULT NOW(),
    fecha_entrega DATE,
    prioridad VARCHAR(50)
);

-- 4. Tabla DETALLE_NOMBRES (Roster de jugadores)
CREATE TABLE IF NOT EXISTS detalle_nombres (
    id SERIAL PRIMARY KEY,
    pedido_id INTEGER REFERENCES pedidos(id) ON DELETE CASCADE,
    nombre VARCHAR(100),
    numero VARCHAR(10),
    talla VARCHAR(10)
);

-- 5. Tabla FICHA_TECNICA (Historial de exportaciones PDF)
CREATE TABLE IF NOT EXISTS ficha_tecnica (
    id SERIAL PRIMARY KEY,
    pedido_id INTEGER REFERENCES pedidos(id) ON DELETE CASCADE UNIQUE,
    ruta_archivo_vectorial VARCHAR(500),
    observaciones_sublimacion TEXT,
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
