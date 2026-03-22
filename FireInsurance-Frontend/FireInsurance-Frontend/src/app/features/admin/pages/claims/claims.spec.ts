import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ClaimsComponent } from './claims';
import { AdminService, Surveyor, Underwriter } from '../../../../features/admin/services/admin.service';
import { DocumentService } from '../../../../core/services/document.service';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError, delay } from 'rxjs';
import { Claim } from '../../../../core/models/claim.model';

describe('ClaimsComponent', () => {
  let component: ClaimsComponent;
  let fixture: ComponentFixture<ClaimsComponent>;
  let mockAdminService: jasmine.SpyObj<AdminService>;
  let mockDocumentService: jasmine.SpyObj<DocumentService>;

  beforeEach(async () => {
    mockAdminService = jasmine.createSpyObj('AdminService', [
      'getAllClaims', 
      'getAllSurveyors', 
      'getAllUnderwriters', 
      'updateClaimStatus',
      'approveClaim',
      'rejectClaim',
      'assignClaimToSurveyor',
      'assignUnderwriterToClaim'
    ]);
    mockDocumentService = jasmine.createSpyObj('DocumentService', ['getDocumentsForEntity', 'downloadDocument']);

    mockAdminService.getAllClaims.and.returnValue(of([{ claimId: 1, description: 'Fire damage', status: 'SUBMITTED' } as Claim]));
    mockAdminService.getAllSurveyors.and.returnValue(of([{ id: 101, name: 'S1' } as any as Surveyor]));
    mockAdminService.getAllUnderwriters.and.returnValue(of([{ id: 201, firstName: 'U1' } as any as Underwriter]));

    await TestBed.configureTestingModule({
      imports: [ClaimsComponent, FormsModule],
      providers: [
        { provide: AdminService, useValue: mockAdminService },
        { provide: DocumentService, useValue: mockDocumentService },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClaimsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load data', () => {
    expect(component).toBeTruthy();
    expect(component.claims().length).toBe(1);
    expect(component.surveyors().length).toBe(1);
    expect(component.underwriters().length).toBe(1);
  });

  it('should search claims correctly', () => {
    component.claims.set([
      { claimId: 1, description: 'Fire', status: 'SUBMITTED' } as any,
      { claimId: 2, description: 'Theft', status: 'APPROVED' } as any
    ]);
    
    component.searchClaims('Theft');
    expect(component.filteredClaims().length).toBe(1);
    expect(component.filteredClaims()[0].claimId).toBe(2);

    component.searchClaims('');
    expect(component.filteredClaims().length).toBe(2);
  });

  it('should update claim status', () => {
    mockAdminService.updateClaimStatus.and.returnValue(of({} as any));
    component.updateClaimStatus(1, 'INSPECTING');
    expect(mockAdminService.updateClaimStatus).toHaveBeenCalledWith(1, 'INSPECTING');
    expect(component.claims()[0].status).toBe('INSPECTING' as any);
  });

  it('should approve claim after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');
    const updatedClaim = { claimId: 1, status: 'APPROVED' } as any;
    mockAdminService.approveClaim.and.returnValue(of(updatedClaim));

    component.approveClaim(1);
    expect(mockAdminService.approveClaim).toHaveBeenCalledWith(1);
    expect(component.claims()[0].status).toBe('APPROVED' as any);
    expect(window.alert).toHaveBeenCalledWith('Claim approved successfully!');
  });

  it('should assign inspection to surveyor', () => {
    spyOn(window, 'alert');
    component.selectedSurveyors[1] = 101;
    mockAdminService.assignClaimToSurveyor.and.returnValue(of({} as any));

    component.assignInspection({ claimId: 1 } as any);
    expect(mockAdminService.assignClaimToSurveyor).toHaveBeenCalledWith(1, 101);
    expect(component.claims()[0].status).toBe('INSPECTING' as any);
  });

  it('should alert if no surveyor is selected during assignment', () => {
    spyOn(window, 'alert');
    component.assignInspection({ claimId: 1 } as any);
    expect(window.alert).toHaveBeenCalledWith('Please select a surveyor first');
  });

  it('should view documents and filter them by CLAIM stage', () => {
    const mockDocs = [
        { documentId: 1, documentType: 'CLAIM_FORM', documentStage: 'CLAIM_STAGE' },
        { documentId: 2, documentType: 'POLICY_SCHEDULE', documentStage: 'POLICY_STAGE' }
    ];
    mockDocumentService.getDocumentsForEntity.and.returnValue(of(mockDocs as any));
    
    component.viewDocuments({ claimId: 1 } as any);
    
    expect(component.selectedClaimForDocs()).toBeTruthy();
    expect(component.claimDocuments().length).toBe(1);
    expect(component.claimDocuments()[0].documentId).toBe(1);
  });

  it('should download document', () => {
    const mockBlob = new Blob(['data'], { type: 'application/pdf' });
    mockDocumentService.downloadDocument.and.returnValue(of(mockBlob));
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob-url');
    spyOn(window.URL, 'revokeObjectURL');

    component.downloadDocument({ documentId: 5, fileName: 'test.pdf' } as any);
    
    expect(mockDocumentService.downloadDocument).toHaveBeenCalledWith(5);
    expect(window.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
  });
});
