-- ============================================
--  CREACIÓN DE TABLAS PARA SESIONES SEMANALES
-- ============================================

-- Tabla de situaciones vitales
CREATE TABLE vital_situation (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    stages INT[] NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Tabla de sesiones dentro de situaciones vitales
CREATE TABLE vital_situation_session (
    id SERIAL PRIMARY KEY,
    vital_situation_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    pdf VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    FOREIGN KEY (vital_situation_id) REFERENCES vital_situation(id)
);

-- Tabla de sesiones semanales
CREATE TABLE weekly_session (
    id SERIAL PRIMARY KEY,
    vital_situation_session_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    group_salle_id INT NOT NULL,
    session_datetime TIMESTAMP NOT NULL,
    status INT NOT NULL DEFAULT 0, -- 0=DRAFT, 1=PUBLISHED, 2=ARCHIVED
    deleted_at TIMESTAMP,
    FOREIGN KEY (vital_situation_session_id) REFERENCES vital_situation_session(id),
    FOREIGN KEY (group_salle_id) REFERENCES group_salle(id)
);

-- Tabla de relación entre sesiones semanales y usuarios (asistencia)
CREATE TABLE weekly_session_user (
    id SERIAL PRIMARY KEY,
    weekly_session_id INT NOT NULL,
    user_group_id INT NOT NULL,
    status INT NOT NULL DEFAULT 0, -- 0=NO_ATTENDS, 1=ATTENDS
    deleted_at TIMESTAMP,
    FOREIGN KEY (weekly_session_id) REFERENCES weekly_session(id),
    FOREIGN KEY (user_group_id) REFERENCES user_group(id),
    UNIQUE(weekly_session_id, user_group_id)
);

-- ============================================
--  INSERTS DE SITUACIONES VITALES Y SESIONES
-- ============================================

-- ETAPA NAZARET (stages 0 y 1)
INSERT INTO vital_situation (title, stages, is_default) VALUES ('Yo soy capaz de...', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy capaz de...' AND stages = ARRAY[0, 1]), '¿Nos conocemos?', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy capaz de...' AND stages = ARRAY[0, 1]), '¿Qué puedo hacer yo solo?', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy capaz de...' AND stages = ARRAY[0, 1]), 'Lo hago yo solo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy capaz de...' AND stages = ARRAY[0, 1]), '¡Sí puedo!', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Mis amigos me necesitan', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me necesitan' AND stages = ARRAY[0, 1]), 'Sillas cooperativas', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me necesitan' AND stages = ARRAY[0, 1]), 'Las tres urnas', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me necesitan' AND stages = ARRAY[0, 1]), 'Juego de firmas y firmas', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me necesitan' AND stages = ARRAY[0, 1]), 'El puzzle de las soluciones', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Yo necesito a los amigos', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Yo necesito a los amigos' AND stages = ARRAY[0, 1]), 'Amigos de redes', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo necesito a los amigos' AND stages = ARRAY[0, 1]), 'Cohesión de grupo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo necesito a los amigos' AND stages = ARRAY[0, 1]), 'Para ser un buen amigo...', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo necesito a los amigos' AND stages = ARRAY[0, 1]), 'Somos uno', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('¿Dónde buscar respuestas?', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¿Dónde buscar respuestas?' AND stages = ARRAY[0, 1]), 'Test sobre la felicidad', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Dónde buscar respuestas?' AND stages = ARRAY[0, 1]), '¿Cómo me siento cuando...?', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Dónde buscar respuestas?' AND stages = ARRAY[0, 1]), 'Oír la voz de Dios', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Dónde buscar respuestas?' AND stages = ARRAY[0, 1]), '"GymBuscaDios"', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Jesús es mi amigo', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Jesús es mi amigo' AND stages = ARRAY[0, 1]), 'Solos y acompañados', true),
    ((SELECT id FROM vital_situation WHERE title = 'Jesús es mi amigo' AND stages = ARRAY[0, 1]), 'La amistad de Jesús', true),
    ((SELECT id FROM vital_situation WHERE title = 'Jesús es mi amigo' AND stages = ARRAY[0, 1]), 'El buen amigo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Jesús es mi amigo' AND stages = ARRAY[0, 1]), 'Clases de amigos', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Soy de Salle Joven', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Soy de Salle Joven' AND stages = ARRAY[0, 1]), '...y mi grupo se llama', true),
    ((SELECT id FROM vital_situation WHERE title = 'Soy de Salle Joven' AND stages = ARRAY[0, 1]), 'Entre todos suena mejor', true),
    ((SELECT id FROM vital_situation WHERE title = 'Soy de Salle Joven' AND stages = ARRAY[0, 1]), 'Me proyecto hacia el futuro', true),
    ((SELECT id FROM vital_situation WHERE title = 'Soy de Salle Joven' AND stages = ARRAY[0, 1]), '¿Cómo te cambia Salle Joven?', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Yo soy...', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy...' AND stages = ARRAY[0, 1]), 'El globo de la autoestima', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy...' AND stages = ARRAY[0, 1]), 'Cómo me veo y cómo me ven', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy...' AND stages = ARRAY[0, 1]), 'Tengo muchas cosas buenas', true),
    ((SELECT id FROM vital_situation WHERE title = 'Yo soy...' AND stages = ARRAY[0, 1]), 'Aprender a decir que no', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Voy creciendo, me hago mayor...', ARRAY[0, 1], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Voy creciendo, me hago mayor...' AND stages = ARRAY[0, 1]), 'Crece... aunque no sepas...', true),
    ((SELECT id FROM vital_situation WHERE title = 'Voy creciendo, me hago mayor...' AND stages = ARRAY[0, 1]), 'Subasta de valores', true),
    ((SELECT id FROM vital_situation WHERE title = 'Voy creciendo, me hago mayor...' AND stages = ARRAY[0, 1]), 'He aprendido a hablar...', true),
    ((SELECT id FROM vital_situation WHERE title = 'Voy creciendo, me hago mayor...' AND stages = ARRAY[0, 1]), 'Juegos infantiles', true);

