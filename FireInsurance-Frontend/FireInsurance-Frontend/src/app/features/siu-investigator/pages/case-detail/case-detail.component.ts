import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FraudService, InvestigationCase, InvestigationStatus } from '../../../../core/services/fraud.service';
import { TokenService } from '../../../../core/services';

@Component({
  selector: 'app-case-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 p-6">
      <div class="max-w-7xl mx-auto">
        <!-- Header -->
        <div class="mb-8 flex items-center justify-between">
          <div class="flex items-center gap-4">
            <button
              [routerLink]="['/siu-investigator/cases']"
              class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <span class="material-icons">arrow_back</span>
            </button>
            <div>
              <h1 class="text-3xl font-bold text-gray-900">
                @if (investigationCase()) {
                  Case #{{ investigationCase()!.investigationId }}
                } @else {
                  Case Details
                }
              </h1>
              <p class="text-gray-600 mt-1">
                @if (investigationCase()) {
                  Investigation for Claim #{{ investigationCase()!.claim?.claimNumber || investigationCase()!.claim?.claimId }}
                } @else {
                  Loading case information...
                }
              </p>
            </div>
          </div>

          @if (investigationCase()) {
            <div class="flex items-center gap-3">
              <span [class]="getStatusBadgeClass(investigationCase()!.status)">
                {{ getStatusLabel(investigationCase()!.status) }}
              </span>
              <span [class]="getPriorityBadgeClass(investigationCase()!.priorityLevel)">
                {{ getPriorityLabel(investigationCase()!.priorityLevel) }}
              </span>
            </div>
          }
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            <span class="ml-3 text-gray-600">Loading case details...</span>
          </div>
        } @else if (error()) {
          <div class="glass-card p-6 text-center">
            <div class="text-red-500 mb-2">
              <span class="material-icons text-4xl">error_outline</span>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Error Loading Case</h3>
            <p class="text-gray-600 mb-4">{{ error() }}</p>
            <button
              (click)="loadCaseDetails()"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              Try Again
            </button>
          </div>
        } @else if (investigationCase()) {
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

            <!-- Main Content -->
            <div class="lg:col-span-2 space-y-6">

              <!-- Case Overview -->
              <div class="glass-card p-6">
                <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                  <span class="material-icons">info</span>
                  Case Overview
                </h2>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

                  <!-- Basic Information -->
                  <div>
                    <h3 class="font-semibold text-gray-900 mb-3">Basic Information</h3>
                    <div class="space-y-2 text-sm">
                      <div class="flex justify-between">
                        <span class="text-gray-600">Investigation ID:</span>
                        <span class="font-medium">{{ investigationCase()!.investigationId }}</span>
                      </div>
                      <div class="flex justify-between">
                        <span class="text-gray-600">Claim ID:</span>
                        <span class="font-medium">{{ investigationCase()!.claim?.claimId }}</span>
                      </div>
                      @if (investigationCase()!.claim?.claimNumber) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Claim Number:</span>
                          <span class="font-medium">{{ investigationCase()!.claim.claimNumber }}</span>
                        </div>
                      }
                      <div class="flex justify-between">
                        <span class="text-gray-600">Assigned Date:</span>
                        <span class="font-medium">{{ formatDate(investigationCase()!.assignedDate) }}</span>
                      </div>
                      @if (investigationCase()!.startedDate) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Started Date:</span>
                          <span class="font-medium">{{ formatDate(investigationCase()!.startedDate!) }}</span>
                        </div>
                      }
                      @if (investigationCase()!.completedDate) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Completed Date:</span>
                          <span class="font-medium">{{ formatDate(investigationCase()!.completedDate!) }}</span>
                        </div>
                      }
                    </div>
                  </div>

                  <!-- Claim Information -->
                  <div>
                    <h3 class="font-semibold text-gray-900 mb-3">Claim Information</h3>
                    <div class="space-y-2 text-sm">
                      @if (investigationCase()!.claim?.claimAmount) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Claim Amount:</span>
                          <span class="font-medium">₹{{ formatCurrency(investigationCase()!.claim.claimAmount) }}</span>
                        </div>
                      }
                      @if (investigationCase()!.claim?.fraudScore) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Fraud Score:</span>
                          <span [class]="getFraudScoreClass(investigationCase()!.claim.fraudScore)">
                            {{ investigationCase()!.claim.fraudScore }}%
                          </span>
                        </div>
                      }
                      @if (investigationCase()!.claim?.riskLevel) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Risk Level:</span>
                          <span [class]="getRiskLevelClass(investigationCase()!.claim.riskLevel)">
                            {{ investigationCase()!.claim.riskLevel }}
                          </span>
                        </div>
                      }
                      @if (investigationCase()!.claim?.status) {
                        <div class="flex justify-between">
                          <span class="text-gray-600">Claim Status:</span>
                          <span class="font-medium">{{ investigationCase()!.claim.status }}</span>
                        </div>
                      }
                    </div>
                  </div>
                </div>

                @if (investigationCase()!.claim?.description) {
                  <div class="mt-4">
                    <h3 class="font-semibold text-gray-900 mb-2">Claim Description</h3>
                    <p class="text-gray-600 text-sm">{{ investigationCase()!.claim.description }}</p>
                  </div>
                }
              </div>

              <!-- Investigation Notes -->
              <div class="glass-card p-6">
                <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                  <span class="material-icons">note_add</span>
                  Investigation Notes
                </h2>

                <!-- Add New Note -->
                <div class="mb-4">
                  <div class="flex gap-2">
                    <textarea
                      [(ngModel)]="newNote"
                      placeholder="Add investigation note..."
                      class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
                      rows="3"
                    ></textarea>
                    <button
                      (click)="addNote()"
                      [disabled]="!newNote().trim() || isAddingNote()"
                      class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                    >
                      @if (isAddingNote()) {
                        <span class="material-icons animate-spin">hourglass_empty</span>
                      } @else {
                        Add
                      }
                    </button>
                  </div>
                </div>

                <!-- Existing Notes -->
                @if (investigationCase()!.initialNotes) {
                  <div class="space-y-3">
                    @for (note of getFormattedNotes(); track $index) {
                      <div class="p-3 bg-gray-50 rounded-lg border-l-4 border-blue-500">
                        <div class="flex justify-between items-start mb-1">
                          <span class="text-xs text-gray-500">{{ note.author }} • {{ note.timestamp }}</span>
                        </div>
                        <p class="text-gray-700 text-sm">{{ note.content }}</p>
                      </div>
                    }
                  </div>
                } @else {
                  <div class="text-center py-4 text-gray-500">
                    <span class="material-icons text-4xl opacity-50">note</span>
                    <p>No investigation notes yet.</p>
                  </div>
                }
              </div>
            </div>

            <!-- Sidebar -->
            <div class="space-y-6">

              <!-- Status Management -->
              <div class="glass-card p-6">
                <h3 class="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <span class="material-icons">format_list_bulleted</span>
                  Status Management
                </h3>

                <div class="space-y-3">
                  @if (investigationCase()!.status === 'ASSIGNED') {
                    <button
                      (click)="updateStatus('INVESTIGATING')"
                      class="w-full px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
                    >
                      Start Investigation
                    </button>
                  }

                  @if (investigationCase()!.status === 'INVESTIGATING') {
                    <button
                      (click)="updateStatus('UNDER_REVIEW')"
                      class="w-full px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition-colors"
                    >
                      Submit for Review
                    </button>
                  }

                  @if (investigationCase()!.status === 'UNDER_REVIEW') {
                    <button
                      (click)="updateStatus('INVESTIGATING')"
                      class="w-full px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                    >
                      Return to Investigation
                    </button>
                    <button
                      (click)="updateStatus('COMPLETED')"
                      class="w-full px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
                    >
                      Mark as Completed
                    </button>
                  }
                </div>
              </div>

              <!-- Investigation Timeline -->
              <div class="glass-card p-6">
                <h3 class="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <span class="material-icons">timeline</span>
                  Timeline
                </h3>

                <div class="space-y-4">
                  @if (investigationCase()!.completedDate) {
                    <div class="flex items-center gap-3">
                      <div class="w-3 h-3 bg-green-500 rounded-full"></div>
                      <div class="flex-1">
                        <p class="text-sm font-medium text-gray-900">Investigation Completed</p>
                        <p class="text-xs text-gray-500">{{ formatDate(investigationCase()!.completedDate!) }}</p>
                      </div>
                    </div>
                  }

                  @if (investigationCase()!.startedDate) {
                    <div class="flex items-center gap-3">
                      <div class="w-3 h-3 bg-blue-500 rounded-full"></div>
                      <div class="flex-1">
                        <p class="text-sm font-medium text-gray-900">Investigation Started</p>
                        <p class="text-xs text-gray-500">{{ formatDate(investigationCase()!.startedDate!) }}</p>
                      </div>
                    </div>
                  }

                  <div class="flex items-center gap-3">
                    <div class="w-3 h-3 bg-gray-400 rounded-full"></div>
                    <div class="flex-1">
                      <p class="text-sm font-medium text-gray-900">Case Assigned</p>
                      <p class="text-xs text-gray-500">{{ formatDate(investigationCase()!.assignedDate) }}</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Investigator Info -->
              @if (investigationCase()!.assignedInvestigator) {
                <div class="glass-card p-6">
                  <h3 class="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                    <span class="material-icons">person</span>
                    Assigned Investigator
                  </h3>

                  <div class="space-y-2 text-sm">
                    <div class="flex justify-between">
                      <span class="text-gray-600">Name:</span>
                      <span class="font-medium">{{ investigationCase()!.assignedInvestigator.username }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gray-600">Badge:</span>
                      <span class="font-medium">{{ investigationCase()!.assignedInvestigator.badgeNumber }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gray-600">Specialization:</span>
                      <span class="font-medium">{{ investigationCase()!.assignedInvestigator.specialization }}</span>
                    </div>
                  </div>
                </div>
              }
            </div>
          </div>
        }
      </div>
    </div>
  `
})
export class CaseDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fraudService = inject(FraudService);
  private tokenService = inject(TokenService);

  investigationCase = signal<InvestigationCase | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);
  newNote = signal('');
  isAddingNote = signal(false);

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.loadCaseDetails(parseInt(id));
      }
    });
  }

  loadCaseDetails(investigationId?: number): void {
    const id = investigationId || parseInt(this.route.snapshot.params['id']);

    this.isLoading.set(true);
    this.error.set(null);

    this.fraudService.getCaseDetails(id).subscribe({
      next: (caseDetails) => {
        this.investigationCase.set(caseDetails);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error fetching case details:', error);
        this.error.set('Failed to load case details. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  updateStatus(newStatus: InvestigationStatus): void {
    const caseId = this.investigationCase()?.investigationId;
    if (!caseId) return;

    this.fraudService.updateCaseStatus(caseId, { status: newStatus }).subscribe({
      next: (updatedCase) => {
        this.investigationCase.set(updatedCase);
      },
      error: (error) => {
        console.error('Error updating case status:', error);
        alert('Failed to update case status. Please try again.');
      }
    });
  }

  addNote(): void {
    const note = this.newNote().trim();
    const caseId = this.investigationCase()?.investigationId;

    if (!note || !caseId) return;

    this.isAddingNote.set(true);

    this.fraudService.addInvestigationNote(caseId, { note }).subscribe({
      next: (updatedCase) => {
        this.investigationCase.set(updatedCase);
        this.newNote.set('');
        this.isAddingNote.set(false);
      },
      error: (error) => {
        console.error('Error adding note:', error);
        alert('Failed to add note. Please try again.');
        this.isAddingNote.set(false);
      }
    });
  }

  getFormattedNotes(): Array<{content: string, author: string, timestamp: string}> {
    const notes = this.investigationCase()?.initialNotes;
    if (!notes) return [];

    return notes.split('\n\n').map(note => {
      const match = note.match(/^\[(.+?) - (.+?)\]: (.+)$/s);
      if (match) {
        return {
          timestamp: match[1],
          author: match[2],
          content: match[3]
        };
      }
      return {
        timestamp: 'Unknown',
        author: 'System',
        content: note
      };
    });
  }

  // Utility methods (same as in assigned-cases component)
  getStatusBadgeClass(status: InvestigationStatus): string {
    const baseClasses = 'px-3 py-1 rounded-full text-sm font-medium';
    switch (status) {
      case 'ASSIGNED': return `${baseClasses} bg-blue-100 text-blue-800`;
      case 'INVESTIGATING': return `${baseClasses} bg-orange-100 text-orange-800`;
      case 'UNDER_REVIEW': return `${baseClasses} bg-purple-100 text-purple-800`;
      case 'COMPLETED': return `${baseClasses} bg-green-100 text-green-800`;
      default: return `${baseClasses} bg-gray-100 text-gray-800`;
    }
  }

  getStatusLabel(status: InvestigationStatus): string {
    switch (status) {
      case 'ASSIGNED': return 'Assigned';
      case 'INVESTIGATING': return 'Investigating';
      case 'UNDER_REVIEW': return 'Under Review';
      case 'COMPLETED': return 'Completed';
      default: return status;
    }
  }

  getPriorityBadgeClass(priority: number): string {
    const baseClasses = 'px-3 py-1 rounded-full text-sm font-medium';
    if (priority <= 2) return `${baseClasses} bg-red-100 text-red-800`;
    if (priority === 3) return `${baseClasses} bg-yellow-100 text-yellow-800`;
    return `${baseClasses} bg-green-100 text-green-800`;
  }

  getPriorityLabel(priority: number): string {
    switch (priority) {
      case 1: return 'Critical';
      case 2: return 'High';
      case 3: return 'Medium';
      case 4: return 'Low';
      case 5: return 'Lowest';
      default: return 'Medium';
    }
  }

  getFraudScoreClass(score: number): string {
    if (score >= 80) return 'text-red-600 font-semibold';
    if (score >= 60) return 'text-orange-600 font-medium';
    if (score >= 40) return 'text-yellow-600';
    return 'text-green-600';
  }

  getRiskLevelClass(riskLevel: string): string {
    switch (riskLevel) {
      case 'CRITICAL': return 'text-red-600 font-semibold uppercase';
      case 'HIGH': return 'text-orange-600 font-medium uppercase';
      case 'MEDIUM': return 'text-yellow-600 uppercase';
      case 'LOW': return 'text-green-600 uppercase';
      default: return 'text-gray-600 uppercase';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN').format(amount);
  }
}