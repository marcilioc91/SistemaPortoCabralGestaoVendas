import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Cliente, ImportacaoClientes } from '../models/cliente';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class ClienteService {
  private api = 'http://localhost:8080/clientes';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private headers(): HttpHeaders {
    const u = this.auth.getUsuarioLogado();
    return new HttpHeaders({
      'X-Usuario-Id': u?.id?.toString() ?? '',
      'X-Usuario-Nome': u?.pessoa?.nome ?? u?.usuarioLogin ?? '',
    });
  }

  listar() {
    return this.http.get<Cliente[]>(this.api);
  }

  /** 409 com { semelhantes }: cadastro parecido já existe; reenviar com confirmarDuplicidade para gravar mesmo assim */
  salvar(cliente: Cliente, confirmarDuplicidade = false) {
    return this.http.post<Cliente>(this.api, cliente, {
      headers: this.headers(),
      params: { confirmarDuplicidade },
    });
  }

  /** confirmar=false: só a prévia; confirmar=true: grava os clientes novos */
  importar(arquivo: File, confirmar: boolean) {
    const dados = new FormData();
    dados.append('arquivo', arquivo);
    return this.http.post<ImportacaoClientes>(`${this.api}/importar`, dados, {
      headers: this.headers(),
      params: { confirmar },
    });
  }

  atualizar(id: number, cliente: Cliente) {
    return this.http.put<Cliente>(`${this.api}/${id}`, cliente, { headers: this.headers() });
  }

  excluir(id: number) {
    return this.http.delete(`${this.api}/${id}`, { headers: this.headers() });
  }
}
