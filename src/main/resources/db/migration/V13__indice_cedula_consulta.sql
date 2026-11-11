-- Consulta pública de lugar de votación por cédula (consultar.xhtml, ConsultaLugarVotacionService):
-- índice para buscar la persona por documento sin recorrer toda la tabla bajo carga.
-- No cambia datos ni migraciones anteriores. Idempotente: no falla si el índice ya existe.
CREATE INDEX IF NOT EXISTS idx_persona_documento ON public.tb_persona (pers_documento);
