import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { of, throwError, delay, timer, switchMap } from 'rxjs';
import { Claim } from '../../../../core/models/claim.model';
import { PolicySubscription } from '../../../../core/models/policy.model';
import { ActivatedRoute, Router } from '@angular/router';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let mockAdminService: jasmine.SpyObj<AdminService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockAdminService = jasmine.createSpyObj('AdminService', [
      'getAllClaims',
      'getAllPolicies',
      'getAllInspections',
      'getAllSubscriptions',
      'getAllUnderwriters',
      'getAllCustomers',
      'approveSubscription',
      'rejectSubscription'
    ]);
    
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    mockAdminService.getAllClaims.and.returnValue(of([{ id: 1, status: 'APPROVED', claimAmount: 1000, createdAt: '2025-01-01T00:00:00Z', updatedAt: '2025-01-05T00:00:00Z' } as any]));
    mockAdminService.getAllPolicies.and.returnValue(of([{ id: 1 } as any]));
    mockAdminService.getAllInspections.and.returnValue(of([{ id: 1, status: 'PENDING' } as any]));
    mockAdminService.getAllSubscriptions.and.returnValue(of([{ id: 1, status: 'ACTIVE' }, { id: 2, status: 'PENDING' }] as PolicySubscription[]));
    mockAdminService.getAllUnderwriters.and.returnValue(of([{ id: 1 } as any]));
    mockAdminService.getAllCustomers.and.returnValue(of([{ id: 1 } as any]));

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [
        { provide: AdminService, useValue: mockAdminService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // calls ngOnInit -> loads data
  });

  it('should create and load initial data properly', fakeAsync(() => {
    expect(component).toBeTruthy();
    tick(); // resolve observables
    expect(component.isLoading()).toBeFalse();
    
    // Check analytics logic
    expect(component.allClaims.length).toBe(1);
    expect(component.analyticsCards()[0].value).toBe('100.0%'); // Approval rate 1/1
    expect(component.dashboardCards().length).toBe(6);
  }));

  it('should correctly format date strings', () => {
    expect(component.formatDate('')).toBe('N/A');
    expect(component.formatDate('2025-01-01T00:00:00Z')).toContain('2025');
  });

  it('should return correct status classes and labels', () => {
    expect(component.getStatusClass('APPROVED')).toContain('bg-green-100');
    expect(component.getStatusClass('UNKNOWN')).toContain('bg-fire-cream');

    expect(component.getStatusLabel('SPOT_SURVEY')).toBe('Spot Survey');

    expect(component.getStatusColor('APPROVED')).toBe('#10b981');
    expect(component.getStatusColor('MISSING')).toBe('#cbd5e1');
  });

  it('should approve subscription', () => {
    mockAdminService.approveSubscription.and.returnValue(of({} as any));
    component.approveSubscription(2);
    expect(mockAdminService.approveSubscription).toHaveBeenCalledWith(2);
  });

  it('should reject subscription', () => {
    mockAdminService.rejectSubscription.and.returnValue(of({} as any));
    component.rejectSubscription(2);
    expect(mockAdminService.rejectSubscription).toHaveBeenCalledWith(2);
  });

  it('should handle errors cleanly on dashboard data load', fakeAsync(() => {
    mockAdminService.getAllSubscriptions.and.returnValue(timer(10).pipe(switchMap(() => throwError(() => new Error('API Error')))));
    // Manually trigger data load to hit error path
    component['loadDashboardData']();
    tick(20);
    fixture.detectChanges();
    
    expect(component.errorMessage()).toBe('Failed to load subscription data');
  }));

  it('should handle approval errors', fakeAsync(() => {
    mockAdminService.approveSubscription.and.returnValue(timer(10).pipe(switchMap(() => throwError(() => new Error('Error')))));
    component.approveSubscription(100);
    tick(20);
    expect(component.errorMessage()).toBe('Failed to approve subscription');
  }));
});
