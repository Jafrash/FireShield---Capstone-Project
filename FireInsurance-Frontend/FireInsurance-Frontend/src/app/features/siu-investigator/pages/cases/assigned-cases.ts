import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FraudService, InvestigationCase, InvestigationStatus } from '../../../../core/services/fraud.service';
import { TokenService } from '../../../../core/services';

@Component({
  selector: 'app-assigned-cases',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-gray-50 p-6">
      <div class="max-w-7xl mx-auto">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Assigned Cases</h1>
          <p class="text-gray-600 mt-2">View and manage your assigned investigation cases</p>
        </div>

        <!-- Filters and Search -->
        <div class="mb-6 flex flex-wrap gap-4">
          <div class="flex-1 min-w-64">
            <input
              type="text"
              placeholder="Search by claim number or customer name..."
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              (input)="onSearchChange($event)"
            />
          </div>
          <select
            class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            (change)="onStatusFilterChange($event)"
          >
            <option value="">All Statuses</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="INVESTIGATING">Investigating</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="COMPLETED">Completed</option>
          </select>
          <select
            class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            (change)="onPriorityFilterChange($event)"
          >
            <option value="">All Priorities</option>
            <option value="1">Critical</option>
            <option value="2">High</option>
            <option value="3">Medium</option>
            <option value="4">Low</option>
            <option value="5">Lowest</option>
          </select>
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            <span class="ml-3 text-gray-600">Loading assigned cases...</span>
          </div>
        } @else if (error()) {
          <div class="glass-card p-6 text-center">
            <div class="text-red-500 mb-2">
              <span class="material-icons text-4xl">error_outline</span>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Error Loading Cases</h3>
            <p class="text-gray-600 mb-4">{{ error() }}</p>
            <button
              (click)="loadAssignedCases()"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              Try Again
            </button>
          </div>
        } @else {
          <div class="glass-card p-6">
            <div class="flex justify-between items-center mb-6">
              <h2 class="text-xl font-bold text-gray-900">
                Active Cases ({{ filteredCases().length }})
              </h2>
              <button
                (click)="loadAssignedCases()"
                class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors flex items-center gap-2"
              >
                <span class="material-icons text-lg">refresh</span>
                Refresh
              </button>
            </div>

            @if (filteredCases().length === 0) {
              <div class="text-center py-12">
                <span class="material-icons text-6xl text-gray-300 mb-4">assignment</span>
                <h3 class="text-lg font-semibold text-gray-900 mb-2">No Cases Found</h3>
                <p class="text-gray-600 mb-4">
                  @if (searchTerm() || statusFilter() || priorityFilter()) {
                    No cases match your current filters. Try adjusting your search criteria.
                  } @else {
                    You have no assigned investigation cases at the moment.
                  }
                </p>
                @if (!searchTerm() && !statusFilter() && !priorityFilter()) {
                  <button
                    (click)="refreshCases()"
                    [disabled]="isLoading()"
                    class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 mx-auto"
                  >
                    <span class="material-icons text-sm">refresh</span>
                    {{ isLoading() ? 'Checking...' : 'Refresh Cases' }}
                  </button>
                }
              </div>
            } @else {
              <div class="space-y-4">
                @for (case of filteredCases(); track case.investigationId) {
                  <div class="border border-gray-200 rounded-lg p-6 bg-white hover:shadow-md transition-shadow">
                    <div class="flex justify-between items-start mb-4">
                      <div class="flex-1">
                        <div class="flex items-center gap-3 mb-2">
                          <h3 class="text-lg font-semibold text-gray-900">
                            Case #{{ case.investigationId }} - Claim #{{ case.claim?.claimNumber || case.claim?.claimId }}
                          </h3>
                          <span [class]="getStatusBadgeClass(case.status)">
                            {{ getStatusLabel(case.status) }}
                          </span>
                          <span [class]="getPriorityBadgeClass(case.priorityLevel)">
                            {{ getPriorityLabel(case.priorityLevel) }}
                          </span>
                        </div>
                        <p class="text-gray-600 mb-3">
                          {{ case.claim?.description || 'Investigation case for potential fraud indicators' }}
                        </p>
                        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm text-gray-500">
                          <div>
                            <span class="font-medium">Assigned:</span>
                            {{ formatDate(case.assignedDate) }}
                          </div>
                          @if (case.startedDate) {
                            <div>
                              <span class="font-medium">Started:</span>
                              {{ formatDate(case.startedDate) }}
                            </div>
                          }
                          @if (case.claim?.fraudScore) {
                            <div>
                              <span class="font-medium">Fraud Score:</span>
                              <span [class]="getFraudScoreClass(case.claim.fraudScore)">
                                {{ case.claim.fraudScore }}%
                              </span>
                            </div>
                          }
                          @if (case.claim?.claimAmount) {
                            <div>
                              <span class="font-medium">Claim Amount:</span>
                              ₹{{ formatCurrency(case.claim.claimAmount) }}
                            </div>
                          }
                        </div>
                      </div>
                    </div>

                    @if (case.initialNotes) {
                      <div class="mb-4 p-3 bg-gray-50 rounded-lg">
                        <h4 class="font-medium text-gray-900 mb-1">Initial Notes:</h4>
                        <p class="text-gray-600 text-sm">{{ case.initialNotes }}</p>
                      </div>
                    }

                    <div class="flex justify-between items-center">
                      <div class="flex gap-2">
                        <button
                          class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors text-sm"
                          [routerLink]="['/siu-investigator/case', case.investigationId]"
                        >
                          View Details
                        </button>
                        @if (case.status === 'ASSIGNED') {
                          <button
                            (click)="startInvestigation(case.investigationId)"
                            class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition-colors text-sm"
                          >
                            Start Investigation
                          </button>
                        }
                        @if (case.status === 'INVESTIGATING') {
                          <button
                            (click)="updateStatus(case.investigationId, 'UNDER_REVIEW')"
                            class="px-4 py-2 bg-orange-500 text-white rounded hover:bg-orange-600 transition-colors text-sm"
                          >
                            Submit for Review
                          </button>
                        }
                      </div>
                      <button
                        (click)="addQuickNote(case.investigationId)"
                        class="px-3 py-1 text-gray-600 hover:text-gray-800 transition-colors text-sm flex items-center gap-1"
                      >
                        <span class="material-icons text-lg">note_add</span>
                        Add Note
                      </button>
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `
})
export class AssignedCasesComponent implements OnInit {
  private fraudService = inject(FraudService);
  private tokenService = inject(TokenService);

  cases = signal<InvestigationCase[]>([]);
  filteredCases = signal<InvestigationCase[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  // Filter states
  searchTerm = signal('');
  statusFilter = signal<InvestigationStatus | ''>('');
  priorityFilter = signal<number | ''>('');

  ngOnInit(): void {
    this.loadAssignedCases();
  }

  loadAssignedCases(): void {
    // Get current username from token
    const username = this.tokenService.getUsername();
    if (!username) {
      this.error.set('Unable to get current user information');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    // First, get all SIU investigators to find the current user's investigator profile
    this.fraudService.getSiuInvestigators().subscribe({
      next: (investigators) => {
        // Find the investigator that matches the current username
        const currentInvestigator = investigators.find(inv => inv.username === username);

        if (!currentInvestigator) {
          console.warn('Available SIU investigators:', investigators.map(inv => ({ id: inv.investigatorId, username: inv.username, badge: inv.badgeNumber })));
          console.warn('Current user username:', username);
          this.error.set(
            `SIU investigator profile not found for "${username}". ` +
            `Please ask your administrator to add you to the SIU investigators table, or run the database script: add_mohit_kumar_siu_investigator.sql`
          );
          this.isLoading.set(false);
          return;
        }

        // Now fetch assigned cases using the investigator's numeric ID
        this.fraudService.getAssignedCases(currentInvestigator.investigatorId).subscribe({
          next: (cases) => {
            console.log(`📋 Loaded ${cases.length} investigation cases for investigator ID: ${currentInvestigator.investigatorId}`);

            // If no cases found, check if there are assigned claims that need sync repair
            if (cases.length === 0) {
              console.log('⚠️ No investigation cases found. Checking for assigned claims that need sync...');
              this.checkAndRepairMissingCases(currentInvestigator.investigatorId);
            }

            this.cases.set(cases);
            this.applyFilters();
            this.isLoading.set(false);
          },
          error: (error) => {
            console.error('Error fetching assigned cases:', error);
            this.error.set('Failed to load assigned cases. Please try again.');
            this.isLoading.set(false);
          }
        });
      },
      error: (error) => {
        console.error('Error fetching SIU investigators:', error);
        this.error.set('Failed to load investigator information. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  private checkAndRepairMissingCases(investigatorId: number): void {
    console.log('🔍 Checking for assigned claims that need investigation case creation...');

    // Check debug data to see if there are assigned claims without investigation cases
    this.fraudService.debugInvestigatorData(investigatorId).subscribe({
      next: (debugData) => {
        console.log('🐛 Debug data for missing cases:', debugData);

        if (debugData.assignedClaimsCount > 0 && debugData.investigationCasesCount === 0) {
          console.log(`⚠️ Found ${debugData.assignedClaimsCount} assigned claims but no investigation cases. Triggering sync repair...`);
          this.attemptSyncRepair(investigatorId);
        } else if (debugData.assignedClaimsCount === 0) {
          console.log('ℹ️ No assigned claims found. This investigator has no current assignments.');
        } else {
          console.log('ℹ️ Investigation cases match assigned claims. No repair needed.');
        }
      },
      error: (err) => {
        console.error('❌ Failed to check debug data:', err);
      }
    });
  }

  private attemptSyncRepair(investigatorId: number): void {
    console.log('🔧 Attempting to sync/repair missing investigation cases...');

    this.fraudService.syncClaimAssignments().subscribe({
      next: (syncResult) => {
        console.log('🔧 Sync repair result:', syncResult);

        if (syncResult.success && syncResult.syncedCases > 0) {
          console.log(`✅ Successfully synced ${syncResult.syncedCases} missing investigation cases. Reloading assigned cases...`);

          // Reload the assigned cases after successful sync
          setTimeout(() => {
            this.loadAssignedCasesAfterSync(investigatorId);
          }, 1500);
        } else if (syncResult.errors && syncResult.errors.length > 0) {
          console.error('❌ Sync repair had errors:', syncResult.errors);
        } else {
          console.log('ℹ️ Sync repair completed but no cases were created.');
        }
      },
      error: (err) => {
        console.error('❌ Sync repair failed:', err);
      }
    });
  }

  private loadAssignedCasesAfterSync(investigatorId: number): void {
    console.log('🔄 Reloading assigned cases after sync repair...');

    this.fraudService.getAssignedCases(investigatorId).subscribe({
      next: (cases) => {
        console.log(`✅ Reloaded ${cases.length} investigation cases after sync`);
        this.cases.set(cases);
        this.applyFilters();

        if (cases.length > 0) {
          console.log('🎉 Sync repair successful! Cases are now visible.');
        }
      },
      error: (error) => {
        console.error('❌ Error reloading cases after sync:', error);
      }
    });
  }

  refreshCases(): void {
    console.log('🔄 Manual refresh requested by user');
    this.loadAssignedCases();
  }

  onSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm.set(target.value.toLowerCase());
    this.applyFilters();
  }

  onStatusFilterChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.statusFilter.set(target.value as InvestigationStatus | '');
    this.applyFilters();
  }

  onPriorityFilterChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.priorityFilter.set(target.value ? parseInt(target.value) : '');
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.cases();

    // Apply search filter
    if (this.searchTerm()) {
      filtered = filtered.filter(caseItem =>
        caseItem.claim?.claimNumber?.toLowerCase().includes(this.searchTerm()) ||
        caseItem.claim?.claimId?.toString().includes(this.searchTerm()) ||
        caseItem.investigationId.toString().includes(this.searchTerm())
      );
    }

    // Apply status filter
    if (this.statusFilter()) {
      filtered = filtered.filter(caseItem => caseItem.status === this.statusFilter());
    }

    // Apply priority filter
    if (this.priorityFilter()) {
      filtered = filtered.filter(caseItem => caseItem.priorityLevel === this.priorityFilter());
    }

    this.filteredCases.set(filtered);
  }

  startInvestigation(investigationId: number): void {
    this.updateStatus(investigationId, 'INVESTIGATING');
  }

  updateStatus(investigationId: number, newStatus: InvestigationStatus): void {
    this.fraudService.updateCaseStatus(investigationId, { status: newStatus }).subscribe({
      next: (updatedCase) => {
        // Update the case in the local array
        const cases = this.cases();
        const index = cases.findIndex(c => c.investigationId === investigationId);
        if (index !== -1) {
          cases[index] = updatedCase;
          this.cases.set([...cases]);
          this.applyFilters();
        }
      },
      error: (error) => {
        console.error('Error updating case status:', error);
        this.error.set('Failed to update case status. Please try again.');
      }
    });
  }

  addQuickNote(investigationId: number): void {
    const note = prompt('Enter a quick note for this investigation:');
    if (note && note.trim()) {
      this.fraudService.addInvestigationNote(investigationId, { note: note.trim() }).subscribe({
        next: (updatedCase) => {
          // Update the case in the local array
          const cases = this.cases();
          const index = cases.findIndex(c => c.investigationId === investigationId);
          if (index !== -1) {
            cases[index] = updatedCase;
            this.cases.set([...cases]);
            this.applyFilters();
          }
        },
        error: (error) => {
          console.error('Error adding note:', error);
          alert('Failed to add note. Please try again.');
        }
      });
    }
  }

  // Utility methods
  getStatusBadgeClass(status: InvestigationStatus): string {
    const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium';
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
    const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium';
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

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN').format(amount);
  }
}