-- ETAPA GENESARET (stages 2 y 3)
INSERT INTO vital_situation (title, stages, is_default) VALUES ('¡Cómo has cambiado!', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¡Cómo has cambiado!' AND stages = ARRAY[2, 3]), 'Voy creciendo', true),
    ((SELECT id FROM vital_situation WHERE title = '¡Cómo has cambiado!' AND stages = ARRAY[2, 3]), '¿Queremos cambiar?', true),
    ((SELECT id FROM vital_situation WHERE title = '¡Cómo has cambiado!' AND stages = ARRAY[2, 3]), 'Soy una sorpresa para mí', true),
    ((SELECT id FROM vital_situation WHERE title = '¡Cómo has cambiado!' AND stages = ARRAY[2, 3]), '¿Quién soy?', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Mis seguridades', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Mis seguridades' AND stages = ARRAY[2, 3]), 'Decisiones', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis seguridades' AND stages = ARRAY[2, 3]), 'Valorarse', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis seguridades' AND stages = ARRAY[2, 3]), 'La escalera', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis seguridades' AND stages = ARRAY[2, 3]), 'El paraguas', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Conozco mis emociones', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Conozco mis emociones' AND stages = ARRAY[2, 3]), 'Hacer algo por los demás', true),
    ((SELECT id FROM vital_situation WHERE title = 'Conozco mis emociones' AND stages = ARRAY[2, 3]), 'El monstruo de colores', true),
    ((SELECT id FROM vital_situation WHERE title = 'Conozco mis emociones' AND stages = ARRAY[2, 3]), '¿Cómo me afectan las...', true),
    ((SELECT id FROM vital_situation WHERE title = 'Conozco mis emociones' AND stages = ARRAY[2, 3]), '¿Cómo expresamos las...', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Consecuencias de nuestras relaciones', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Consecuencias de nuestras relaciones' AND stages = ARRAY[2, 3]), 'Le he fallado a alguien', true),
    ((SELECT id FROM vital_situation WHERE title = 'Consecuencias de nuestras relaciones' AND stages = ARRAY[2, 3]), 'Creer lo primero que te dicen', true),
    ((SELECT id FROM vital_situation WHERE title = 'Consecuencias de nuestras relaciones' AND stages = ARRAY[2, 3]), 'Jóvenes que dejan huella', true),
    ((SELECT id FROM vital_situation WHERE title = 'Consecuencias de nuestras relaciones' AND stages = ARRAY[2, 3]), 'El conflicto', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Modelos ideales y Jesús como modelo', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Modelos ideales y Jesús como modelo' AND stages = ARRAY[2, 3]), 'Los modelos de mi vida', true),
    ((SELECT id FROM vital_situation WHERE title = 'Modelos ideales y Jesús como modelo' AND stages = ARRAY[2, 3]), 'El hombre de las manos atadas', true),
    ((SELECT id FROM vital_situation WHERE title = 'Modelos ideales y Jesús como modelo' AND stages = ARRAY[2, 3]), 'Rico, como el joven rico', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Mis amigos me dicen cómo soy', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me dicen cómo soy' AND stages = ARRAY[2, 3]), 'Mi confianza en los demás', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me dicen cómo soy' AND stages = ARRAY[2, 3]), 'La verdadera amistad', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me dicen cómo soy' AND stages = ARRAY[2, 3]), 'La silla de la verdad', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mis amigos me dicen cómo soy' AND stages = ARRAY[2, 3]), '¿Confío en mis amigos?', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Me gustas - te gusto', ARRAY[2, 3], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Me gustas - te gusto' AND stages = ARRAY[2, 3]), '¡Cómo me gusta mi imagen!', true),
    ((SELECT id FROM vital_situation WHERE title = 'Me gustas - te gusto' AND stages = ARRAY[2, 3]), 'Todos vamos en el mismo...', true),
    ((SELECT id FROM vital_situation WHERE title = 'Me gustas - te gusto' AND stages = ARRAY[2, 3]), 'Cartel publicitario', true);

