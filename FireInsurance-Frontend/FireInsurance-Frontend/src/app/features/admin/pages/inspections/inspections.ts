import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Surveyor } from '../../../../features/admin/services/admin.service';

export interface UnifiedInspection {
  type: 'PROPERTY' | 'CLAIM';
  id: number; // propertyId or claimId
  inspectionId?: number; // the ID of the actual scheduled inspection if assigned
  entityIdString: string; // e.g., '#PROP-12' or '#CLM-4'
  status: string;
  surveyorName?: string;
  date?: string;
}

@Component({
  selector: 'app-inspections',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inspections.html'
})
export class AdminInspectionsComponent implements OnInit {
  private adminService = inject(AdminService);

  unifiedItems = signal<UnifiedInspection[]>([]);
  filteredItems = signal<UnifiedInspection[]>([]);
  surveyors = signal<Surveyor[]>([]);
  
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  assignmentSuccess = signal<string>('');
  assignmentError = signal<string>('');
  activeTypeFilter = signal<string>('ALL');

  // We map entity id to the chosen surveyor ID in the dropdown
  selectedSurveyors: { [entityKey: string]: number } = {};

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    // Fetch properties, claims, and internal inspections and surveyors
    Promise.all([
      this.adminService.getAllProperties().toPromise(),
      this.adminService.getAllClaims().toPromise(),
      this.adminService.getAllInspections().toPromise(),
      this.adminService.getAllSurveyors().toPromise()
    ]).then(([props, claims, propertyInspections, surveyors]) => {
      this.surveyors.set(surveyors || []);

      const items: UnifiedInspection[] = [];

      // Add actual assigned property inspections
      const propInspMap = new Map((propertyInspections || []).map(pi => [pi.property?.propertyId, pi]));

      (props || []).forEach(p => {
        const insp = propInspMap.get(p.propertyId);
        if (insp) {
           items.push({
             type: 'PROPERTY',
             id: p.propertyId,
             inspectionId: insp.id,
             entityIdString: `#PROP-${p.propertyId}`,
             status: insp.status || 'ASSIGNED',
             surveyorName: insp.surveyor?.firstName + ' ' + insp.surveyor?.lastName,
             date: insp.scheduledDate
           });
        } else {
           items.push({
             type: 'PROPERTY',
             id: p.propertyId,
             entityIdString: `#PROP-${p.propertyId}`,
             status: 'PENDING',
           });
        }
      });

      // Add claims
      // Note: we don't have a getAllClaimInspections without admin access (yet the backend has it). 
      // But we can just use the claim's own status (INSPECTING) to derive if it's assigned.
      // Or we can just build the claim list and allow assigning if they are under review or pending.
      (claims || []).forEach(c => {
         items.push({
           type: 'CLAIM',
           id: c.claimId,
           entityIdString: `#CLM-${c.claimId}`,
           status: c.status === 'SUBMITTED' || c.status === 'UNDER_REVIEW' || c.status === 'INSPECTING' ? 'PENDING' : c.status,
           date: c.incidentDate
         });
      });

      this.unifiedItems.set(items);
      this.applyFilter(this.activeTypeFilter());
      this.isLoading.set(false);
    }).catch(err => {
      console.error(err);
      this.errorMessage.set('Failed to load inspection data.');
      this.isLoading.set(false);
    });
  }

  applyFilter(type: string): void {
    this.activeTypeFilter.set(type);
    if (type === 'ALL') {
      this.filteredItems.set(this.unifiedItems());
    } else {
      this.filteredItems.set(this.unifiedItems().filter(i => i.type === type));
    }
  }

  onSurveyorSelect(itemType: string, itemId: number, surveyorId: string): void {
    const key = `${itemType}_${itemId}`;
    this.selectedSurveyors[key] = Number(surveyorId);
  }

  assignSurveyor(item: UnifiedInspection): void {
    const key = `${item.type}_${item.id}`;
    const sId = this.selectedSurveyors[key];
    
    if (!sId) {
      this.assignmentError.set('Please select a surveyor first.');
      return;
    }

    this.assignmentError.set('');
    this.assignmentSuccess.set('');
    
    if (item.type === 'PROPERTY') {
      this.adminService.assignInspectionToSurveyor(item.id, sId).subscribe({
        next: () => {
          this.assignmentSuccess.set('Property inspection assigned successfully.');
          this.loadData();
          setTimeout(() => this.assignmentSuccess.set(''), 4000);
        },
        error: (err) => {
          this.assignmentError.set('Failed to assign property inspection.');
          console.error(err);
        }
      });
    } else if (item.type === 'CLAIM') {
      this.adminService.assignClaimToSurveyor(item.id, sId).subscribe({
        next: () => {
          this.assignmentSuccess.set('Claim inspection assigned successfully.');
          
          // Let's also verify that we update the claim status to INSPECTING
          this.adminService.updateClaimStatus(item.id, 'INSPECTING').subscribe();

          this.loadData();
          setTimeout(() => this.assignmentSuccess.set(''), 4000);
        },
        error: (err) => {
          this.assignmentError.set('Failed to assign claim inspection.');
          console.error(err);
        }
      });
    }
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'bg-yellow-100 text-yellow-800',
      'ASSIGNED': 'bg-blue-100 text-blue-800',
      'INSPECTING': 'bg-purple-100 text-purple-800',
      'COMPLETED': 'bg-green-100 text-green-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }
}
