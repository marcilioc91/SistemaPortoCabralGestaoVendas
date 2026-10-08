import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Categoria } from '../models/categoria';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private api = 'http://localhost:8080/categorias';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private headers(): HttpHeaders {
    const u = this.auth.getUsuarioLogado();
    return new HttpHeaders({
      'X-Usuario-Id': u?.id?.toString() ?? '',
      'X-Usuario-Nome': u?.pessoa?.nome ?? u?.usuarioLogin ?? '',
    });
  }

  listar() {
    return this.http.get<Categoria[]>(this.api);
  }

  salvar(categoria: Categoria) {
    return this.http.post<Categoria>(this.api, categoria, { headers: this.headers() });
  }

  atualizar(id: number, categoria: Categoria) {
    return this.http.put<Categoria>(`${this.api}/${id}`, categoria, { headers: this.headers() });
  }

  excluir(id: number) {
    return this.http.delete(`${this.api}/${id}`, { headers: this.headers() });
  }
}
