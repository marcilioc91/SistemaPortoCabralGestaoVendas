-- Imagem do produto (catalogo da tela de vendas)

-- Arquivo da imagem, separado de PRODUTO para a listagem de produtos nao carregar os bytes
CREATE TABLE PRODUTO_IMAGEM (
    PRODUTO_ID  BIGINT NOT NULL,
    TIPO        VARCHAR(100) NOT NULL,
    CONTEUDO    BLOB SUB_TYPE BINARY NOT NULL,
    CONSTRAINT PK_PRODUTO_IMAGEM PRIMARY KEY (PRODUTO_ID),
    CONSTRAINT FK_PRODUTO_IMAGEM_PRODUTO FOREIGN KEY (PRODUTO_ID) REFERENCES PRODUTO (ID)
);

-- Momento da ultima troca da imagem (NULL = sem imagem); vai na URL para o navegador nao usar a imagem antiga do cache
ALTER TABLE PRODUTO ADD IMAGEM_VERSAO BIGINT;
