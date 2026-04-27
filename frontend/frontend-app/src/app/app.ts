import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './components/shared/navbar/navbar';
import { AuthService } from './services/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule, NavbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly title = signal('Gestión de Proveedores');
  isLoggedIn = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.authService.isAuthenticated$.subscribe(isLoggedIn => {
      this.isLoggedIn = isLoggedIn;
      
      // Si no está autenticado y no está en login, redirige a login
      if (!isLoggedIn && !this.router.url.includes('/login')) {
        this.router.navigate(['/login']);
      }
    });
  }
}
