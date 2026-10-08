export interface Pessoa {
  id?: number;
  nome: string;
  cpf?: string;
  telefone?: string;
}

export interface Usuario {
  id?: number;
  pessoa?: Pessoa;
  usuarioLogin: string;
  email: string;
  senha?: string;
  perfil?: 'ADMIN' | 'OPERADOR';
  trocarSenha?: boolean;
}

export interface LoginRequest {
  login: string;
  senha: string;
}