-- ETAPA CAFARNAÚM (stages 4 y 5)
INSERT INTO vital_situation (title, stages, is_default) VALUES ('¿Cómo soy? ¿Cómo quiero ser?', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¿Cómo soy? ¿Cómo quiero ser?' AND stages = ARRAY[4, 5]), 'El árbol de la vida', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Cómo soy? ¿Cómo quiero ser?' AND stages = ARRAY[4, 5]), 'Dar fruto', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Cómo soy? ¿Cómo quiero ser?' AND stages = ARRAY[4, 5]), 'Mi yo 3.0', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Cómo soy? ¿Cómo quiero ser?' AND stages = ARRAY[4, 5]), 'Rayos X', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('El compromiso con los pobres', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'El compromiso con los pobres' AND stages = ARRAY[4, 5]), 'La pobreza "globalizada"', true),
    ((SELECT id FROM vital_situation WHERE title = 'El compromiso con los pobres' AND stages = ARRAY[4, 5]), 'Testimonio', true),
    ((SELECT id FROM vital_situation WHERE title = 'El compromiso con los pobres' AND stages = ARRAY[4, 5]), 'Buenas intenciones', true),
    ((SELECT id FROM vital_situation WHERE title = 'El compromiso con los pobres' AND stages = ARRAY[4, 5]), 'Optando', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('El mundo visto desde el otro', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'El mundo visto desde el otro' AND stages = ARRAY[4, 5]), 'Los "sin hogar"', true),
    ((SELECT id FROM vital_situation WHERE title = 'El mundo visto desde el otro' AND stages = ARRAY[4, 5]), 'Discapacitate', true),
    ((SELECT id FROM vital_situation WHERE title = 'El mundo visto desde el otro' AND stages = ARRAY[4, 5]), 'Siendo un inmigrante', true),
    ((SELECT id FROM vital_situation WHERE title = 'El mundo visto desde el otro' AND stages = ARRAY[4, 5]), '¡Pringate!', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Mi grupo de amigos y yo', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Mi grupo de amigos y yo' AND stages = ARRAY[4, 5]), 'Amistad virtual', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mi grupo de amigos y yo' AND stages = ARRAY[4, 5]), 'Los prejuicios', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mi grupo de amigos y yo' AND stages = ARRAY[4, 5]), 'Consejos para ser buen amigo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Mi grupo de amigos y yo' AND stages = ARRAY[4, 5]), 'Los demás me definen', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('No puedo llegar a todo', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'No puedo llegar a todo' AND stages = ARRAY[4, 5]), 'Aprendo a elegir', true),
    ((SELECT id FROM vital_situation WHERE title = 'No puedo llegar a todo' AND stages = ARRAY[4, 5]), 'Cuestión de prioridades', true),
    ((SELECT id FROM vital_situation WHERE title = 'No puedo llegar a todo' AND stages = ARRAY[4, 5]), 'Crezco con mis limitaciones', true),
    ((SELECT id FROM vital_situation WHERE title = 'No puedo llegar a todo' AND stages = ARRAY[4, 5]), 'Debilidades y fortalezas', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('¿Qué es ser adulto?', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¿Qué es ser adulto?' AND stages = ARRAY[4, 5]), 'Preparo una fiesta', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Qué es ser adulto?' AND stages = ARRAY[4, 5]), 'Tener éxito', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Qué es ser adulto?' AND stages = ARRAY[4, 5]), 'Somos catequistas', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Qué es ser adulto?' AND stages = ARRAY[4, 5]), 'Tomando decisiones', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('¿Qué imagen doy? ¿Cómo me ven?', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¿Qué imagen doy? ¿Cómo me ven?' AND stages = ARRAY[4, 5]), '¿Cómo quién soy?', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Qué imagen doy? ¿Cómo me ven?' AND stages = ARRAY[4, 5]), '¿Con quién?', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Qué imagen doy? ¿Cómo me ven?' AND stages = ARRAY[4, 5]), 'Recibo mi imagen', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Qué imagen doy? ¿Cómo me ven?' AND stages = ARRAY[4, 5]), 'Vida real Vs redes sociales', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Quiero ser feliz', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Quiero ser feliz' AND stages = ARRAY[4, 5]), '¿Qué me hace feliz?', true),
    ((SELECT id FROM vital_situation WHERE title = 'Quiero ser feliz' AND stages = ARRAY[4, 5]), '¿Mi Iglesia es feliz?', true),
    ((SELECT id FROM vital_situation WHERE title = 'Quiero ser feliz' AND stages = ARRAY[4, 5]), 'La Fe ayuda a la Felicidad', true),
    ((SELECT id FROM vital_situation WHERE title = 'Quiero ser feliz' AND stages = ARRAY[4, 5]), 'Tus prioridades', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Sensibilidad ante las injusticias', ARRAY[4, 5], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Sensibilidad ante las injusticias' AND stages = ARRAY[4, 5]), 'Viviendo la injusticia', true),
    ((SELECT id FROM vital_situation WHERE title = 'Sensibilidad ante las injusticias' AND stages = ARRAY[4, 5]), 'Consumismo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Sensibilidad ante las injusticias' AND stages = ARRAY[4, 5]), 'Me afectan las noticias del...', true),
    ((SELECT id FROM vital_situation WHERE title = 'Sensibilidad ante las injusticias' AND stages = ARRAY[4, 5]), 'Opiniones diferentes', true);

