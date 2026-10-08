import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { Categoria } from '../../models/categoria';
import { Produto } from '../../models/produto';
import { CategoriaService } from '../../services/categoria.service';
import { ProdutoService } from '../../services/produto.service';

// ── Diálogo de novo/editar produto ────────────────────────────────────────────
@Component({
  selector: 'app-produto-form-dialog',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  templateUrl: './produto-form-dialog.html',
})
export class ProdutoFormDialog {
  dados: Produto;
  categoriaId: number | null;
  modoEdicao: boolean;
  erro = '';

  constructor(
    private dialogRef: MatDialogRef<ProdutoFormDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { produto?: Produto; categorias: Categoria[] } | null
  ) {
    this.modoEdicao = !!data?.produto;
    this.dados = data?.produto
      ? { ...data.produto }
      : { nome: '', preco: 0, preco_custo: 0, estoque: 0 };
    this.categoriaId = this.dados.categoria?.id ?? null;
  }

  confirmar() {
    if (this.dados.preco <= 0) {
      this.erro = 'O preço deve ser maior que zero.';
      return;
    }
    const categoria = this.data?.categorias.find(c => c.id === this.categoriaId) ?? null;
    this.dialogRef.close({ ...this.dados, categoria });
  }

  fechar() { this.dialogRef.close(); }
}

// ── Página de produtos ─────────────────────────────────────────────────────────
@Component({
  selector: 'app-produto-form',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatDialogModule,
    RouterLink,
  ],
  templateUrl: './produto-form.html',
  styleUrl: './produto-form.css',
})
export class ProdutoForm implements OnInit {
  produtos: Produto[] = [];
  categorias: Categoria[] = [];
  colunas = ['semEstoque', 'nome', 'categoria', 'preco', 'estoque', 'acoes'];

  constructor(
    private produtoService: ProdutoService,
    private categoriaService: CategoriaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.carregar();
    this.categoriaService.listar().subscribe({
      next: dados => this.categorias = dados,
      error: () => this.snackBar.open('Erro ao carregar categorias.', 'Fechar', { duration: 3000 })
    });
  }

  carregar() {
    this.produtoService.listar().subscribe({
      next: dados => {
        this.produtos = dados;
        this.cdr.detectChanges();
      },
      error: () => this.snackBar.open('Erro ao carregar produtos.', 'Fechar', { duration: 3000 })
    });
  }

  abrirFormulario() {
    const ref = this.dialog.open(ProdutoFormDialog, { width: '400px', data: { categorias: this.categorias } });
    ref.afterClosed().subscribe((dados: Produto | undefined) => {
      if (!dados) return;
      this.produtoService.salvar(dados).subscribe({
        next: () => {
          this.snackBar.open('Produto cadastrado com sucesso!', 'Fechar', { duration: 3000 });
          this.carregar();
        },
        error: () => this.snackBar.open('Erro ao cadastrar produto.', 'Fechar', { duration: 3000 })
      });
    });
  }

  abrirEdicao(produto: Produto) {
    const ref = this.dialog.open(ProdutoFormDialog, { width: '400px', data: { produto, categorias: this.categorias } });
    ref.afterClosed().subscribe((dados: Produto | undefined) => {
      if (!dados || !dados.id) return;
      this.produtoService.atualizar(dados.id, dados).subscribe({
        next: () => {
          this.snackBar.open('Produto atualizado!', 'Fechar', { duration: 3000 });
          this.carregar();
        },
        error: () => this.snackBar.open('Erro ao atualizar produto.', 'Fechar', { duration: 3000 })
      });
    });
  }

  marcarSemEstoque(produto: Produto, checked: boolean) {
    if (!produto.id || !checked) return;
    const atualizado = { ...produto, estoque: 0 };
    this.produtoService.atualizar(produto.id, atualizado).subscribe({
      next: () => {
        this.snackBar.open('Produto marcado como sem estoque.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Erro ao atualizar estoque.', 'Fechar', { duration: 3000 })
    });
  }

  excluir(produto: Produto) {
    if (!produto.id) return;
    this.produtoService.excluir(produto.id).subscribe({
      next: () => {
        this.snackBar.open('Produto excluído.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Erro ao excluir produto.', 'Fechar', { duration: 3000 })
    });
  }
}
