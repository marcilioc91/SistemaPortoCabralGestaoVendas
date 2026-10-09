import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID, signal } from '@angular/core';

/**
 * Tema claro/escuro do sistema inteiro: a classe "tema-escuro" no <body> troca as variáveis de
 * cor (styles.css) e o esquema do Angular Material (material-theme.scss).
 * A escolha fica salva neste computador (localStorage).
 */
@Injectable({
  providedIn: 'root',
})
export class TemaService {
  private readonly chave = 'portocabral.tema';
  readonly escuro = signal(false);

  constructor(
    @Inject(DOCUMENT) private documento: Document,
    @Inject(PLATFORM_ID) private plataforma: object
  ) {
    if (!isPlatformBrowser(this.plataforma)) return;
    let salvo: string | null = null;
    try {
      salvo = localStorage.getItem(this.chave);
    } catch {
      // Armazenamento indisponível: começa no tema claro
    }
    this.aplicar(salvo === 'escuro');
  }

  alternar() {
    this.aplicar(!this.escuro());
    try {
      localStorage.setItem(this.chave, this.escuro() ? 'escuro' : 'claro');
    } catch {
      // Sem armazenamento: o tema vale só até fechar o sistema
    }
  }

  private aplicar(escuro: boolean) {
    this.escuro.set(escuro);
    if (isPlatformBrowser(this.plataforma)) {
      this.documento.body.classList.toggle('tema-escuro', escuro);
    }
  }
}
