import { inject } from '@angular/core';
import {
  HttpInterceptorFn
} from '@angular/common/http';
import { from } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  // Public request
  if (!authService.isAuthenticated()) {
    return next(req);
  }

  return from(authService.updateToken(30)).pipe(

    switchMap((refreshed) => {

      if (!refreshed && !authService.isAuthenticated()) {
        return next(req);
      }

      const token = authService.getToken();

      if (!token) {
        return next(req);
      }

      const authenticatedRequest = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

      console.log('AuthInterceptor: Authorization header added');

      return next(authenticatedRequest);
    })
  );
};
