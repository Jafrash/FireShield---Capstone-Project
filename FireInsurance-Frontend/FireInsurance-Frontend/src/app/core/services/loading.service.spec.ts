import { TestBed } from '@angular/core/testing';
import { LoadingService } from './loading.service';

describe('LoadingService', () => {
  let service: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoadingService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with isLoading false', () => {
    expect(service.isLoading()).toBeFalse();
    expect(service.getActiveRequestCount()).toBe(0);
  });

  it('should set isLoading to true when show is called', () => {
    service.show();
    expect(service.isLoading()).toBeTrue();
    expect(service.getActiveRequestCount()).toBe(1);
  });

  it('should set isLoading to false when hide is called after show', () => {
    service.show();
    service.hide();
    expect(service.isLoading()).toBeFalse();
    expect(service.getActiveRequestCount()).toBe(0);
  });

  it('should handle multiple shows and hides', () => {
    service.show();
    service.show();
    expect(service.isLoading()).toBeTrue();
    expect(service.getActiveRequestCount()).toBe(2);

    service.hide();
    expect(service.isLoading()).toBeTrue();
    expect(service.getActiveRequestCount()).toBe(1);

    service.hide();
    expect(service.isLoading()).toBeFalse();
    expect(service.getActiveRequestCount()).toBe(0);
  });

  it('should force hide correctly', () => {
    service.show();
    service.show();
    service.forceHide();
    expect(service.isLoading()).toBeFalse();
    expect(service.getActiveRequestCount()).toBe(0);
  });
});
