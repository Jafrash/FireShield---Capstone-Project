import 'zone.js';
import 'zone.js/testing';
import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationService } from './notification.service';
import { environment } from '../../../environments/environment';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(NotificationService);
    httpTestingController = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch notifications', () => {
    const mockNotes = [{ id: '1', title: 'hello' }];
    service.getNotifications(10).subscribe(res => {
      expect(res).toEqual(mockNotes as any);
    });

    const req = httpTestingController.expectOne(`${environment.apiUrl}/notifications?limit=10`);
    expect(req.request.method).toBe('GET');
    req.flush(mockNotes);
  });

  it('should poll notifications', fakeAsync(() => {
    const mockNotes = [{ id: '1', title: 'hello' }];
    let result: any;
    
    const subscription = service.pollNotifications(5, 1000).subscribe(res => {
      result = res;
    });
    tick(0);

    const req1 = httpTestingController.expectOne(`${environment.apiUrl}/notifications?limit=5`);
    req1.flush(mockNotes);
    expect(result).toEqual(mockNotes);

    tick(1000); // Trigger next poll
    const req2 = httpTestingController.expectOne(`${environment.apiUrl}/notifications?limit=5`);
    req2.flush([]);
    expect(result).toEqual([]);
    
    subscription.unsubscribe();
    discardPeriodicTasks();
    httpTestingController.verify();
  }));

  it('should calculate unread count correctly', () => {
    const notifications: any[] = [
      { id: '1', isRead: true },
      { id: '2', isRead: false },
      { id: '3', isRead: false }
    ];
    expect(service.getUnreadCount(notifications)).toBe(2);
  });

  it('should handle read status with localStorage', () => {
    const notifications: any[] = [{ id: '1' }, { id: '2' }];
    service.markAsRead('user1', '1');

    const decorated = service.decorateWithReadStatus('user1', notifications);
    expect(decorated[0].isRead).toBeTrue();
    expect(decorated[1].isRead).toBeFalse();
  });

  it('should mark all as read', () => {
    const notifications: any[] = [{ id: '1' }, { id: '2' }];
    service.markAllAsRead('user2', notifications);

    const decorated = service.decorateWithReadStatus('user2', notifications);
    expect(decorated[0].isRead).toBeTrue();
    expect(decorated[1].isRead).toBeTrue();
  });
});
