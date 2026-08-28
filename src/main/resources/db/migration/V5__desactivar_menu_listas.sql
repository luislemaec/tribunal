-- La administracion de listas se centraliza en candidatos.jsf.
-- Se conserva el registro y sus relaciones para mantener historial y auditoria.
UPDATE public.tb_menu_rol mr
   SET estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
 WHERE mr.menu_id IN (
     SELECT m.menu_id
       FROM public.tb_menu m
      WHERE m.componente_id = 'm_listas'
         OR m.menu_url = '/listas.jsf'
 );

UPDATE public.tb_menu m
   SET estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
 WHERE m.componente_id = 'm_listas'
    OR m.menu_url = '/listas.jsf';
