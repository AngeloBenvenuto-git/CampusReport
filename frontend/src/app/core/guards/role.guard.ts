import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Consente l'accesso solo se il ruolo dell'utente è presente nella route data
 * (chiave "ruoli"), altrimenti reindirizza al login.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const ruoliAmmessi = route.data['ruoli'] as string[] | undefined;
  const ruoloUtente = authService.getRuolo();

  if (ruoloUtente && (!ruoliAmmessi || ruoliAmmessi.includes(ruoloUtente))) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
