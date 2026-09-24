import {
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import {from, switchMap} from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    return next(req);
  }

  return from(authService.updateToken()).pipe(
    switchMap(tokenUpdated => {

      if (!tokenUpdated) {
        return next(req);
      }

      const token = authService.getToken();

      if (!token) {
        return next(req);
      }

      return next(
        req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        })
      );
    })
  );
};
