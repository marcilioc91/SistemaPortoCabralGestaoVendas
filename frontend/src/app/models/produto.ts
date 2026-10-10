import { Categoria } from './categoria';

export interface Produto {
  id?: number;
  nome: string;
  preco: number;
  preco_custo: number;
  estoque: number;
  categoria?: Categoria | null;
  /** Muda a cada troca de imagem; null = sem imagem */
  imagemVersao?: number | null;
}
