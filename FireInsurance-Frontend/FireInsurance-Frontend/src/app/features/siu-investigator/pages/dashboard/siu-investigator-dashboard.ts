import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TokenService } from '../../../../core/services';
import { FraudService, SiuCaseResponse } from '../../../../core/services/fraud.service';

@Component({
  selector: 'app-siu-investigator-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 p-6">
      <div class="max-w-7xl mx-auto">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">SIU Investigator Dashboard</h1>
          <p class="text-gray-600 mt-2">Welcome, {{ username() }}!</p>
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            <span class="ml-3 text-gray-600">Loading case statistics...</span>
          </div>
        } @else {
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <div class="glass-card p-6">
              <div class="flex items-center">
                <span class="material-icons text-3xl text-blue-500">assignment</span>
                <div class="ml-4">
                  <p class="text-sm font-medium text-gray-600">Assigned Cases</p>
                  <p class="text-2xl font-bold text-gray-900">{{ assignedCases() }}</p>
                </div>
              </div>
            </div>

            <div class="glass-card p-6">
              <div class="flex items-center">
                <span class="material-icons text-3xl text-orange-500">pending</span>
                <div class="ml-4">
                  <p class="text-sm font-medium text-gray-600">Pending Investigations</p>
                  <p class="text-2xl font-bold text-gray-900">{{ pendingCases() }}</p>
                </div>
              </div>
            </div>

            <div class="glass-card p-6">
              <div class="flex items-center">
                <span class="material-icons text-3xl text-green-500">check_circle</span>
                <div class="ml-4">
                  <p class="text-sm font-medium text-gray-600">Completed Cases</p>
                  <p class="text-2xl font-bold text-gray-900">{{ completedCases() }}</p>
                </div>
              </div>
            </div>

            <div class="glass-card p-6">
              <div class="flex items-center">
                <span class="material-icons text-3xl text-red-500">warning</span>
                <div class="ml-4">
                  <p class="text-sm font-medium text-gray-600">High Priority</p>
                  <p class="text-2xl font-bold text-gray-900">{{ highPriorityCases() }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="glass-card p-6">
            <h2 class="text-xl font-bold text-gray-900 mb-4">Recent Activity</h2>
            <p class="text-gray-600">Real-time case statistics loaded from backend API. High priority cases are determined by fraud score ≥ 80%.</p>
          </div>
        }
      </div>
    </div>
  `
})
export class SiuInvestigatorDashboardComponent implements OnInit {
  private tokenService = inject(TokenService);
  private fraudService = inject(FraudService);

  username = signal('');
  assignedCases = signal(0);
  pendingCases = signal(0);
  completedCases = signal(0);
  highPriorityCases = signal(0);
  isLoading = signal(true);

  ngOnInit(): void {
    this.username.set(this.tokenService.getUsername() || 'SIU Investigator');
    this.loadSiuStatistics();
  }

  private loadSiuStatistics(): void {
    this.isLoading.set(true);

    // Get current username from token
    const username = this.tokenService.getUsername();
    if (!username) {
      console.error('No username found in token');
      this.setDummyDataWithError('Unable to get current user information');
      return;
    }

    // First, get all SIU investigators to find the current user's investigator profile
    this.fraudService.getSiuInvestigators().subscribe({
      next: (investigators) => {
        console.log('🔍 Debug: Available SIU investigators:', investigators.map(inv => ({ id: inv.investigatorId, username: inv.username })));
        console.log('🔍 Debug: Current user username:', username);

        // Find the investigator that matches the current username
        const currentInvestigator = investigators.find(inv => inv.username === username);

        if (!currentInvestigator) {
          console.warn('Available SIU investigators:', investigators.map(inv => ({ id: inv.investigatorId, username: inv.username })));
          console.warn('Current user username:', username);
          this.setDummyDataWithError(`SIU investigator profile not found for "${username}"`);
          return;
        }

        console.log('✅ Found investigator profile:', { id: currentInvestigator.investigatorId, username: currentInvestigator.username });

        // Now fetch statistics for this specific investigator
        this.loadInvestigatorSpecificStats(currentInvestigator.investigatorId);
      },
      error: (error) => {
        console.error('Error fetching SIU investigators:', error);
        this.setDummyDataWithError('Failed to load investigator information');
      }
    });
  }

  private loadInvestigatorSpecificStats(investigatorId: number): void {
    console.log(`🔍 Debug: Fetching cases for investigator ID: ${investigatorId}`);

    // Call debug endpoint first to understand the data state
    this.fraudService.debugInvestigatorData(investigatorId).subscribe({
      next: (debugData) => {
        console.log('🐛 Debug data from backend:', debugData);

        // Check if we need to sync (claims assigned but no investigation cases)
        if (debugData.assignedClaimsCount > 0 && debugData.investigationCasesCount === 0) {
          console.log('⚠️ Found assigned claims but no investigation cases. Triggering sync repair...');
          this.attemptSyncRepair(investigatorId);
        }
      },
      error: (err) => {
        console.error('Debug endpoint error:', err);
      }
    });

    // Fetch cases assigned specifically to this investigator
    this.fraudService.getClaimsByInvestigator(investigatorId).subscribe({
      next: (claims) => {
        console.log(`✅ Loaded ${claims.length} claims for investigator ID: ${investigatorId}`);
        console.log('📋 Claims details:', claims);

        // If no claims found, try to sync assignments as a recovery mechanism
        if (claims.length === 0) {
          console.log('⚠️ No claims found. Attempting to sync assignments...');
          this.attemptSyncRepair(investigatorId);
        }

        this.calculateStatisticsFromClaims(claims);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('❌ Error fetching investigator-specific cases:', error);
        console.log('🔄 Trying alternative statistics endpoint...');
        // Try alternative: get statistics endpoint
        this.loadStatisticsEndpoint(investigatorId);
      }
    });
  }

  private loadStatisticsEndpoint(investigatorId: number): void {
    // Try the dedicated statistics endpoint if available
    this.fraudService.getInvestigatorStatistics(investigatorId).subscribe({
      next: (stats) => {
        console.log('✅ Loaded investigator statistics:', stats);
        this.assignedCases.set(stats.totalCases || 0);
        this.pendingCases.set(stats.assignedCases || 0);
        this.completedCases.set(stats.completedCases || 0);
        this.highPriorityCases.set(stats.highPriorityCases || 0);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error fetching investigator statistics:', error);
        this.setDummyDataWithError('Failed to load case statistics');
      }
    });
  }

  private attemptSyncRepair(investigatorId: number): void {
    console.log('🔧 Attempting to sync/repair assignments...');

    // Call sync endpoint (admin function)
    this.fraudService.syncClaimAssignments().subscribe({
      next: (syncResult) => {
        console.log('🔧 Sync result:', syncResult);
        if (syncResult.syncedCases > 0) {
          console.log(`✅ Synced ${syncResult.syncedCases} missing investigation cases. Reloading...`);
          // Reload the statistics after sync
          setTimeout(() => {
            this.loadInvestigatorSpecificStats(investigatorId);
          }, 1000);
        }
      },
      error: (err) => {
        console.error('❌ Sync repair failed:', err);
        // Continue with normal error handling
      }
    });
  }

  private calculateStatisticsFromClaims(claims: any[]): void {
    const totalCases = claims.length;
    const pendingCases = claims.filter(claim =>
      claim.fraudStatus === 'SIU_INVESTIGATION' ||
      claim.fraudStatus === 'UNDER_REVIEW' ||
      claim.status === 'SUBMITTED' ||
      claim.status === 'UNDER_REVIEW'
    ).length;
    const completedCases = claims.filter(claim =>
      claim.fraudStatus === 'CLEARED' ||
      claim.fraudStatus === 'CONFIRMED_FRAUD' ||
      claim.status === 'APPROVED' ||
      claim.status === 'REJECTED'
    ).length;
    const highPriorityCases = claims.filter(claim =>
      (claim.fraudScore && claim.fraudScore >= 80) ||
      claim.riskLevel === 'HIGH' ||
      claim.riskLevel === 'CRITICAL'
    ).length;

    this.assignedCases.set(totalCases);
    this.pendingCases.set(pendingCases);
    this.completedCases.set(completedCases);
    this.highPriorityCases.set(highPriorityCases);

    console.log(`📊 Statistics calculated: Total=${totalCases}, Pending=${pendingCases}, Completed=${completedCases}, High Priority=${highPriorityCases}`);
  }

  private setDummyDataWithError(errorMessage: string): void {
    console.error('⚠️ Using fallback data due to error:', errorMessage);
    // Set all to 0 instead of dummy data to show real state
    this.assignedCases.set(0);
    this.pendingCases.set(0);
    this.completedCases.set(0);
    this.highPriorityCases.set(0);
    this.isLoading.set(false);
  }
}