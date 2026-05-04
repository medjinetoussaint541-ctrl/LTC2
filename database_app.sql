-- 1. Table USERS (Gère l'authentification)
CREATE TABLE USERS (
    userId NUMBER PRIMARY KEY,
    email VARCHAR2(100) UNIQUE NOT NULL,
    password VARCHAR2(255), -- Peut être NULL si auth via Google
    authProvider VARCHAR2(20) DEFAULT 'MANUAL',
    role VARCHAR2(20) DEFAULT 'USER',
    statutLine VARCHAR2(20) DEFAULT 'OFFLINE' NOT NULL,
    visibilite VARCHAR2(10) DEFAULT 'ON' NOT NULL,
    lastSeen TIMESTAMP,
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT chk_auth CHECK (authProvider IN ('MANUAL', 'GOOGLE')),
    CONSTRAINT chk_statut_line CHECK (statutLine IN ('ONLINE', 'OFFLINE')),
    CONSTRAINT chk_visibilite CHECK (visibilite IN ('ON', 'OFF'))
);

-- 2. Table PERSONS (Gère le profil public)
CREATE TABLE PERSONS (
    personId NUMBER PRIMARY KEY,
    userId NUMBER UNIQUE NOT NULL, -- UNIQUE garantit une relation 1-to-1 avec USERS
    nom VARCHAR2(50),
    prenom VARCHAR2(50),
    sexe VARCHAR2(1),
    faceId VARCHAR2(255) UNIQUE, -- ID retourné par CompreFace
    photoUrl VARCHAR2(500), -- Lien externe (ex: AWS S3, Cloudinary)
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sexe CHECK (sexe IN ('M', 'F')),
    CONSTRAINT fk_userId_person FOREIGN KEY (userId) REFERENCES USERS(userId) ON DELETE CASCADE
);

-- 3. Table DEMANDE
CREATE TABLE DEMANDE (
    demandeId NUMBER PRIMARY KEY,
    idDemandeur NUMBER NOT NULL,
    idReceveur NUMBER NOT NULL,
    statut VARCHAR2(20) DEFAULT 'EN ATTENTE',
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_demandeur FOREIGN KEY (idDemandeur) REFERENCES USERS(userId),
    CONSTRAINT fk_receveur FOREIGN KEY (idReceveur) REFERENCES USERS(userId),
    CONSTRAINT chk_statut_demande CHECK (statut IN ('EN ATTENTE', 'ACCEPTEE', 'REFUSEE')),
    CONSTRAINT chk_diff_users_demande CHECK (idDemandeur <> idReceveur) -- Empêche l'auto-demande
);

-- 4. Table RELATIONS
CREATE TABLE RELATIONS (
    relationId NUMBER PRIMARY KEY,
    user1Id NUMBER NOT NULL,
    user2Id NUMBER NOT NULL,
    statut VARCHAR2(20) DEFAULT 'EN COUPLE',
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    endDate TIMESTAMP, -- Pratique pour savoir QUAND ils sont devenus "EX"
    user1LastReadMessageId NUMBER,
    user2LastReadMessageId NUMBER,
    CONSTRAINT fk_user1 FOREIGN KEY (user1Id) REFERENCES USERS(userId),
    CONSTRAINT fk_user2 FOREIGN KEY (user2Id) REFERENCES USERS(userId),
    CONSTRAINT chk_statut_relation CHECK (statut IN ('EN COUPLE', 'EX')),
    CONSTRAINT chk_diff_users_rel CHECK (user1Id <> user2Id) -- Empêche d'être en couple avec soi-même
);

-- 5. Table CHAT_MESSAGES
CREATE TABLE CHAT_MESSAGES (
    messageId NUMBER PRIMARY KEY,
    relationId NUMBER NOT NULL,
    senderId NUMBER NOT NULL,
    cipherText CLOB NOT NULL,
    messageIv VARCHAR2(64) NOT NULL,
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_relation FOREIGN KEY (relationId) REFERENCES RELATIONS(relationId) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (senderId) REFERENCES USERS(userId)
);

-- 6. Table HISTORIQUE (Pour l'audit et les triggers)
CREATE TABLE HISTORIQUE (
    historiqueId NUMBER PRIMARY KEY,
    typeAction VARCHAR2(50) NOT NULL,
    description VARCHAR2(500) NOT NULL,
    dateAction TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Trigger pour les actions sur la table demande et relation
CREATE OR REPLACE TRIGGER trg_historique_demande
AFTER INSERT OR UPDATE ON DEMANDE
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        INSERT INTO HISTORIQUE (typeAction, description)
        VALUES ('TENTATIVE_AJOUT', 'L''utilisateur ' || :NEW.idDemandeur || ' a tenté d''ajouter l''utilisateur ' || :NEW.idReceveur);
    ELSIF UPDATING AND :NEW.statut = 'REFUSEE' AND :OLD.statut = 'EN ATTENTE' THEN
        INSERT INTO HISTORIQUE (typeAction, description)
        VALUES ('DEMANDE_REFUSEE', 'L''utilisateur ' || :NEW.idReceveur || ' a refusé la demande de ' || :NEW.idDemandeur);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_historique_relation
AFTER INSERT OR UPDATE ON RELATIONS
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        INSERT INTO HISTORIQUE (typeAction, description)
        VALUES ('NOUVEAU_COUPLE', 'L''utilisateur ' || :NEW.user1Id || ' est en couple avec l''utilisateur ' || :NEW.user2Id);
    ELSIF UPDATING AND :NEW.statut = 'EX' AND :OLD.statut = 'EN COUPLE' THEN
        INSERT INTO HISTORIQUE (typeAction, description)
        VALUES ('RUPTURE', 'L''utilisateur ' || :NEW.user1Id || ' a rompu avec l''utilisateur ' || :NEW.user2Id);
    END IF;
END;
/

-- 8. Sequences (PK auto-increment Oracle)
CREATE SEQUENCE SEQ_USERS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_PERSONS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_DEMANDE START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_RELATIONS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_CHAT_MESSAGES START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_HISTORIQUE START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 9. Trigger pour l'auto incrementation
create or replace TRIGGER trg_persons_pk
BEFORE INSERT ON PERSONS
FOR EACH ROW
WHEN (NEW.personId IS NULL)
BEGIN
    SELECT SEQ_PERSONS.NEXTVAL INTO :NEW.personId FROM DUAL;
END;
/

create or replace TRIGGER trg_users_pk
BEFORE INSERT ON USERS
FOR EACH ROW
WHEN (NEW.userId IS NULL)
BEGIN
   SELECT SEQ_USERS.NEXTVAL INTO :NEW.userId FROM DUAL;
END;
/

create or replace trigger trg_demande_pk
BEFORE INSERT ON DEMANDE
FOR EACH ROW
WHEN (NEW.demandeId IS NULL)
BEGIN
    SELECT SEQ_DEMANDE.NEXTVAL INTO :NEW.demandeId FROM DUAL;
END;
/

create or replace trigger trg_relation_pk
BEFORE INSERT ON RELATIONS
FOR EACH ROW
WHEN(NEW.relationId IS NULL)
BEGIN
    SELECT SEQ_RELATIONS.NEXTVAL INTO :NEW.relationId FROM DUAL;
END;
/

create or replace trigger trg_chat_message_pk
BEFORE INSERT ON CHAT_MESSAGES
FOR EACH ROW
WHEN(NEW.messageId IS NULL)
BEGIN
    SELECT SEQ_CHAT_MESSAGES.NEXTVAL INTO :NEW.messageId FROM DUAL;
END;
/

create or replace trigger trg_historique_pk
BEFORE INSERT ON HISTORIQUE
FOR EACH ROW
WHEN(NEW.historiqueId IS NULL)
BEGIN
    SELECT SEQ_HISTORIQUE.NEXTVAL INTO :NEW.historiqueId FROM DUAL;
END;
/

create or replace TRIGGER trg_historique_crush
AFTER INSERT OR UPDATE ON CRUSHES
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        INSERT INTO HISTORIQUE (typeAction, description)
        VALUES ('NOUVEAU_CRUSH', 'L''utilisateur ' || :NEW.ownerid || ' crush sur l''utilisateur ' || :NEW.targetid);
    END IF;
END;

--oracle job pour rendre les messages ephemeres, max 24h
BEGIN
    DBMS_SCHEDULER.CREATE_JOB (
        job_name        => 'DB_APP.JOB_DELETE_OLD_CHAT_MESSAGES',
        job_type        => 'PLSQL_BLOCK',
        job_action      => '
            BEGIN
                DELETE FROM DB_APP.CHAT_MESSAGES
                WHERE CREATIONDATE < SYSTIMESTAMP - INTERVAL ''24'' HOUR;
                COMMIT;
            END;',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=HOURLY; INTERVAL=1',
        enabled         => TRUE
    );
END;
/


ALTER TABLE USERS ADD (
    STATUTLINE VARCHAR2(20 CHAR) DEFAULT 'OFFLINE',
    LASTSEEN TIMESTAMP
);

UPDATE USERS
SET STATUTLINE = 'OFFLINE'
WHERE STATUTLINE IS NULL;

ALTER TABLE USERS MODIFY (
    STATUTLINE VARCHAR2(20 CHAR) NOT NULL
);

ALTER TABLE USERS ADD CONSTRAINT CHK_STATUT_LINE
CHECK (STATUTLINE IN ('ONLINE', 'OFFLINE'));
COMMIT;


ALTER TABLE USERS ADD (VISIBILITE VARCHAR2(10 CHAR) DEFAULT 'ON');
UPDATE USERS SET VISIBILITE = 'ON' WHERE VISIBILITE IS NULL;
ALTER TABLE USERS MODIFY (VISIBILITE VARCHAR2(10 CHAR) NOT NULL);
ALTER TABLE USERS ADD CONSTRAINT CHK_VISIBILITE CHECK (VISIBILITE IN ('ON', 'OFF'));

ALTER TABLE RELATIONS ADD (USER1LASTREADMESSAGEID NUMBER);
ALTER TABLE RELATIONS ADD (USER2LASTREADMESSAGEID NUMBER);
