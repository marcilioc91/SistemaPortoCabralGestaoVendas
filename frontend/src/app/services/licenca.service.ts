import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs';

export interface LicencaStatus {
  ativa: boolean;
  cliente: string | null;
  codigoMaquina: string | null;
  /** Motivo de a licença gravada não valer (ex.: copiada de outra máquina) */
  mensagem: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class LicencaService {
  private api = "http://localhost:8080/licenca"

  /** Depois de confirmada, não consulta o backend a cada navegação */
  private ativa = false;

  constructor(private http: HttpClient) {}

  status() {
    return this.http.get<LicencaStatus>(this.api + "/status").pipe(tap(s => (this.ativa = s.ativa)))
  }

  ativar(chave: string) {
    return this.http.post<LicencaStatus>(this.api + "/ativar", { chave }).pipe(tap(s => (this.ativa = s.ativa)))
  }

  estaAtiva(): boolean {
    return this.ativa;
  }

  /** Chamado quando a API responde 423 (sistema bloqueado) */
  marcarBloqueado() {
    this.ativa = false;
  }
}
