-- Ticketeo - Utilidades de mantenimiento (PostgreSQL)
-- Base detectada en application.properties: PostgreSQL
-- IMPORTANTE: ejecutar primero en ambiente de desarrollo.

-- ============================================================
-- 1) Limpiar TODA la base de datos (solo datos, NO tablas)
-- ============================================================
-- Esto elimina filas y reinicia secuencias/autoincrementos.
TRUNCATE TABLE
    boletos_compra,
    compras,
    reporte_interes,
    evento_tipos_entrada,
    eventos,
    tipo_entradas,
    categoria_eventos,
    usuarios
RESTART IDENTITY CASCADE;


-- ============================================================
-- 2) Consultar eventos en la base de datos
-- ============================================================
-- Lista general
SELECT
    e.id_evento,
    e.nombre_evento,
    e.fecha_evento,
    e.estado,
    c.nombre_categoria,
    e.valor_minimo_evento,
    e.imagen_evento,
    CASE
        WHEN e.estado = TRUE AND e.fecha_evento < CURRENT_DATE THEN TRUE
        ELSE FALSE
    END AS es_completado
FROM eventos e
LEFT JOIN categoria_eventos c ON c.id_categoria = e.id_categoria
ORDER BY e.fecha_evento ASC, e.id_evento ASC;

-- Solo eventos completados (segun la logica actual)
SELECT
    e.id_evento,
    e.nombre_evento,
    e.fecha_evento,
    e.estado
FROM eventos e
WHERE e.estado = TRUE
  AND e.fecha_evento < CURRENT_DATE
ORDER BY e.fecha_evento DESC;


-- ============================================================
-- 3) Marcar un evento como completado cambiando la fecha por ID
-- ============================================================
-- Cambia el ID segun el evento que quieras afectar.
-- Ejemplo: para el evento 10, dejarlo con fecha de ayer.
UPDATE eventos
SET fecha_evento = CURRENT_DATE - 1
WHERE id_evento = 10;

-- Verificacion rapida del evento actualizado
SELECT
    e.id_evento,
    e.nombre_evento,
    e.fecha_evento,
    e.estado,
    (e.estado = TRUE AND e.fecha_evento < CURRENT_DATE) AS es_completado
FROM eventos e
WHERE e.id_evento = 10;

