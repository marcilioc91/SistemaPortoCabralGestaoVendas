import { Pessoa } from './usuario';

export interface Cliente {
  id?: number;
  pessoa: Pessoa;
  obs?: string;
}

/** Cadastro já existente que parece ser a mesma pessoa (resposta 409 do cadastro) */
export interface PessoaSemelhante {
  pessoaId: number;
  nome: string;
  telefone?: string;
  possuiUsuario: boolean;
}

export type SituacaoImportacao = 'NOVO' | 'DUPLICADO' | 'ERRO';

export interface LinhaImportacao {
  linha: number;
  nome: string;
  telefone?: string;
  obs?: string;
  situacao: SituacaoImportacao;
  motivo?: string;
  avisos: string[];
}

export interface ImportacaoClientes {
  /** false: só prévia, nada foi gravado */
  confirmado: boolean;
  incluidos: number;
  ignorados: number;
  erros: number;
  linhas: LinhaImportacao[];
}
