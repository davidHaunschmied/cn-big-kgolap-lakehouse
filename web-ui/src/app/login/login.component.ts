import {Component, OnInit} from '@angular/core';
import {AuthService} from '../auth/auth.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  un: string;
  pw: string;
  showLoginError: boolean;

  constructor(private authService: AuthService, private router: Router) {
    this.showLoginError = false;
  }

  ngOnInit(): void {
  }

  login() {
    this.showLoginError = false;
    this.authService.authenticate(this.un, this.pw).subscribe(user => {
      this.router.navigate(['']);
    }, error => this.showLoginError = true);
  }
}
