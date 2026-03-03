DELETE FROM reporte_interes
WHERE id_evento IN (
    SELECT id_evento FROM eventos WHERE nombre_evento LIKE 'SEED Event %'
);

DELETE FROM eventos WHERE nombre_evento LIKE 'SEED Event %';


