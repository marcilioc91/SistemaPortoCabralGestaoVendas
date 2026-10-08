-- Indica que o usuário precisa definir uma nova senha no próximo login
-- (DDL separado do UPDATE: no Firebird a coluna nova só fica visível após o commit)

ALTER TABLE USUARIO ADD TROCAR_SENHA BOOLEAN DEFAULT FALSE NOT NULL;
