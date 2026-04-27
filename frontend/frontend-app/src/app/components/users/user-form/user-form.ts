import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../../services/user';
import { User, UserRole, UserStatus } from '../../../models/auth.model';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-user-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule
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
  roles = Object.values(UserRole);

  constructor(
    private formBuilder: FormBuilder,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.initializeForm();
    
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.userId = params['id'];
        this.loadUser(params['id']);
      }
    });
  }

  initializeForm() {
    this.form = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', this.isEditMode ? [] : [Validators.required, Validators.minLength(6)]],
      fullName: ['', [Validators.required]],
      role: [UserRole.FUNCIONARIO, [Validators.required]]
    });
  }

  loadUser(id: string) {
    this.loading = true;
    this.userService.getUserById(id).subscribe({
      next: (user) => {
        this.form.patchValue({
          username: user.username,
          email: user.email,
          fullName: user.fullName,
          role: user.role
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user:', error);
        this.error = 'Error al cargar el usuario';
        this.loading = false;
      }
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
    const data: User = {
      username: this.f['username'].value,
      email: this.f['email'].value,
      fullName: this.f['fullName'].value,
      role: this.f['role'].value,
      status: UserStatus.ACTIVO
    };

    if (!this.isEditMode) {
      data.password = this.f['password'].value;
    }

    const request = this.isEditMode && this.userId
      ? this.userService.updateUser(this.userId, data)
      : this.userService.createUser(data);

    request.subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/users']);
      },
      error: (error) => {
        this.loading = false;
        console.error('Error saving user:', error);
        this.error = error.error?.message || 'Error al guardar el usuario';
      }
    });
  }

  onCancel() {
    this.router.navigate(['/users']);
  }
}
