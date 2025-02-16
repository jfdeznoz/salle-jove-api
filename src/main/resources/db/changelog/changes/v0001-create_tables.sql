-- Script de Migración SQL para "Salle Joven"

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
    center INT,
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
    roles VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    event_date DATE NOT NULL,
    divided BOOLEAN DEFAULT FALSE
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
CREATE TABLE historicaldata (
    id SERIAL PRIMARY KEY,
    data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);