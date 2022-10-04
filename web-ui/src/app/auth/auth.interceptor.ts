import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {AuthService} from './auth.service';
import {environment} from '../../environments/environment';
import {Router} from '@angular/router';
import {catchError} from 'rxjs/operators';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) {
  }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // add header with basic auth credentials if user is logged in and request is to the api url
    const isApiUrl = request.url.startsWith(environment.surfaceBaseUrl);
    if (this.authService.isUserLoggedIn() && isApiUrl) {
      request = request.clone({
        setHeaders: {
          Authorization: `Basic ${this.authService.getUserAuthData()}`
        }
      });
    }

    return next.handle(request)
      .pipe(catchError(err => {
        if (err.status === 401) {
          this.router.navigate(['login']);
        }
        return throwError('Request to backend failed!');
      }));
  }
}
