import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';

import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

    const authService = inject(AuthService);

    console.log('AuthInterceptor:', req.url);

    if (!authService.isAuthenticated()) {
        console.log('Not authenticated');
        return next(req);
    }

    const token = authService.getToken();

    if (!token) {
        console.warn('Authenticated but no access token');
        return next(req);
    }

    console.log('Adding Authorization header');

    const authenticatedRequest = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

    return next(authenticatedRequest);

};
