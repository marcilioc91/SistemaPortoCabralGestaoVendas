@echo off
setlocal
chcp 65001 > nul

echo.
echo ============================================
echo   MIGRACAO SQL Server -^> Firebird - Porto Cabral
echo ============================================
echo.
echo Uso: migrar-mssql.bat [banco_sqlserver] [pasta_destino]
echo   banco_sqlserver : nome do banco restaurado no SQL Server local (padrao: PortoCabral_Origem)
echo   pasta_destino   : pasta VAZIA onde sera gerado o PORTOCABRAL.FDB (padrao: .\migracao)
echo   Credenciais     : perguntadas na execucao, ou pelas variaveis SQL_USUARIO e SQL_SENHA
echo.

set ROOT=%~dp0
set JAR=%ROOT%backend\target\backend-0.0.1-SNAPSHOT.jar

set BANCO=%~1
if "%BANCO%"=="" set BANCO=PortoCabral_Origem

set DESTINO=%~2
if "%DESTINO%"=="" set DESTINO=%ROOT%migracao

if not exist "%JAR%" (
    echo ERRO: backend nao compilado. Rode antes: cd backend ^&^& mvnw.cmd package -DskipTests
    pause
    exit /b 1
)

rem O proprio backend recusa um destino que ja tenha dados (um banco vazio de tentativa anterior e aceito)

rem Credenciais: perguntadas aqui ou informadas antes pelas variaveis SQL_USUARIO / SQL_SENHA
if not defined SQL_USUARIO set /p SQL_USUARIO=Usuario do SQL Server [sa]:
if not defined SQL_USUARIO set SQL_USUARIO=sa
if not defined SQL_SENHA set /p SQL_SENHA=Senha do SQL Server:

echo.
echo Origem : SQL Server localhost, banco %BANCO%
echo Destino: %DESTINO%\PORTOCABRAL.FDB
echo.

java --enable-native-access=ALL-UNNAMED -jar "%JAR%" ^
  --spring.main.web-application-type=none ^
  --app.dados.dir="%DESTINO%" ^
  "--app.migracao.origem-url=jdbc:sqlserver://localhost:1433;databaseName=%BANCO%;encrypt=true;trustServerCertificate=true" ^
  --app.migracao.origem-usuario=%SQL_USUARIO% ^
  --app.migracao.origem-senha=%SQL_SENHA%

if %errorlevel% neq 0 (
    echo.
    echo ERRO: a migracao falhou. Veja a mensagem "MIGRACAO FALHOU" acima.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   MIGRACAO CONCLUIDA
echo ============================================
echo Banco gerado: %DESTINO%\PORTOCABRAL.FDB
echo Copie esse arquivo para C:\PortoCabral\dados\ na maquina de destino (com o sistema fechado).
echo.
pause
