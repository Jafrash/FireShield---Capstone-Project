import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentUploadComponent } from '../document-upload/document-upload.component';
import { Document, DocumentType, DocumentEntityType } from '../../../../core/models/document.model';

@Component({
  selector: 'app-inspection-document-upload',
  standalone: true,
  imports: [CommonModule, DocumentUploadComponent],
  template: `
    <app-document-upload
      [title]="title"
      [documentType]="documentType"
      [linkedEntityType]="linkedEntityType"
      [linkedEntityId]="inspectionId"
      [requiredDocuments]="requiredDocs"
      (uploadComplete)="onComplete($event)"
    ></app-document-upload>
  `
})
export class InspectionDocumentUploadComponent {
  @Input() inspectionId!: number;
  @Input() isClaimInspection: boolean = false;
  @Output() uploadComplete = new EventEmitter<Document>();

  get title(): string {
    return this.isClaimInspection ? 'Upload Claim Inspection Documents' : 'Upload Property Inspection Documents';
  }

  get documentType(): DocumentType {
    return this.isClaimInspection ? 'CLAIM_INSPECTION_REPORT' : 'INSPECTION_REPORT';
  }

  get linkedEntityType(): DocumentEntityType {
    return this.isClaimInspection ? 'CLAIM_INSPECTION' : 'INSPECTION';
  }

  get requiredDocs(): string[] {
    if (this.isClaimInspection) {
      return [
        'Damage verification report',
        'Loss assessment report',
        'Inspection photos'
      ];
    }
    return [
      'Inspection report PDF',
      'Site photos',
      'Risk assessment document'
    ];
  }

  onComplete(doc: Document) {
    this.uploadComplete.emit(doc);
  }
}
