--
-- JBoss, Home of Professional Open Source
-- Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
-- contributors by the @authors tag. See the copyright.txt in the
-- distribution for a full listing of individual contributors.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- http://www.apache.org/licenses/LICENSE-2.0
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- You can use this file to load seed data into the database using SQL statements
ALTER SEQUENCE public.tb_iglesia_igl_id_seq RESTART WITH 1 OWNED BY public.tb_iglesia.igl_id;
ALTER SEQUENCE public.tb_iglesia_igl_id_seq OWNER TO postgres;

ALTER SEQUENCE public.tb_iglesia_persona_igpe_id_seq RESTART WITH 1 OWNED BY public.tb_iglesia_persona.igpe_id;
ALTER SEQUENCE public.tb_iglesia_persona_igpe_id_seq OWNER TO postgres;

ALTER SEQUENCE public.tb_persona_pers_id_seq RESTART WITH 8 OWNED BY public.tb_persona.pers_id;
ALTER SEQUENCE public.tb_persona_pers_id_seq OWNER TO postgres;

ALTER SEQUENCE tec.padron_padron_id_seq RESTART WITH 1 OWNED BY tec.padron.padron_id;
ALTER SEQUENCE tec.padron_padron_id_seq OWNER TO postgres;