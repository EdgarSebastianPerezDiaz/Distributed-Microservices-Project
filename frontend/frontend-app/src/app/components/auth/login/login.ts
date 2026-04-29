import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth';
import { LoginRequest } from '../../../models/auth.model';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  error: string | null = null;
  returnUrl: string | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.initializeForm();
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  initializeForm() {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit() {
    this.submitted = true;
    this.error = null;

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    const credentials: LoginRequest = {
      username: this.f['username'].value,
      password: this.f['password'].value
    };

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.loading = false;
        // Redirige según el rol del usuario
        const role = response.user.role;
        let redirectUrl = this.returnUrl || '/dashboard';
        
        if (role === 'ADMINISTRADOR') {
          redirectUrl = '/users';
        } else if (role === 'FUNCIONARIO') {
          redirectUrl = '/contratos';
        } else if (role === 'AUDITOR') {
          redirectUrl = '/auditoria';
        }

        this.router.navigate([redirectUrl]);
      },
      error: (error) => {
        this.loading = false;
        console.error('Login error:', error);
        
        if (error.status === 401) {
          this.error = 'Credenciales inválidas. Por favor, verifica tu usuario y contraseña.';
        } else if (error.status === 403) {
          this.error = 'Usuario inactivo o sin permisos de acceso.';
        } else if (error.status === 0) {
          this.error = 'No se pudo conectar al servidor. Verifica que el backend esté corriendo en http://localhost:8081';
        } else {
          this.error = error.error?.message || 'Error al iniciar sesión. Intenta de nuevo.';
        }
      }
    });
  }
}
