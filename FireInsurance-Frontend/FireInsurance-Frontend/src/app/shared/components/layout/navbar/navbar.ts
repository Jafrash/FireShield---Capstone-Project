import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';
import { TokenService, AuthService, NotificationService } from '../../../../core/services';
import { AppNotification } from '../../../../core/models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.html'
  // Styles provided via Tailwind utility classes
})
export class NavbarComponent implements OnInit {
  private tokenService = inject(TokenService);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  username: string | null = null;
  email: string | null = null;
  role: string | null = null;
  showUserMenu = false;
  showNotifications = false;

  notifications$!: Observable<AppNotification[]>;
  unreadCount$!: Observable<number>;

  ngOnInit(): void {
    this.loadUserInfo();
    if (this.username) {
      this.notifications$ = this.notificationService
        .pollNotifications(20, 30000)
        .pipe(map(items => this.notificationService.decorateWithReadStatus(this.username!, items)));
      this.unreadCount$ = this.notifications$.pipe(
        map(notifications => this.notificationService.getUnreadCount(notifications))
      );
    }
  }

  private loadUserInfo(): void {
    this.username = this.tokenService.getUsername();
    this.email = this.tokenService.getEmail();
    this.role = this.tokenService.getRole();
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
    if (this.showUserMenu) {
      this.showNotifications = false;
    }
  }

  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.showUserMenu = false;
    }
  }

  markNotificationAsRead(notification: AppNotification): void {
    if (!this.username) {
      return;
    }
    this.notificationService.markAsRead(this.username, notification.id);
    if (notification.actionUrl) {
      this.router.navigateByUrl(notification.actionUrl);
    }
  }

  markAllNotificationsAsRead(notifications: AppNotification[]): void {
    if (!this.username) {
      return;
    }
    this.notificationService.markAllAsRead(this.username, notifications);
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'CLAIM':
        return 'gavel';
      case 'PROPERTY_INSPECTION':
      case 'CLAIM_INSPECTION':
        return 'fact_check';
      default:
        return 'campaign';
    }
  }

  formatNotificationTime(value: string): string {
    if (!value) {
      return 'Just now';
    }
    const timestamp = new Date(value).getTime();
    if (Number.isNaN(timestamp)) {
      return 'Just now';
    }
    const diffMs = Date.now() - timestamp;
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
  }

  trackById(index: number, item: AppNotification) {
    return item.id;
  }

  logout(): void {
    this.authService.logout();
  }

  getUserInitials(): string {
    if (!this.username) return 'U';
    const parts = this.username.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return this.username.substring(0, 2).toUpperCase();
  }
}
