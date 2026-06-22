-- Las mesas sin conteo historico pueden tener NULL por el esquema legacy.
-- Para los resumenes, la ausencia de votos registrados equivale a cero.
UPDATE tec.mesas
   SET totalvotos = 0
 WHERE totalvotos IS NULL;

ALTER TABLE tec.mesas
    ALTER COLUMN totalvotos SET DEFAULT 0;

ALTER TABLE tec.mesas
    ALTER COLUMN totalvotos SET NOT NULL;
