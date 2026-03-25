import { Component, inject, OnInit, signal, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudService } from '../../../../core/services/fraud.service';

// Evidence interface
export interface Evidence {
  evidenceId: number;
  fileName: string;
  originalFileName: string;
  fileType: EvidenceType;
  filePath: string;
  fileSize: number;
  mimeType: string;
  description?: string;
  uploadedBy: string;
  uploadedAt: string;
  tags: string[];
  isCritical: boolean;
  chainOfCustodyNotes?: string;
}

export type EvidenceType = 'PHOTO' | 'DOCUMENT' | 'REPORT' | 'COMMUNICATION' | 'VIDEO' | 'AUDIO' | 'OTHER';

// Evidence statistics interface
export interface EvidenceStatistics {
  totalFiles: number;
  totalSizeBytes: number;
  photoCount: number;
  documentCount: number;
  reportCount: number;
  communicationCount: number;
  formattedTotalSize?: string;
}

@Component({
  selector: 'app-evidence-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="evidence-manager">
      <!-- Header with Statistics -->
      <div class="mb-6">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-xl font-bold text-gray-900 flex items-center gap-2">
            <span class="material-icons">folder</span>
            Evidence Repository
          </h3>
          <button
            (click)="refreshEvidence()"
            class="px-3 py-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors flex items-center gap-2"
          >
            <span class="material-icons">refresh</span>
            Refresh
          </button>
        </div>

        @if (statistics()) {
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div class="bg-blue-50 p-3 rounded-lg">
              <div class="font-medium text-blue-900">Total Files</div>
              <div class="text-2xl font-bold text-blue-700">{{ statistics()!.totalFiles }}</div>
            </div>
            <div class="bg-green-50 p-3 rounded-lg">
              <div class="font-medium text-green-900">Photos</div>
              <div class="text-2xl font-bold text-green-700">{{ statistics()!.photoCount }}</div>
            </div>
            <div class="bg-purple-50 p-3 rounded-lg">
              <div class="font-medium text-purple-900">Documents</div>
              <div class="text-2xl font-bold text-purple-700">{{ statistics()!.documentCount }}</div>
            </div>
            <div class="bg-orange-50 p-3 rounded-lg">
              <div class="font-medium text-orange-900">Total Size</div>
              <div class="text-lg font-bold text-orange-700">{{ formatFileSize(statistics()!.totalSizeBytes) }}</div>
            </div>
          </div>
        }
      </div>

      <!-- Upload Area -->
      <div class="mb-6">
        <div
          class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center transition-colors"
          [class.border-blue-400]="isDragOver()"
          [class.bg-blue-50]="isDragOver()"
          (dragover)="onDragOver($event)"
          (dragleave)="onDragLeave($event)"
          (drop)="onDrop($event)"
        >
          @if (isUploading()) {
            <div class="flex items-center justify-center">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
              <span class="ml-3 text-gray-600">Uploading evidence...</span>
            </div>
          } @else {
            <span class="material-icons text-4xl text-gray-400 mb-4">cloud_upload</span>
            <h4 class="text-lg font-medium text-gray-900 mb-2">Upload Evidence</h4>
            <p class="text-gray-600 mb-4">Drag and drop files here, or click to browse</p>
            <input
              type="file"
              #fileInput
              multiple
              (change)="onFileSelect($event)"
              class="hidden"
              accept="image/*,.pdf,.doc,.docx,.txt"
            />
            <button
              (click)="fileInput.click()"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              Choose Files
            </button>
          }
        </div>

        <!-- Upload Form -->
        @if (selectedFiles().length > 0 && !isUploading()) {
          <div class="mt-4 p-4 bg-gray-50 rounded-lg">
            <h5 class="font-medium text-gray-900 mb-3">Upload Details</h5>

            <!-- Selected Files List -->
            <div class="mb-4">
              @for (file of selectedFiles(); track file.name) {
                <div class="flex items-center justify-between p-2 bg-white rounded border mb-2">
                  <div class="flex items-center">
                    <span class="material-icons text-gray-500 mr-2">{{ getFileIcon(file.type) }}</span>
                    <span class="text-sm">{{ file.name }} ({{ formatFileSize(file.size) }})</span>
                  </div>
                  <button
                    (click)="removeFile(file)"
                    class="text-red-500 hover:text-red-700"
                  >
                    <span class="material-icons text-sm">close</span>
                  </button>
                </div>
              }
            </div>

            <!-- Upload Options -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  [(ngModel)]="uploadDescription"
                  placeholder="Describe this evidence..."
                  class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  rows="2"
                ></textarea>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Tags (comma-separated)</label>
                <input
                  type="text"
                  [(ngModel)]="uploadTags"
                  placeholder="evidence, photo, scene..."
                  class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
                <div class="mt-1">
                  <label class="flex items-center">
                    <input
                      type="checkbox"
                      [(ngModel)]="isCritical"
                      class="mr-2"
                    />
                    <span class="text-sm text-gray-700">Mark as critical evidence</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="flex gap-3">
              <button
                (click)="uploadEvidence()"
                class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
              >
                Upload All Files
              </button>
              <button
                (click)="clearSelection()"
                class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors"
              >
                Clear Selection
              </button>
            </div>
          </div>
        }
      </div>

      <!-- Evidence List -->
      <div>
        <!-- Filters -->
        <div class="flex flex-wrap gap-4 mb-4">
          <select
            [(ngModel)]="filterType"
            (ngModelChange)="applyFilters()"
            class="px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">All Types</option>
            <option value="PHOTO">Photos</option>
            <option value="DOCUMENT">Documents</option>
            <option value="REPORT">Reports</option>
            <option value="COMMUNICATION">Communications</option>
            <option value="VIDEO">Videos</option>
            <option value="AUDIO">Audio</option>
            <option value="OTHER">Other</option>
          </select>

          <input
            type="text"
            [(ngModel)]="searchTerm"
            (ngModelChange)="applyFilters()"
            placeholder="Search evidence..."
            class="flex-1 min-w-64 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />

          <label class="flex items-center">
            <input
              type="checkbox"
              [(ngModel)]="showCriticalOnly"
              (ngModelChange)="applyFilters()"
              class="mr-2"
            />
            <span class="text-sm text-gray-700">Critical only</span>
          </label>
        </div>

        <!-- Evidence Items -->
        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            <span class="ml-3 text-gray-600">Loading evidence...</span>
          </div>
        } @else if (filteredEvidence().length === 0) {
          <div class="text-center py-12 text-gray-500">
            <span class="material-icons text-6xl opacity-50">folder_open</span>
            <p class="mt-2">No evidence files found</p>
          </div>
        } @else {
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            @for (evidence of filteredEvidence(); track evidence.evidenceId) {
              <div class="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow">

                <!-- File Preview -->
                <div class="h-32 bg-gray-100 flex items-center justify-center">
                  @if (isImageFile(evidence.mimeType)) {
                    <img
                      [src]="getFilePreviewUrl(evidence.filePath)"
                      [alt]="evidence.originalFileName"
                      class="h-full w-full object-cover"
                      (error)="$event.target.style.display='none'"
                    />
                  } @else {
                    <span class="material-icons text-4xl text-gray-400">{{ getFileIcon(evidence.mimeType) }}</span>
                  }
                  @if (evidence.isCritical) {
                    <div class="absolute top-2 right-2 bg-red-500 text-white px-2 py-1 rounded text-xs">
                      CRITICAL
                    </div>
                  }
                </div>

                <!-- File Details -->
                <div class="p-4">
                  <h5 class="font-medium text-gray-900 mb-2 truncate" [title]="evidence.originalFileName">
                    {{ evidence.originalFileName }}
                  </h5>

                  @if (evidence.description) {
                    <p class="text-sm text-gray-600 mb-2 line-clamp-2">{{ evidence.description }}</p>
                  }

                  <div class="text-xs text-gray-500 space-y-1 mb-3">
                    <div>Type: {{ evidence.fileType }}</div>
                    <div>Size: {{ formatFileSize(evidence.fileSize) }}</div>
                    <div>Uploaded: {{ formatDate(evidence.uploadedAt) }}</div>
                    <div>By: {{ evidence.uploadedBy }}</div>
                  </div>

                  @if (evidence.tags && evidence.tags.length > 0) {
                    <div class="mb-3">
                      @for (tag of evidence.tags.slice(0, 3); track tag) {
                        <span class="inline-block bg-gray-100 text-gray-700 px-2 py-1 rounded text-xs mr-1 mb-1">
                          {{ tag }}
                        </span>
                      }
                      @if (evidence.tags.length > 3) {
                        <span class="text-xs text-gray-500">+{{ evidence.tags.length - 3 }} more</span>
                      }
                    </div>
                  }

                  <!-- Actions -->
                  <div class="flex gap-2">
                    <button
                      (click)="downloadEvidence(evidence)"
                      class="flex-1 px-3 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600 transition-colors"
                    >
                      <span class="material-icons text-sm">download</span>
                    </button>
                    <button
                      (click)="editEvidence(evidence)"
                      class="flex-1 px-3 py-1 bg-gray-500 text-white rounded text-sm hover:bg-gray-600 transition-colors"
                    >
                      <span class="material-icons text-sm">edit</span>
                    </button>
                    <button
                      (click)="deleteEvidence(evidence)"
                      class="flex-1 px-3 py-1 bg-red-500 text-white rounded text-sm hover:bg-red-600 transition-colors"
                    >
                      <span class="material-icons text-sm">delete</span>
                    </button>
                  </div>
                </div>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .line-clamp-2 {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .evidence-manager {
      max-width: 100%;
    }

    .drag-over {
      border-color: #3b82f6;
      background-color: #eff6ff;
    }
  `]
})
export class EvidenceManagerComponent implements OnInit {
  // Input properties
  investigationCaseId = input.required<number>();

  // Output events
  evidenceUploaded = output<Evidence>();
  evidenceDeleted = output<number>();

  private fraudService = inject(FraudService);

  // State signals
  evidence = signal<Evidence[]>([]);
  filteredEvidence = signal<Evidence[]>([]);
  statistics = signal<EvidenceStatistics | null>(null);
  isLoading = signal(false);
  isUploading = signal(false);
  isDragOver = signal(false);

  // File upload state
  selectedFiles = signal<File[]>([]);
  uploadDescription = signal('');
  uploadTags = signal('');
  isCritical = signal(false);

  // Filter state
  filterType = signal<EvidenceType | ''>('');
  searchTerm = signal('');
  showCriticalOnly = signal(false);

  ngOnInit(): void {
    this.loadEvidence();
  }

  // File handling methods
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
    const files = event.dataTransfer?.files;
    if (files) {
      this.addFiles(Array.from(files));
    }
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.addFiles(Array.from(input.files));
    }
  }

  private addFiles(files: File[]): void {
    const validFiles = files.filter(file => this.validateFile(file));
    this.selectedFiles.set([...this.selectedFiles(), ...validFiles]);
  }

  private validateFile(file: File): boolean {
    const maxSize = 10 * 1024 * 1024; // 10MB
    const allowedTypes = ['image/', 'application/pdf', 'text/', 'application/msword', 'application/vnd.openxml'];

    if (file.size > maxSize) {
      alert(`File ${file.name} is too large. Maximum size is 10MB.`);
      return false;
    }

    if (!allowedTypes.some(type => file.type.startsWith(type))) {
      alert(`File ${file.name} is not a supported file type.`);
      return false;
    }

    return true;
  }

  removeFile(file: File): void {
    this.selectedFiles.set(this.selectedFiles().filter(f => f !== file));
  }

  clearSelection(): void {
    this.selectedFiles.set([]);
    this.uploadDescription.set('');
    this.uploadTags.set('');
    this.isCritical.set(false);
  }

  async uploadEvidence(): Promise<void> {
    if (this.selectedFiles().length === 0) return;

    this.isUploading.set(true);

    try {
      const tags = this.uploadTags().split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);

      // Upload files sequentially to avoid overwhelming the server
      for (const file of this.selectedFiles()) {
        const formData = new FormData();
        formData.append('file', file);
        if (this.uploadDescription()) {
          formData.append('description', this.uploadDescription());
        }
        if (tags.length > 0) {
          tags.forEach(tag => formData.append('tags', tag));
        }
        formData.append('isCritical', this.isCritical().toString());

        // Call the backend API (you'll need to implement this in FraudService)
        const uploadedEvidence = await this.uploadSingleFile(formData);
        this.evidenceUploaded.emit(uploadedEvidence);
      }

      this.clearSelection();
      this.loadEvidence(); // Refresh the list

    } catch (error) {
      console.error('Error uploading evidence:', error);
      alert('Error uploading evidence. Please try again.');
    } finally {
      this.isUploading.set(false);
    }
  }

  private async uploadSingleFile(formData: FormData): Promise<Evidence> {
    // This is a placeholder - you'll need to implement the actual upload API call
    // return this.fraudService.uploadEvidence(this.investigationCaseId(), formData).toPromise();
    throw new Error('Upload API not implemented yet');
  }

  // Evidence management methods
  loadEvidence(): void {
    this.isLoading.set(true);
    // Placeholder for loading evidence from API
    // this.fraudService.getCaseEvidence(this.investigationCaseId()).subscribe({
    //   next: (evidence) => {
    //     this.evidence.set(evidence);
    //     this.applyFilters();
    //     this.loadStatistics();
    //   },
    //   error: (error) => console.error('Error loading evidence:', error),
    //   complete: () => this.isLoading.set(false)
    // });

    // Mock data for now
    setTimeout(() => {
      this.evidence.set([]);
      this.applyFilters();
      this.isLoading.set(false);
    }, 1000);
  }

  refreshEvidence(): void {
    this.loadEvidence();
  }

  applyFilters(): void {
    let filtered = this.evidence();

    if (this.filterType()) {
      filtered = filtered.filter(e => e.fileType === this.filterType());
    }

    if (this.searchTerm()) {
      const term = this.searchTerm().toLowerCase();
      filtered = filtered.filter(e =>
        e.originalFileName.toLowerCase().includes(term) ||
        e.description?.toLowerCase().includes(term) ||
        e.tags?.some(tag => tag.toLowerCase().includes(term))
      );
    }

    if (this.showCriticalOnly()) {
      filtered = filtered.filter(e => e.isCritical);
    }

    this.filteredEvidence.set(filtered);
  }

  // Evidence actions
  downloadEvidence(evidence: Evidence): void {
    // Implement download logic
    console.log('Download evidence:', evidence);
  }

  editEvidence(evidence: Evidence): void {
    // Implement edit modal/form
    console.log('Edit evidence:', evidence);
  }

  deleteEvidence(evidence: Evidence): void {
    if (confirm(`Are you sure you want to delete "${evidence.originalFileName}"?`)) {
      // Implement delete API call
      console.log('Delete evidence:', evidence);
      this.evidenceDeleted.emit(evidence.evidenceId);
    }
  }

  // Utility methods
  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType.includes('pdf')) return 'picture_as_pdf';
    if (mimeType.includes('word') || mimeType.includes('document')) return 'description';
    if (mimeType.includes('sheet') || mimeType.includes('excel')) return 'table_chart';
    if (mimeType.startsWith('video/')) return 'videocam';
    if (mimeType.startsWith('audio/')) return 'audiotrack';
    return 'insert_drive_file';
  }

  isImageFile(mimeType: string): boolean {
    return mimeType.startsWith('image/');
  }

  getFilePreviewUrl(filePath: string): string {
    // Return URL for file preview - you'll need to implement this
    return `/api/evidence/preview/${filePath}`;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  private loadStatistics(): void {
    // Load evidence statistics
    // this.fraudService.getEvidenceStatistics(this.investigationCaseId()).subscribe({
    //   next: (stats) => this.statistics.set(stats),
    //   error: (error) => console.error('Error loading statistics:', error)
    // });
  }
}