; Acoes extras do instalador NSIS (electron-builder)
; A pasta de dados fica FORA da pasta do programa: atualizacoes e desinstalacao nao apagam o banco.

!macro customInstall
  CreateDirectory "C:\PortoCabral\dados"
  ; Grupo Usuarios (SID S-1-5-32-545, independente do idioma do Windows) com permissao de modificar,
  ; para que qualquer usuario do Windows consiga gravar no banco
  nsExec::Exec 'icacls "C:\PortoCabral\dados" /grant *S-1-5-32-545:(OI)(CI)M'
  Pop $0
!macroend
