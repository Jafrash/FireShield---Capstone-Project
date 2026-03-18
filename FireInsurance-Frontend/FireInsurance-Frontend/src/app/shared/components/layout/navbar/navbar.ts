import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
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
  private notificationSubscription?: Subscription;

  username: string | null = null;
  email: string | null = null;
  role: string | null = null;
  showUserMenu = false;
  showNotifications = false;
  notifications: AppNotification[] = [];
  unreadCount = 0;

  ngOnInit(): void {
    this.loadUserInfo();
    this.startNotificationPolling();
  }

  ngOnDestroy(): void {
    this.notificationSubscription?.unsubscribe();
  }

  /**
   * Load user information from TokenService
   */
  private loadUserInfo(): void {
    this.username = this.tokenService.getUsername();
    this.email = this.tokenService.getEmail();
    this.role = this.tokenService.getRole();
  }

  /**
   * Toggle user menu dropdown
   */
  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
    if (this.showUserMenu) {
      this.showNotifications = false;
    }
  }

  /**
   * Close user menu
   */
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
    this.notifications = this.notificationService.decorateWithReadStatus(this.username, this.notifications);
    this.unreadCount = this.notificationService.getUnreadCount(this.notifications);

    if (notification.actionUrl) {
      this.router.navigateByUrl(notification.actionUrl);
    }
  }

  markAllNotificationsAsRead(): void {
    if (!this.username) {
      return;
    }

    this.notificationService.markAllAsRead(this.username, this.notifications);
    this.notifications = this.notificationService.decorateWithReadStatus(this.username, this.notifications);
    this.unreadCount = 0;
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

  private startNotificationPolling(): void {
    if (!this.username) {
      return;
    }

    this.notificationSubscription = this.notificationService.pollNotifications(20, 30000).subscribe({
      next: (items) => {
        this.notifications = this.notificationService.decorateWithReadStatus(this.username!, items);
        this.unreadCount = this.notificationService.getUnreadCount(this.notifications);
      },
      error: () => {
        this.notifications = [];
        this.unreadCount = 0;
      }
    });
  }

  /**
   * Logout user
   */
  logout(): void {
    this.authService.logout();
  }

  /**
   * Get user initials for avatar
   */
  getUserInitials(): string {
    if (!this.username) return 'U';
    const parts = this.username.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return this.username.substring(0, 2).toUpperCase();
  }
}
