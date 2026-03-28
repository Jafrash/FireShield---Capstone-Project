import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminService, Surveyor } from './admin.service';
import { environment } from '../../../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpTestingController: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AdminService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all claims', () => {
    service.getAllClaims().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/claims`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get all policies', () => {
    service.getAllPolicies().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/policies`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get all customers', () => {
    service.getAllCustomers().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/customers`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get all subscriptions', () => {
    service.getAllSubscriptions().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/subscriptions`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get all underwriters', () => {
    service.getAllUnderwriters().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/admin/underwriters`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get all surveyors', () => {
    service.getAllSurveyors().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/surveyors`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get all inspections', () => {
    service.getAllInspections().subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/inspections`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
