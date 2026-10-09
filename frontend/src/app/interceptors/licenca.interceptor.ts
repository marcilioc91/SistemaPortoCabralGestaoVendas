import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { LicencaService } from '../services/licenca.service';

/** A API responde 423 (Locked) enquanto o sistema não está ativado: vai para a tela de ativação */
export const licencaInterceptor: HttpInterceptorFn = (req, next) => {
  const licenca = inject(LicencaService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 423) {
        licenca.marcarBloqueado();
        router.navigate(['/ativacao']);
      }
      return throwError(() => err);
    })
  );
};
