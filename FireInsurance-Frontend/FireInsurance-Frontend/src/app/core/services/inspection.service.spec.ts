import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { InspectionService } from './inspection.service';

describe('InspectionService', () => {
  let service: InspectionService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        InspectionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(InspectionService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch surveyors', () => {
    const mockSurveyors = [{ id: 1, name: 'John Doe' }];
    service.getSurveyors().subscribe(res => {
      expect(res).toEqual(mockSurveyors as any);
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/surveyors');
    expect(req.request.method).toBe('GET');
    req.flush(mockSurveyors);
  });

  it('should assign inspection and map response correctly', () => {
    const payload = { subscriptionId: 10, propertyId: 5, surveyorId: 99 };
    const mockApiResponse = { status: 'PENDING' };

    service.assignInspection(payload).subscribe(res => {
      expect(res.subscriptionId).toBe(10);
      expect(res.propertyId).toBe(5);
      expect(res.surveyorId).toBe(99);
      expect(res.status).toBe('PENDING');
      expect(res.assignedDate).toBeTruthy();
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/subscriptions/10/assign-surveyor/99');
    expect(req.request.method).toBe('PUT');
    req.flush(mockApiResponse);
  });

  it('should fetch inspection status', () => {
    const mockStatus = { inspectionId: 1, subscriptionId: 10, status: 'DONE' };
    service.getInspectionStatus(10).subscribe(res => {
      expect(res).toEqual(mockStatus as any);
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/inspections/subscription/10');
    expect(req.request.method).toBe('GET');
    req.flush(mockStatus);
  });
});
