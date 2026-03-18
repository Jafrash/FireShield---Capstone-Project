import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationPreference } from '../models/notification-preference.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NotificationPreferenceService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/notification-preferences`;

  getPreferences(): Observable<NotificationPreference> {
    return this.http.get<NotificationPreference>(this.apiUrl);
  }

  updatePreferences(preferences: NotificationPreference): Observable<NotificationPreference> {
    return this.http.put<NotificationPreference>(this.apiUrl, preferences);
  }
}
