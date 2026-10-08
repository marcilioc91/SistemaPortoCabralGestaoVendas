import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

// ── Diálogo "Esqueci minha senha": 1) solicitar código  2) informar código + nova senha ──
@Component({
  selector: 'app-recuperar-senha-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './recuperar-senha-dialog.html',
  styleUrl: './recuperar-senha-dialog.css',
})
export class RecuperarSenhaDialog {
  etapa: 'solicitar' | 'redefinir' = 'solicitar';
  identificador = '';
  codigo = '';
  novaSenha = '';
  confirmacaoSenha = '';
  ocultarSenha = true;
  enviando = false;
  erro = '';
  aviso = '';

  constructor(
    private auth: AuthService,
    private dialogRef: MatDialogRef<RecuperarSenhaDialog>,
    private cdr: ChangeDetectorRef
  ) {}

  solicitar() {
    if (!this.identificador.trim() || this.enviando) return;
    this.enviando = true;
    this.erro = '';
    this.auth.solicitarRecuperacaoSenha(this.identificador.trim()).subscribe({
      next: msg => {
        this.enviando = false;
        this.aviso = msg;
        this.etapa = 'redefinir';
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => this.falha(err, 'Não foi possível solicitar o código.'),
    });
  }

  redefinir() {
    if (!this.podeRedefinir() || this.enviando) return;
    this.enviando = true;
    this.erro = '';
    this.auth.redefinirSenhaComCodigo(this.identificador.trim(), this.codigo.trim(), this.novaSenha).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err: HttpErrorResponse) => this.falha(err, 'Não foi possível redefinir a senha.'),
    });
  }

  podeRedefinir(): boolean {
    return /^\d{6}$/.test(this.codigo.trim())
      && !!this.novaSenha
      && this.novaSenha === this.confirmacaoSenha;
  }

  voltar() {
    this.etapa = 'solicitar';
    this.codigo = '';
    this.erro = '';
  }

  fechar() { this.dialogRef.close(false); }

  private falha(err: HttpErrorResponse, padrao: string) {
    this.enviando = false;
    if (err.status === 0) {
      this.erro = 'Erro de conexão. Tente novamente.';
    } else {
      this.erro = typeof err.error === 'string' && err.error ? err.error : padrao;
    }
    this.cdr.detectChanges();
  }
}
