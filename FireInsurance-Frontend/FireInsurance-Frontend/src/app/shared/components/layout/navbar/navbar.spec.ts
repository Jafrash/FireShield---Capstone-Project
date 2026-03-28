import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavbarComponent } from './navbar';
import { TokenService, AuthService, NotificationService } from '../../../../core/services';
import { Router } from '@angular/router';
import { of } from 'rxjs';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let mockTokenService: jasmine.SpyObj<TokenService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockNotificationService: jasmine.SpyObj<NotificationService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockTokenService = jasmine.createSpyObj('TokenService', ['getUsername', 'getEmail', 'getRole']);
    mockAuthService = jasmine.createSpyObj('AuthService', ['logout']);
    mockNotificationService = jasmine.createSpyObj('NotificationService', [
      'pollNotifications',
      'decorateWithReadStatus',
      'getUnreadCount',
      'markAsRead',
      'markAllAsRead'
    ]);
    mockRouter = jasmine.createSpyObj('Router', ['navigateByUrl']);

    mockTokenService.getUsername.and.returnValue('John Doe');
    mockTokenService.getEmail.and.returnValue('john@example.com');
    mockTokenService.getRole.and.returnValue('USER' as any);

    mockNotificationService.pollNotifications.and.returnValue(of([]));
    mockNotificationService.decorateWithReadStatus.and.returnValue([]);
    mockNotificationService.getUnreadCount.and.returnValue(0);

    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        { provide: TokenService, useValue: mockTokenService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.username).toBe('John Doe');
  });

  it('should toggle user menu', () => {
    expect(component.showUserMenu).toBeFalse();
    component.toggleUserMenu();
    expect(component.showUserMenu).toBeTrue();
    // Verify notifications close when user menu opens
    component.showUserMenu = false;
    component.showNotifications = true;
    component.toggleUserMenu();
    expect(component.showNotifications).toBeFalse();
  });

  it('should logout', () => {
    component.logout();
    expect(mockAuthService.logout).toHaveBeenCalled();
  });

  it('should get initials correctly', () => {
    mockTokenService.getUsername.and.returnValue('Jane Smith');
    component.ngOnInit(); // reload info
    expect(component.getUserInitials()).toBe('JS');

    mockTokenService.getUsername.and.returnValue('Admin');
    component.ngOnInit();
    expect(component.getUserInitials()).toBe('AD');
  });
});
