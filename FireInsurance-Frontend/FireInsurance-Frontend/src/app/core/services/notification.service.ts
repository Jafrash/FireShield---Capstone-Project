import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AppNotification } from '../models';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  getNotifications(limit = 20): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(`${this.baseUrl}?limit=${limit}`);
  }

  pollNotifications(limit = 20, intervalMs = 30000): Observable<AppNotification[]> {
    return timer(0, intervalMs).pipe(
      switchMap(() => this.getNotifications(limit))
    );
  }

  decorateWithReadStatus(username: string, notifications: AppNotification[]): AppNotification[] {
    const readIds = this.getReadIds(username);
    return notifications.map(notification => ({
      ...notification,
      isRead: readIds.includes(notification.id)
    }));
  }

  getUnreadCount(notifications: AppNotification[]): number {
    return notifications.filter(notification => !notification.isRead).length;
  }

  markAsRead(username: string, notificationId: string): void {
    const readIds = this.getReadIds(username);
    if (readIds.includes(notificationId)) {
      return;
    }
    readIds.push(notificationId);
    this.saveReadIds(username, readIds);
  }

  markAllAsRead(username: string, notifications: AppNotification[]): void {
    const allIds = notifications.map(notification => notification.id);
    this.saveReadIds(username, Array.from(new Set(allIds)));
  }

  private getReadIds(username: string): string[] {
    const storageKey = this.getStorageKey(username);
    const raw = localStorage.getItem(storageKey);
    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as string[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  private saveReadIds(username: string, ids: string[]): void {
    localStorage.setItem(this.getStorageKey(username), JSON.stringify(ids));
  }

  private getStorageKey(username: string): string {
    return `read_notifications_${username}`;
  }
}