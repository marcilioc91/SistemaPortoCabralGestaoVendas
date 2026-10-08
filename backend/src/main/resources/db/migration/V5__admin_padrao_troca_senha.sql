-- Admin padrão ainda com a senha de fábrica ('admin') deve trocá-la no primeiro acesso

UPDATE USUARIO SET TROCAR_SENHA = TRUE
WHERE USUARIO_LOGIN = 'admin'
  AND SENHA = '$2a$10$BNtzZY20dm2xGMrLjF87duIKmy/OvDNQbNQobyucR6IWhdnHyL.YO';
