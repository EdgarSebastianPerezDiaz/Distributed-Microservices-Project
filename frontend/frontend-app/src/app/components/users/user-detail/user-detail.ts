import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { UserService } from '../../../services/user';
import { User, UserStatus, UserRole } from '../../../models/auth.model';

@Component({
  selector: 'app-user-detail',
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './user-detail.html',
  styleUrl: './user-detail.scss',
})
export class UserDetailComponent implements OnInit {
  user: User | null = null;
  loading = false;
  userId: string | null = null;
  error: string | null = null;
  
  UserStatus = UserStatus;
  UserRole = UserRole;

  constructor(
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.userId = params['id'];
        this.loadUser(params['id']);
      }
    });
  }

  loadUser(id: string) {
    this.loading = true;
    this.userService.getUserById(id).subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user:', error);
        this.error = 'Error al cargar el usuario';
        this.loading = false;
      }
    });
  }

  editUser() {
    if (this.userId) {
      this.router.navigate(['/users', this.userId, 'edit']);
    }
  }

  goBack() {
    this.router.navigate(['/users']);
  }
}
