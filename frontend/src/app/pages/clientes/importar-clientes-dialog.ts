import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ImportacaoClientes, SituacaoImportacao } from '../../models/cliente';
import { ClienteService } from '../../services/cliente.service';
import { formatarTelefone } from '../../utils/utils';

/** Importa clientes de um CSV: primeiro mostra a prévia, depois grava os novos. Fecha com o resultado. */
@Component({
  selector: 'app-importar-clientes-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './importar-clientes-dialog.html',
  styleUrl: './importar-clientes-dialog.css',
})
export class ImportarClientesDialog {
  arquivo?: File;
  previa?: ImportacaoClientes;
  carregando = false;
  erro = '';

  constructor(
    private dialogRef: MatDialogRef<ImportarClientesDialog, ImportacaoClientes>,
    private clienteService: ClienteService,
    private cdr: ChangeDetectorRef
  ) { }

  selecionar(event: Event) {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    input.value = '';
    if (!arquivo) return;
    this.arquivo = arquivo;
    this.previa = undefined;
    this.enviar(false);
  }

  importar() {
    if (this.arquivo && this.previa?.incluidos) this.enviar(true);
  }

  private enviar(confirmar: boolean) {
    this.carregando = true;
    this.erro = '';
    this.clienteService.importar(this.arquivo!, confirmar).subscribe({
      next: resultado => {
        this.carregando = false;
        if (confirmar) {
          this.dialogRef.close(resultado);
          return;
        }
        this.previa = resultado;
        this.cdr.detectChanges();
      },
      error: err => {
        this.carregando = false;
        this.erro = typeof err.error === 'string' ? err.error : 'Erro ao ler o arquivo.';
        this.cdr.detectChanges();
      }
    });
  }

  baixarModelo() {
    // BOM para o Excel abrir os acentos corretamente
    const conteudo = String.fromCharCode(0xfeff) + 'Nome;CPF;Telefone;Observações\r\nMARIA DA SILVA;;(81) 99999-9999;\r\n';
    const url = URL.createObjectURL(new Blob([conteudo], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'modelo-importacao-clientes.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  rotulo(situacao: SituacaoImportacao): string {
    return { NOVO: 'Novo', DUPLICADO: 'Já cadastrado', ERRO: 'Erro' }[situacao];
  }

  /** Celular sem DDD não tem máscara: mostra como veio */
  telefone(valor?: string): string {
    const formatado = formatarTelefone(valor);
    return formatado === '—' && valor ? valor : formatado;
  }

  fechar() { this.dialogRef.close(); }
}
