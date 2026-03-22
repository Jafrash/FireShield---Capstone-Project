import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { loadingInterceptor } from './loading.interceptor';
import { LoadingService } from '../services/loading.service';

describe('loadingInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let mockLoadingService: jasmine.SpyObj<LoadingService>;

  beforeEach(() => {
    mockLoadingService = jasmine.createSpyObj('LoadingService', ['show', 'hide']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loadingInterceptor])),
        provideHttpClientTesting(),
        { provide: LoadingService, useValue: mockLoadingService }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should show loading when a request starts and hide when it completes', () => {
    httpClient.get('/api/data').subscribe();

    const req = httpTestingController.expectOne('/api/data');
    expect(mockLoadingService.show).toHaveBeenCalled();

    req.flush({});
    expect(mockLoadingService.hide).toHaveBeenCalled();
  });

  it('should skip loading for specific endpoints', () => {
    httpClient.get('/api/notifications').subscribe();
    httpTestingController.expectOne('/api/notifications').flush({});
    expect(mockLoadingService.show).not.toHaveBeenCalled();

    httpClient.get('/api/auth/refresh').subscribe();
    httpTestingController.expectOne('/api/auth/refresh').flush({});
    expect(mockLoadingService.show).not.toHaveBeenCalled();
  });

  it('should skip loading if X-Skip-Loading header is present', () => {
    httpClient.get('/api/data', { headers: { 'X-Skip-Loading': 'true' } }).subscribe();

    const req = httpTestingController.expectOne('/api/data');
    expect(req.request.headers.has('X-Skip-Loading')).toBeFalse(); // Header should be removed
    expect(mockLoadingService.show).not.toHaveBeenCalled();

    req.flush({});
  });

  it('should hide loading even if the request fails', () => {
    httpClient.get('/api/error').subscribe({
      error: () => {}
    });

    const req = httpTestingController.expectOne('/api/error');
    expect(mockLoadingService.show).toHaveBeenCalled();

    req.error(new ProgressEvent('error'));
    expect(mockLoadingService.hide).toHaveBeenCalled();
  });
});
