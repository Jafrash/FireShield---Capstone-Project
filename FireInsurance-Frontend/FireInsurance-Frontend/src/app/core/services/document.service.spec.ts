import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DocumentService } from './document.service';
import { HttpEventType } from '@angular/common/http';

describe('DocumentService', () => {
  let service: DocumentService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DocumentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DocumentService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch documents for property entity type', () => {
    const mockDocs = [{ id: 1, name: 'doc1.pdf' }];
    service.getDocumentsForEntity(101, 'PROPERTY').subscribe((docs) => {
      expect(docs).toEqual(mockDocs as any);
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/documents/property/101');
    expect(req.request.method).toEqual('GET');
    req.flush(mockDocs);
  });

  it('should fetch documents for claim entity type', () => {
    const mockDocs = [{ id: 2, name: 'claim_form.pdf' }];
    service.getDocumentsForEntity(202, 'CLAIM').subscribe((docs) => {
      expect(docs).toEqual(mockDocs as any);
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/documents/claim/202');
    expect(req.request.method).toEqual('GET');
    req.flush(mockDocs);
  });

  it('should handle document upload logic for PROPERTY', () => {
    const mockFile = new File([''], 'test.pdf');
    let triggeredStates: string[] = [];
    service.uploadDocument(mockFile, 'PDF' as any, 101, 'PROPERTY').subscribe(progress => {
      triggeredStates.push(progress.state);
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/documents/upload/customer');
    expect(req.request.method).toEqual('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    // Flush responses simulating progress
    req.event({ type: HttpEventType.Sent });
    req.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
    req.event({ type: HttpEventType.Response, body: { id: 1 }, clone: null as any, headers: null as any, status: 200, statusText: 'OK', url: null as any, ok: true });
    
    expect(triggeredStates).toContain('PENDING');
    expect(triggeredStates).toContain('IN_PROGRESS');
    expect(triggeredStates).toContain('DONE');
  });

  it('should handle document upload logic for INSPECTION', () => {
    const mockFile = new File([''], 'survey.pdf');
    service.uploadDocument(mockFile, 'PDF' as any, 101, 'INSPECTION').subscribe();

    const req = httpTestingController.expectOne('http://localhost:8080/api/documents/upload/surveyor');
    expect(req.request.method).toEqual('POST');
    req.flush({});
  });

  it('should delete document', () => {
    service.deleteDocument(1).subscribe();
    const req = httpTestingController.expectOne('http://localhost:8080/api/documents/1');
    expect(req.request.method).toEqual('DELETE');
    req.flush({});
  });

  it('should download document', () => {
    service.downloadDocument(1).subscribe(blob => {
      expect(blob instanceof Blob).toBeTrue();
    });
    const req = httpTestingController.expectOne('http://localhost:8080/api/documents/download/1');
    expect(req.request.method).toEqual('GET');
    expect(req.request.responseType).toEqual('blob');
    req.flush(new Blob(['test data']));
  });
});
