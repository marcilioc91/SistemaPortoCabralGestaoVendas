import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { VendaService } from '../../services/venda.service';
import { RelatorioInventarioItem } from '../../models/venda';

@Component({
  selector: 'app-relatorio-inventario',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule,
  ],
  templateUrl: './relatorio-inventario.html',
  styleUrl: './relatorio-inventario.css',
})
export class RelatorioInventario implements OnInit {
  itens: RelatorioInventarioItem[] = [];
  carregando = true;
  colunas = ['nomeCategoria', 'nomeProduto', 'quantidadeVendida', 'totalReceita', 'totalCusto', 'lucro'];
  totalReceita = 0;
  totalCusto = 0;
  totalLucro = 0;

  constructor(private vendaService: VendaService, private router: Router, private cdRef: ChangeDetectorRef) {}

  ngOnInit() {
    this.vendaService.getRelatorioInventario().subscribe({
      next: (dados) => {
        this.itens = dados;

        this.totalReceita = this.itens.reduce((s, i) => s + (i.totalReceita ?? 0), 0);
        this.totalCusto = this.itens.reduce((s, i) => s + (i.totalCusto ?? 0), 0);
        this.totalLucro = this.itens.reduce((s, i) => s + (i.lucro ?? 0), 0);

        this.carregando = false;
        this.cdRef.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar relatório de inventário:', err);
        this.carregando = false;
      },
    });
  }

  voltar() {
    this.router.navigate(['/home']);
  }
  trackByProduto(index: number, item: RelatorioInventarioItem) {
    return item.nomeProduto;
  }
}
