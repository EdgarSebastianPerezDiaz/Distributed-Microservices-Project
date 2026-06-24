import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { UserService } from '../../../services/user';
import { User, UserRole } from '../../../models/auth.model';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-user-form',
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatIconModule,
  ],
  templateUrl: './user-form.html',
  styleUrl: './user-form.scss',
})
export class UserFormComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  submitted = false;
  isEditMode = false;
  userId: string | null = null;
  error: string | null = null;

  UserRole = UserRole;

  readonly roleLabels: { value: UserRole; label: string }[] = [
    { value: UserRole.ADMINISTRADOR, label: 'Administrador' },
    { value: UserRole.FUNCIONARIO, label: 'Funcionario' },
    { value: UserRole.AUDITOR, label: 'Auditor' },
  ];

  constructor(
    private formBuilder: FormBuilder,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      this.isEditMode = !!id;
      this.userId = id;

      if (this.isEditMode && id) {
        this.initializeForm(true);
        this.loadUser(id);
      } else {
        this.initializeForm(false);
      }
    });
  }

  private static passwordsMatch(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirm = control.get('confirmPassword');
    if (!password || !confirm) {
      return null;
    }
    if (!password.value && !confirm.value) {
      return null;
    }
    return password.value === confirm.value ? null : { passwordMismatch: true };
  }

  initializeForm(editMode: boolean) {
    if (editMode) {
      this.form = this.formBuilder.group({
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        fullName: ['', [Validators.required]],
        role: [UserRole.FUNCIONARIO, [Validators.required]],
        active: [true, [Validators.required]],
      });
      return;
    }

    this.form = this.formBuilder.group(
      {
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        fullName: ['', [Validators.required]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        role: [UserRole.FUNCIONARIO, [Validators.required]],
        active: [true, [Validators.required]],
      },
      { validators: [UserFormComponent.passwordsMatch] },
    );
  }

  loadUser(id: string) {
    this.loading = true;
    this.userService.getUserById(id).subscribe({
      next: (user) => {
        this.form.patchValue({
          username: user.username,
          email: user.email,
          fullName: user.fullName,
          role: user.role,
          active: user.active !== false,
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user:', error);
        this.error = 'Error al cargar el usuario';
        this.loading = false;
      },
    });
  }

  get f() {
    return this.form.controls;
  }

  onSubmit() {
    this.submitted = true;
    this.error = null;

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    const base: User = {
      username: this.f['username'].value,
      email: this.f['email'].value,
      fullName: this.f['fullName'].value,
      role: this.f['role'].value,
      active: !!this.f['active'].value,
    };

    if (!this.isEditMode) {
      base.password = this.f['password'].value;
    }

    const request =
      this.isEditMode && this.userId
        ? this.userService.updateUser(this.userId, base)
        : this.userService.createUser(base);

    // If creating a new user, after successful creation activate it so it appears in lists
    if (!this.isEditMode) {
      request.subscribe({
        next: (created: User) => {
          // If backend returned the created user id, call activate endpoint
          const id = (created as any)?.id;
          if (id) {
            this.userService.activateUser(id).subscribe({
              next: () => {
                this.loading = false;
                // remove temporary placeholder if present
                this.userService.tempUsers = (this.userService.tempUsers || []).filter(u => u.id !== id);
                try { alert('Usuario creado y activado correctamente'); } catch (e) {}
                this.router.navigate(['/users']);
              },
              error: (err) => {
                this.loading = false;
                console.error('Error al activar usuario:', err);
                // still navigate back to list so user can see created record if backend made it active later
                this.router.navigate(['/users']);
              }
            });
          } else {
            this.loading = false;
            this.router.navigate(['/users']);
          }
        },
        error: (error) => {
          this.loading = false;
          console.error('Error creating user:', error);
          this.error = error.error?.message || 'Error al guardar el usuario';
        }
      });
      return;
    }

    // Update existing user flow
    request.subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/users']);
      },
      error: (error) => {
        this.loading = false;
        console.error('Error saving user:', error);
        this.error = error.error?.message || 'Error al guardar el usuario';
      },
    });
  }

  onCancel() {
    this.router.navigate(['/users']);
  }
}
