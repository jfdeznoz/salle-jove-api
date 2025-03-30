-- ============================================
--        SCRIPT DE MIGRACIÓN SQL
-- ============================================

-- ============================================
--  CREACIÓN DE TABLAS
-- ============================================

-- Tabla de centros educativos
CREATE TABLE center (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL
);

-- Tabla de grupos
CREATE TABLE group_salle (
    id SERIAL PRIMARY KEY,
    stage INT NOT NULL,
    center INT NOT NULL,
    FOREIGN KEY (center) REFERENCES center(id)
);

-- Tabla principal de usuarios
CREATE TABLE user_salle (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    dni VARCHAR(20),
    phone VARCHAR(15),
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    tshirt_size INT,
    health_card_number VARCHAR(50),
    intolerances TEXT,
    chronic_diseases TEXT,
    image_authorization BOOLEAN NOT NULL,
    birth_date DATE NOT NULL,
    gender INT NOT NULL DEFAULT 2,
    address VARCHAR(255),
    city VARCHAR(255),
    roles VARCHAR(255) NOT NULL,
    mother_full_name VARCHAR(255),
    father_full_name VARCHAR(255),
    mother_email VARCHAR(100),
    father_email VARCHAR(100),
    father_phone VARCHAR(15),
    mother_phone VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Relación muchos a muchos entre usuarios y grupos
CREATE TABLE user_group (
    user_salle INT,
    group_salle INT,
    PRIMARY KEY (user_salle, group_salle),
    FOREIGN KEY (user_salle) REFERENCES user_salle(id),
    FOREIGN KEY (group_salle) REFERENCES group_salle(id)
);

-- Tabla de eventos
CREATE TABLE event (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    event_date DATE NOT NULL,
    file_name VARCHAR(255),
    place VARCHAR(255),
    divided BOOLEAN DEFAULT FALSE,
    stages INT[] NOT NULL,
    deleted_at TIMESTAMP
);

-- Relación muchos a muchos entre eventos y grupos
CREATE TABLE event_group (
    event INT,
    group_salle INT,
    PRIMARY KEY (event, group_salle),
    FOREIGN KEY (event) REFERENCES event(id),
    FOREIGN KEY (group_salle) REFERENCES group_salle(id)
);

-- Relación muchos a muchos entre eventos y usuarios
CREATE TABLE event_user (
    event INT,
    user_salle INT,
    status INT DEFAULT 0,
    PRIMARY KEY (event, user_salle),
    FOREIGN KEY (event) REFERENCES event(id),
    FOREIGN KEY (user_salle) REFERENCES user_salle(id)
);

-- Tabla de tokens de refresco
CREATE TABLE refresh_token (
    id SERIAL PRIMARY KEY,
    token VARCHAR(10000) NOT NULL,
    revoked BOOLEAN,
    user_salle INT,
    FOREIGN KEY (user_salle) REFERENCES user_salle(id)
);

-- Tabla para datos históricos
CREATE TABLE historical_data (
    id SERIAL PRIMARY KEY,
    data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--  INSERTS DE DATOS INICIALES
-- ============================================

-- Insertar centros educativos
INSERT INTO center (name, city, address)
VALUES 
    ('Colegio La Salle Madrid', 'Madrid', 'Calle Mayor 123'),
    ('Colegio La Salle Barcelona', 'Barcelona', 'Avenida Diagonal 456');

-- Insertar grupos en los centros creados
INSERT INTO group_salle (stage, center)
VALUES 
    (1, 1),
    (2, 1),
    (3, 1),
    (4, 1),
    (1, 2),
    (2, 2);

-- Insertar usuarios ADMINISTRADORES
INSERT INTO user_salle (name, last_name, email, password, roles, image_authorization, birth_date, gender, address, city)
VALUES
    ('Admin', 'Admin', 'admin@admin.com',
     '$2a$10$virGW4/FDxxZjUbhdZ9qH.O0kGu6TbfPUDoKLEzoIKD9F/T/TeOHK',
     'ROLE_ADMIN', true, '1990-01-01', 2, 'Calle Falsa 123', 'Madrid'),

    ('Pastoral Delegate', 'Delegate', 'pastoral@delegate.com',
     '$2a$10$J5dbEZ15J8YO6BnQZBJ6ke2IOwr0A5a9cKoy3yp/aev3QpouknnRW',
     'ROLE_PASTORAL_DELEGATE', true, '1985-05-15', 2, 'Avenida Siempre Viva 456', 'Barcelona');

-- Insertar 20 usuarios PARTICIPANTES y obtener sus IDs
WITH inserted_users AS (
    INSERT INTO user_salle (name, last_name, dni, phone, email, password, tshirt_size,
                            health_card_number, intolerances, chronic_diseases, image_authorization,
                            birth_date, roles, gender, address, city, mother_full_name, father_full_name, mother_email, father_email, father_phone, mother_phone)
    VALUES
        ('Pedro', 'García', '12345678A', '612345678', 'pedro.garcia@mail.com',
        '$2a$10$ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF12',
        2, 'HC123456', 'Gluten', 'Asma', true, '2005-04-12', 'ROLE_PARTICIPANT', 0, 'Calle Mayor 1', 'Madrid', 'María García', 'Juan García', 'maria.garcia@mail.com', 'juan.garcia@mail.com', '612345678', '687654321'),

        ('Marta', 'López', '87654321B', '687654321', 'marta.lopez@mail.com',
        '$2a$10$ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF12',
        3, 'HC654321', 'Lactosa', NULL, true, '2004-09-22', 'ROLE_PARTICIPANT', 1, 'Avenida Diagonal 2', 'Barcelona', 'María López', 'Juan López', 'maria.lopez@mail.com', 'juan.lopez@mail.com', '687654321', '699887766'),

        ('Luis', 'Fernández', '11223344C', '699887766', 'luis.fernandez@mail.com',
        '$2a$10$ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF12',
        1, 'HC998877', NULL, 'Diabetes', true, '2003-02-18', 'ROLE_PARTICIPANT', 0, 'Calle Gran Vía 3', 'Madrid', 'María Fernández', 'Juan Fernández', 'maria.fernandez@mail.com', 'juan.fernandez@mail.com', '699887766', '633221144'),

        ('Elena', 'Ruiz', '22334455D', '633221144', 'elena.ruiz@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        4, 'HC556677', 'Mariscos', NULL, true, '2006-06-30', 'ROLE_PARTICIPANT', 1, 'Paseo de Gracia 4', 'Barcelona', 'María Ruiz', 'Juan Ruiz', 'maria.ruiz@mail.com', 'juan.ruiz@mail.com', '633221144', '644332211'),

        ('Javier', 'Martínez', '33445566E', '644332211', 'javier.martinez@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        5, 'HC112233', NULL, NULL, true, '2002-12-10', 'ROLE_PARTICIPANT', 0, 'Calle Serrano 5', 'Madrid', 'María Martínez', 'Juan Martínez', 'maria.martinez@mail.com', 'juan.martinez@mail.com', '644332211', '600123456'),

        ('Alejandro', 'Torres', '44556677F', '600123456', 'alejandro.torres@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        2, 'HC445566', 'Frutos secos', 'Hipertensión', true, '2005-07-21', 'ROLE_PARTICIPANT', 0, 'Rambla de Catalunya 6', 'Barcelona', 'María Torres', 'Juan Torres', 'maria.torres@mail.com', 'juan.torres@mail.com', '600123456', '611234567'),

        ('Lucía', 'Santos', '55667788G', '611234567', 'lucia.santos@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        3, 'HC556677', 'Pescado', NULL, true, '2006-02-15', 'ROLE_PARTICIPANT', 1, 'Calle Alcalá 7', 'Madrid', 'María Santos', 'Juan Santos', 'maria.santos@mail.com', 'juan.santos@mail.com', '611234567', '622345678'),

        ('Sofía', 'Moreno', '66778899H', '622345678', 'sofia.moreno@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        4, 'HC667788', NULL, 'Asma', true, '2004-06-09', 'ROLE_PARTICIPANT', 1, 'Avenida Meridiana 8', 'Barcelona', 'María Moreno', 'Juan Moreno', 'maria.moreno@mail.com', 'juan.moreno@mail.com', '622345678', '633456789'),

        ('Manuel', 'Giménez', '77889900I', '633456789', 'manuel.gimenez@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        5, 'HC778899', 'Lácteos', NULL, true, '2003-12-25', 'ROLE_PARTICIPANT', 0, 'Calle Velázquez 9', 'Madrid', 'María Giménez', 'Juan Giménez', 'maria.gimenez@mail.com', 'juan.gimenez@mail.com', '633456789', '644567890'),

        ('Natalia', 'Rey', '88990011J', '644567890', 'natalia.rey@mail.com',
        '$2a$10$XYZABC1234567890XYZABC1234567890XYZABC1234567890XYZABC12',
        1, 'HC889900', NULL, 'Diabetes', true, '2002-03-18', 'ROLE_PARTICIPANT', 1, 'Gran Via de les Corts Catalanes 10', 'Barcelona', 'María Rey', 'Juan Rey', 'maria.rey@mail.com', 'juan.rey@mail.com', '644567890', '655678901')
    RETURNING id
)
-- Asignar usuarios a los grupos usando los IDs generados
INSERT INTO user_group (user_salle, group_salle)
SELECT id, (CASE WHEN id % 3 = 0 THEN 1 WHEN id % 3 = 1 THEN 2 ELSE 3 END) FROM inserted_users;

-- Insertar usuario adicional
INSERT INTO user_salle (
    name, last_name, dni, phone, email, password, tshirt_size,
    health_card_number, intolerances, chronic_diseases,
    image_authorization, birth_date, roles, gender, address, city, mother_full_name, father_full_name, mother_email, father_email, father_phone, mother_phone
)
VALUES (
    'Laura', 'Pérez', '99887766Z', '611112223', 'laura.perez@mail.com',
    '$2a$10$J5dbEZ15J8YO6BnQZBJ6ke2IOwr0A5a9cKoy3yp/aev3QpouknnRW',
    2, 'HC999888', NULL, NULL, true, '1995-08-10', 'ROLE_GROUP_LEADER', 1, 'Calle Balmes 11', 'Barcelona', 'María Pérez', 'Juan Pérez', 'maria.perez@mail.com', 'juan.perez@mail.com', '611112223', '622345678'
);

INSERT INTO user_group (user_salle, group_salle)
SELECT id, 1 FROM user_salle WHERE email = 'laura.perez@mail.com';