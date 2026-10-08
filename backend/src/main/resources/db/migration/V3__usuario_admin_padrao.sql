-- Usuário administrador padrão, disponível em toda instalação nova.
-- Login: admin | Senha: admin (hash BCrypt). Troque a senha após o primeiro acesso.
-- Não faz nada se já existir um usuário com login 'admin'.

INSERT INTO PESSOA (NOME)
SELECT 'ADMINISTRADOR' FROM RDB$DATABASE
WHERE NOT EXISTS (SELECT 1 FROM USUARIO WHERE USUARIO_LOGIN = 'admin');

INSERT INTO USUARIO (PESSOA_ID, USUARIO_LOGIN, EMAIL, SENHA, PERFIL, DATA_CRIACAO)
SELECT FIRST 1 p.ID, 'admin', 'admin@portocabral.local',
       '$2a$10$BNtzZY20dm2xGMrLjF87duIKmy/OvDNQbNQobyucR6IWhdnHyL.YO',
       'ADMIN', CURRENT_TIMESTAMP
FROM PESSOA p
WHERE p.NOME = 'ADMINISTRADOR'
  AND NOT EXISTS (SELECT 1 FROM USUARIO u WHERE u.PESSOA_ID = p.ID)
  AND NOT EXISTS (SELECT 1 FROM USUARIO WHERE USUARIO_LOGIN = 'admin')
ORDER BY p.ID DESC;
