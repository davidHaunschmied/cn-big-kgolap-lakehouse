import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../environments/environment';
import {Observable} from 'rxjs';

export class User {
  constructor(
    public name: string,
    public authData: string
  ) {
  }
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly AUTH_USER_KEY = 'auth-user';

  constructor(
    private httpClient: HttpClient
  ) {
  }

  authenticate(username, password): Observable<User> {
    const userAuthData = btoa(username + ':' + password);
    const headers = new HttpHeaders({Authorization: 'Basic ' + userAuthData});
    return this.httpClient.get<any>(environment.surfaceBaseUrl + '/validateLogin', {headers}).pipe(
      map(
        anything => {
          const user = new User(username, userAuthData);
          localStorage.setItem(this.AUTH_USER_KEY, JSON.stringify(user));
          return user;
        }
      ), catchError((err) => {
        localStorage.removeItem(this.AUTH_USER_KEY);
        throw err;
      }));
  }


  isUserLoggedIn(): boolean {
    const user = localStorage.getItem(this.AUTH_USER_KEY);
    return !(user === null);
  }

  getUserAuthData(): string {
    return this.getUser().authData;
  }

  getUser(): User {
    return JSON.parse(localStorage.getItem(this.AUTH_USER_KEY)) as User;
  }
}
