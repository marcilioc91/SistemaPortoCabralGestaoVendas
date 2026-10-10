import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Produto } from '../models/produto';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class ProdutoService {
  private api = 'http://localhost:8080/produtos';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private headers(): HttpHeaders {
    const u = this.auth.getUsuarioLogado();
    return new HttpHeaders({
      'X-Usuario-Id': u?.id?.toString() ?? '',
      'X-Usuario-Nome': u?.pessoa?.nome ?? u?.usuarioLogin ?? '',
    });
  }

  listar() {
    return this.http.get<Produto[]>(this.api);
  }

  salvar(produto: Produto) {
    return this.http.post<Produto>(this.api, produto, { headers: this.headers() });
  }

  atualizar(id: number, produto: Produto) {
    return this.http.put<Produto>(`${this.api}/${id}`, produto, { headers: this.headers() });
  }

  excluir(id: number) {
    return this.http.delete(`${this.api}/${id}`, { headers: this.headers() });
  }

  /** URL da imagem do produto (null se não tiver); a versão evita mostrar a imagem antiga do cache */
  urlImagem(produto: Produto): string | null {
    return produto.id && produto.imagemVersao ? `${this.api}/${produto.id}/imagem?v=${produto.imagemVersao}` : null;
  }

  salvarImagem(id: number, imagem: Blob) {
    const form = new FormData();
    form.append('arquivo', imagem, 'imagem.jpg');
    return this.http.put(`${this.api}/${id}/imagem`, form, { headers: this.headers() });
  }

  removerImagem(id: number) {
    return this.http.delete(`${this.api}/${id}/imagem`, { headers: this.headers() });
  }
}