-- ETAPA BETANIA (stages 6 y 7)
INSERT INTO vital_situation (title, stages, is_default) VALUES ('¿Esta tribu me atrae?', ARRAY[6, 7], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¿Esta tribu me atrae?' AND stages = ARRAY[6, 7]), 'Sígueme', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Esta tribu me atrae?' AND stages = ARRAY[6, 7]), 'Sigo en Salle Joven', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Esta tribu me atrae?' AND stages = ARRAY[6, 7]), '¿Qué me falta?', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Relaciones positivas y negativas', ARRAY[6, 7], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Relaciones positivas y negativas' AND stages = ARRAY[6, 7]), 'Testimonio', true),
    ((SELECT id FROM vital_situation WHERE title = 'Relaciones positivas y negativas' AND stages = ARRAY[6, 7]), 'Mis espacios en el mundo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Relaciones positivas y negativas' AND stages = ARRAY[6, 7]), 'Línea del tiempo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Relaciones positivas y negativas' AND stages = ARRAY[6, 7]), '¿Cómo te relacionas?', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Consumismo de experiencias', ARRAY[6, 7], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Consumismo de experiencias' AND stages = ARRAY[6, 7]), 'Personas que importan', true),
    ((SELECT id FROM vital_situation WHERE title = 'Consumismo de experiencias' AND stages = ARRAY[6, 7]), 'Mi imagen de Dios', true),
    ((SELECT id FROM vital_situation WHERE title = 'Consumismo de experiencias' AND stages = ARRAY[6, 7]), '¿A qué dedico mi tiempo?', true),
    ((SELECT id FROM vital_situation WHERE title = 'Consumismo de experiencias' AND stages = ARRAY[6, 7]), 'Vida contemplativa', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('¿Tengo todo lo que quiero?', ARRAY[6, 7], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = '¿Tengo todo lo que quiero?' AND stages = ARRAY[6, 7]), 'Sobrio, austero y religioso', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Tengo todo lo que quiero?' AND stages = ARRAY[6, 7]), 'Presupuesto personal', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Tengo todo lo que quiero?' AND stages = ARRAY[6, 7]), 'El Dios de los pobres', true),
    ((SELECT id FROM vital_situation WHERE title = '¿Tengo todo lo que quiero?' AND stages = ARRAY[6, 7]), 'Dioses del mundo', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Vivir a tope...', ARRAY[6, 7], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Vivir a tope...' AND stages = ARRAY[6, 7]), 'Voluntarios por el mundo', true),
    ((SELECT id FROM vital_situation WHERE title = 'Vivir a tope...' AND stages = ARRAY[6, 7]), 'UBUNTU', true),
    ((SELECT id FROM vital_situation WHERE title = 'Vivir a tope...' AND stages = ARRAY[6, 7]), 'La fiesta de Dios', true);

INSERT INTO vital_situation (title, stages, is_default) VALUES ('Soy protagonista de mi vida', ARRAY[6, 7], true);
INSERT INTO vital_situation_session (vital_situation_id, title, is_default) VALUES 
    ((SELECT id FROM vital_situation WHERE title = 'Soy protagonista de mi vida' AND stages = ARRAY[6, 7]), 'Soy llamado', true),
    ((SELECT id FROM vital_situation WHERE title = 'Soy protagonista de mi vida' AND stages = ARRAY[6, 7]), 'Escala de valores', true),
    ((SELECT id FROM vital_situation WHERE title = 'Soy protagonista de mi vida' AND stages = ARRAY[6, 7]), 'Bajar a lo profundo', true);

