export interface Document {
  documentId: number;
  documentType: DocumentType;
  fileUrl: string;
  uploadedBy: number;
  uploadDate: string;
  linkedEntityId: number;
  linkedEntityType: DocumentEntityType;
  fileName?: string;
  fileSize?: number;
}

export type DocumentType = 
  | 'PROPERTY_DOCUMENT'
  | 'POLICY_APPLICATION_DOCUMENT'
  | 'CLAIM_DOCUMENT'
  | 'INSPECTION_REPORT'
  | 'CLAIM_INSPECTION_REPORT';

export type DocumentEntityType = 'PROPERTY' | 'POLICY_SUBSCRIPTION' | 'CLAIM' | 'INSPECTION' | 'CLAIM_INSPECTION';

export interface DocumentUploadRequest {
  documentType: DocumentType;
  linkedEntityId: number;
  linkedEntityType: DocumentEntityType;
  file: File;
}
