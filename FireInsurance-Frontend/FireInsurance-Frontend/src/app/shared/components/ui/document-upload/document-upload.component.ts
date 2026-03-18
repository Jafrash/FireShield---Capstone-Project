import { Component, EventEmitter, Input, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService, UploadProgress } from '../../../../core/services/document.service';
import { DocumentType, DocumentEntityType, Document } from '../../../../core/models/document.model';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mt-6">
      <h3 class="text-lg font-bold text-gray-900 mb-4">{{ title }}</h3>
      
      <div class="mb-4 text-sm text-gray-600">
        <p>Required documents:</p>
        <ul class="list-disc pl-5 mt-2 space-y-1">
          @for (doc of requiredDocuments; track doc) {
            <li>{{ doc }}</li>
          }
        </ul>
      </div>

      <div class="border-2 border-dashed border-gray-300 rounded-lg p-6 flex flex-col items-center justify-center bg-gray-50 hover:bg-gray-100 transition-colors cursor-pointer relative">
        <input 
          type="file" 
          class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" 
          (change)="onFileSelected($event)"
          [disabled]="isUploading()"
          multiple>
        
        <span class="material-icons text-4xl text-gray-400 mb-2">cloud_upload</span>
        <p class="text-sm font-medium text-gray-700">Click or drag file to this area to upload</p>
        <p class="text-xs text-gray-500 mt-1">Maximum file size: 10MB</p>
      </div>

      @if (selectedFile()) {
        <div class="mt-4 flex items-center justify-between bg-blue-50 p-3 rounded-lg border border-blue-100">
          <div class="flex items-center gap-3 overflow-hidden">
            <span class="material-icons text-blue-500">description</span>
            <span class="text-sm font-medium text-blue-900 truncate">{{ selectedFile()?.name }}</span>
          </div>
          <button (click)="upload()" 
                  [disabled]="isUploading()"
                  class="px-4 py-1.5 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors">
            {{ isUploading() ? 'Uploading...' : 'Upload' }}
          </button>
        </div>
      }

      @if (uploadProgress() && uploadProgress()!.state === 'IN_PROGRESS') {
        <div class="mt-4">
          <div class="flex justify-between text-xs text-blue-600 mb-1">
            <span>Uploading...</span>
            <span>{{ uploadProgress()!.progress }}%</span>
          </div>
          <div class="w-full bg-blue-100 rounded-full h-2">
            <div class="bg-blue-600 h-2 rounded-full transition-all duration-300" [style.width.%]="uploadProgress()!.progress"></div>
          </div>
        </div>
      }

      @if (errorMsg()) {
        <div class="mt-4 text-sm text-red-600 flex items-center gap-1">
          <span class="material-icons text-sm">error</span>
          {{ errorMsg() }}
        </div>
      }
      
      @if (successMsg()) {
        <div class="mt-4 text-sm text-green-600 flex items-center gap-1">
          <span class="material-icons text-sm">check_circle</span>
          {{ successMsg() }}
        </div>
      }

      <div class="mt-6">
        <h4 class="text-sm font-bold text-gray-700 mb-3 border-b pb-2">Uploaded Documents</h4>
        @if (uploadedDocuments().length === 0) {
          <p class="text-sm text-gray-500 italic">No documents uploaded yet.</p>
        } @else {
          <ul class="space-y-2">
            @for (doc of uploadedDocuments(); track doc.documentId) {
              <li class="flex items-center justify-between text-sm p-2 bg-gray-50 rounded border border-gray-100">
                <div class="flex items-center gap-2 overflow-hidden">
                  <span class="material-icons text-gray-400 text-sm">task</span>
                  <span class="truncate" [title]="doc.fileName">{{ doc.fileName || doc.documentId }}</span>
                </div>
              </li>
            }
          </ul>
        }
      </div>
    </div>
  `
})
export class DocumentUploadComponent {
  private documentService = inject(DocumentService);

  @Input() title: string = 'Upload Documents';
  @Input() documentType!: DocumentType;
  @Input() linkedEntityType!: DocumentEntityType;
  @Input() linkedEntityId!: number;
  @Input() requiredDocuments: string[] = [];

  @Output() uploadComplete = new EventEmitter<Document>();

  selectedFile = signal<File | null>(null);
  isUploading = signal(false);
  uploadProgress = signal<UploadProgress | null>(null);
  errorMsg = signal('');
  successMsg = signal('');
  uploadedDocuments = signal<Document[]>([]);

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        this.errorMsg.set('File is too large. Maximum size is 10MB.');
        return;
      }
      this.selectedFile.set(file);
      this.errorMsg.set('');
      this.successMsg.set('');
      this.uploadProgress.set(null);
    }
  }

  upload() {
    const file = this.selectedFile();
    if (!file || !this.linkedEntityId) {
      this.errorMsg.set('Please select a file and ensure the entity is created.');
      return;
    }

    this.isUploading.set(true);
    this.errorMsg.set('');
    this.uploadProgress.set({ progress: 0, state: 'PENDING' });

    this.documentService.uploadDocument(file, this.documentType, this.linkedEntityId, this.linkedEntityType).subscribe({
      next: (progress: UploadProgress) => {
        this.uploadProgress.set(progress);
        
        if (progress.state === 'DONE' && progress.response) {
          this.successMsg.set('Document uploaded successfully.');
          this.selectedFile.set(null);
          this.uploadedDocuments.update(docs => [...docs, progress.response]);
          this.uploadComplete.emit(progress.response);
          this.isUploading.set(false);
          
          // Clear file input
          const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
          if (fileInput) fileInput.value = '';
        }
      },
      error: (err) => {
        console.error('Upload Error:', err);
        this.errorMsg.set(err.error?.message || err.message || 'Failed to upload document.');
        this.isUploading.set(false);
        this.uploadProgress.set(null);
      }
    });
  }
}
