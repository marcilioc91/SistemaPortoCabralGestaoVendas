import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, retry, throwError, timer } from 'rxjs';
import { LicencaService } from '../services/licenca.service';

/** Consulta a licença; enquanto o backend ainda está subindo (sem resposta), tenta de novo por até 90s
 *  (em máquinas lentas o Java pode levar mais de 30s para iniciar) */
function consultarStatus(licenca: LicencaService) {
  return licenca.status().pipe(
    retry({
      count: 90,
      delay: (err: HttpErrorResponse) => (err.status === 0 ? timer(1000) : throwError(() => err)),
    })
  );
}

/** Sistema não ativado: manda para a tela de ativação */
export const licencaGuard: CanActivateFn = () => {
  const licenca = inject(LicencaService);
  const router = inject(Router);
  if (!isPlatformBrowser(inject(PLATFORM_ID)) || licenca.estaAtiva()) return true;
  return consultarStatus(licenca).pipe(
    map(s => (s.ativa ? true : router.createUrlTree(['/ativacao']))),
    // Sem resposta do backend: segue (a API continua bloqueada lá e o interceptor redireciona)
    catchError(() => of(true))
  );
};

/** Tela de ativação: se o sistema já estiver ativado, volta para o login */
export const ativacaoGuard: CanActivateFn = () => {
  const licenca = inject(LicencaService);
  const router = inject(Router);
  if (!isPlatformBrowser(inject(PLATFORM_ID))) return true;
  return consultarStatus(licenca).pipe(
    map(s => (s.ativa ? router.createUrlTree(['/']) : true)),
    catchError(() => of(true))
  );
};
