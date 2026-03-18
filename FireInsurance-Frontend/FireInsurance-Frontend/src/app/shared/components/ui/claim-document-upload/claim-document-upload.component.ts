import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentUploadComponent } from '../document-upload/document-upload.component';
import { Document, DocumentType, DocumentEntityType } from '../../../../core/models/document.model';

@Component({
  selector: 'app-claim-document-upload',
  standalone: true,
  imports: [CommonModule, DocumentUploadComponent],
  template: `
    <app-document-upload
      [title]="'Upload Claim Documents'"
      [documentType]="'CLAIM_DOCUMENT'"
      [linkedEntityType]="'CLAIM'"
      [linkedEntityId]="claimId"
      [requiredDocuments]="requiredDocs"
      (uploadComplete)="onComplete($event)"
    ></app-document-upload>
  `
})
export class ClaimDocumentUploadComponent {
  @Input() claimId!: number;
  @Output() uploadComplete = new EventEmitter<Document>();

  requiredDocs = [
    'Fire brigade report',
    'Damage photos',
    'Loss estimate document',
    'Repair quotation'
  ];

  onComplete(doc: Document) {
    this.uploadComplete.emit(doc);
  }
}
