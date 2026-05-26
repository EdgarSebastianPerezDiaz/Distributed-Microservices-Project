import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { UserService } from '../../../services/user';
import { AuthService } from '../../../services/auth';
import { User, UserRole, UserStatus } from '../../../models/auth.model';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-user-list',
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatMenuModule,
    MatDialogModule,
    MatCardModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserListComponent implements OnInit {
  displayedColumns: string[] = ['fullName', 'username', 'email', 'role', 'status', 'actions'];
  users: User[] = [];
  loading = false;
  actionLoading: Record<string, boolean> = {};
  
  // Pagination
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  
  // Search and filters
  searchTerm = '';
  roleFilter: UserRole | 'ALL' = 'ALL';
  statusFilter: UserStatus | 'ALL' = 'ALL';
  private searchTerms$ = new Subject<string>();
  private destroy$ = new Subject<void>();
  
  // Current user
  currentUser: User | null = null;
  
  UserRole = UserRole;
  UserStatus = UserStatus;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
    this.initSearchSubscription();
    this.loadData();

    // Refresh when service signals mutations
    this.userService.refresh$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.loadUsers();
    });
  }

  // Public alias used by templates to load data when filters change
  loadData() {
    this.currentPage = 0;
    this.loadUsers();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initSearchSubscription() {
    this.searchTerms$
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadUsers();
      });
  }

  loadUsers() {
    this.loading = true;
    this.userService
      .getUsers(
        this.currentPage,
        this.pageSize,
        this.searchTerm,
        this.roleFilter,
        this.statusFilter
      )
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          this.users = response?.content || response || [];
          this.totalElements = response?.totalElements ?? this.users.length;

          // Merge any temporary users created client-side that the server may still
          // report as inactive or not include on the current page so they become
          // immediately visible to the admin after creation.
          const temps = this.userService.tempUsers || [];
          if (temps.length) {
            // create visible copies (mark active true for UI only) and prepend
            const visibleTemps = temps.map(t => ({ ...t, __pending: true } as any));
            // combine and remove duplicates by id, keeping first occurrence
            this.users = [...visibleTemps, ...this.users].filter((u, i, self) => self.findIndex(s => s.id === u.id) === i);
            this.totalElements = Math.max(this.totalElements || 0, this.users.length);
          }
        },
        error: (error) => {
          console.error('Error loading users:', error);
        }
      });
  }

  onSearchTermChange(valueOrEvent: string | Event) {
    const value = typeof valueOrEvent === 'string' ? valueOrEvent : (valueOrEvent.target as HTMLInputElement).value;
    this.searchTerm = value;
    this.searchTerms$.next(value);
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(event: PageEvent) {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  onSearch() {
    this.currentPage = 0;
    this.loadUsers();
  }

  clearSearch() {
    this.searchTerm = '';
    this.roleFilter = 'ALL';
    this.statusFilter = 'ALL';
    this.currentPage = 0;
    this.loadUsers();
  }

  viewUser(id: string | undefined) {
    if (id) {
      this.router.navigate(['/users', id]);
    }
  }

  editUser(id: string | undefined) {
    if (id) {
      this.router.navigate(['/users', id, 'edit']);
    }
  }

  createUser() {
    this.router.navigate(['/users/new']);
  }

  toggleUserStatus(user: User) {
    if (!user.id) return;
    
    // Prevent admin from deactivating themselves
    if (user.id === this.currentUser?.id && user.active === true) {
      alert('No puedes desactivar tu propia cuenta');
      return;
    }

    if (user.active === true) {
      this.deactivateUser(user.id);
    } else {
      this.activateUser(user.id);
    }
  }

  activateUser(id: string) {
    if (!id) return;
    this.actionLoading[id] = true;
    this.userService.activateUser(id)
      .pipe(finalize(() => (this.actionLoading[id] = false)))
      .subscribe({
        next: () => this.loadUsers(),
        error: (error) => console.error('Error activating user:', error)
      });
  }

  deactivateUser(id: string) {
    if (!id) return;
    this.actionLoading[id] = true;
    this.userService.deactivateUser(id)
      .pipe(finalize(() => (this.actionLoading[id] = false)))
      .subscribe({
        next: () => this.loadUsers(),
        error: (error) => console.error('Error deactivating user:', error)
      });
  }

  deleteUser(id: string | undefined) {
    if (!id) return;
    
    if (confirm('¿Estás seguro de que deseas eliminar este usuario?')) {
      this.actionLoading[id] = true;
      this.userService.deleteUser(id).pipe(finalize(() => (this.actionLoading[id] = false))).subscribe({
        next: () => this.loadUsers(),
        error: (error) => console.error('Error deleting user:', error),
      });
    }
  }

  isCurrentUser(user: User): boolean {
    return user.id === this.currentUser?.id;
  }

  isAdmin(): boolean {
    return this.authService.hasRole(UserRole.ADMINISTRADOR);
  }

  getInitials(user: User): string {
    const source = user.fullName || user.username || user.email || '';
    const parts = source.trim().split(/\s+/).filter(Boolean);
    const a = parts[0]?.charAt(0);
    const b = parts.length > 1 ? parts[parts.length - 1]?.charAt(0) : parts[0]?.charAt(1);
    return ((a || '') + (b || '')).toUpperCase() || 'U';
  }

  roleLabel(role: UserRole): string {
    const map: Record<UserRole, string> = {
      [UserRole.ADMINISTRADOR]: 'Administrador',
      [UserRole.FUNCIONARIO]: 'Funcionario',
      [UserRole.AUDITOR]: 'Auditor',
    };
    return map[role] || role;
  }
}
