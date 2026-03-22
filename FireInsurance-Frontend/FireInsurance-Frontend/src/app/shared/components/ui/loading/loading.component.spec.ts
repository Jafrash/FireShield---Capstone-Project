import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoadingComponent } from './loading.component';
import { LoadingService } from '../../../../core/services/loading.service';
import { signal } from '@angular/core';

describe('LoadingComponent', () => {
  let component: LoadingComponent;
  let fixture: ComponentFixture<LoadingComponent>;
  let mockLoadingService: any;
  let isLoadingSignal = signal(false);

  beforeEach(async () => {
    isLoadingSignal = signal(false);
    mockLoadingService = {
      isLoading: isLoadingSignal
    };

    await TestBed.configureTestingModule({
      imports: [LoadingComponent],
      providers: [
        { provide: LoadingService, useValue: mockLoadingService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render anything if loading is false', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.fixed')).toBeFalsy();
  });

  it('should render loading overlay when active', () => {
    isLoadingSignal.set(true);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const overlay = compiled.querySelector('.fixed');
    expect(overlay).toBeTruthy();
    expect(overlay?.textContent).toContain('Loading...');
  });
});
