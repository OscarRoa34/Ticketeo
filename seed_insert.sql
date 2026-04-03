DO $$
DECLARE
    cat_conciertos INTEGER;
    cat_deportes   INTEGER;
    cat_teatro     INTEGER;
    i INTEGER;
BEGIN
    SELECT id_categoria INTO cat_conciertos FROM categoria_eventos WHERE LOWER(nombre_categoria) = 'conciertos' LIMIT 1;
    SELECT id_categoria INTO cat_deportes   FROM categoria_eventos WHERE LOWER(nombre_categoria) = 'deportes'   LIMIT 1;
    SELECT id_categoria INTO cat_teatro     FROM categoria_eventos WHERE LOWER(nombre_categoria) = 'teatro'     LIMIT 1;

    FOR i IN 1..1000 LOOP
        INSERT INTO eventos (id_evento, id_categoria, nombre_evento, fecha_evento, valor_minimo_evento, descripcion_evento, imagen_evento, estado)
        VALUES (
            nextval('evento_seq'),
            CASE (i % 3)
                WHEN 1 THEN cat_conciertos
                WHEN 2 THEN cat_deportes
                ELSE cat_teatro
            END,
            'SEED Event ' || i,
            CURRENT_DATE + ((i * 3) % 365),
            (i % 20 + 1) * 15000.0,
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Event number ' || i || '.',
            'https://picsum.photos/seed/' || i || '/800/600',
            (i % 5 != 0)
        );
    END LOOP;

    RAISE NOTICE 'SEED: 1000 eventos insertados.';
END;
$$;

