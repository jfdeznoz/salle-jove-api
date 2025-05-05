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
    address VARCHAR(255)
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
    birth_date DATE,
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

-- Insertar usuarios ADMINISTRADORES
INSERT INTO user_salle (name, last_name, email, password, roles, image_authorization, birth_date, gender, address, city)
VALUES
    ('Admin', 'Admin', 'admin@admin.com',
     '$2a$10$virGW4/FDxxZjUbhdZ9qH.O0kGu6TbfPUDoKLEzoIKD9F/T/TeOHK',
     'ROLE_ADMIN', true, '1990-01-01', 2, 'Calle Falsa 123', 'Madrid'),

    ('Pastoral Delegate', 'Delegate', 'pastoral@delegate.com',
     '$2a$10$J5dbEZ15J8YO6BnQZBJ6ke2IOwr0A5a9cKoy3yp/aev3QpouknnRW',
     'ROLE_PASTORAL_DELEGATE', true, '1985-05-15', 2, 'Avenida Siempre Viva 456', 'Barcelona');