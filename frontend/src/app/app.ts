import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TemaService } from './services/tema.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('porto-cabral');

  // Instanciado na abertura para aplicar o tema salvo (claro/escuro) em qualquer tela inicial
  constructor(_tema: TemaService) {}
}
