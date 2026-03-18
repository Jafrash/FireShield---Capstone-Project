import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Underwriter } from '../../../../features/admin/services/admin.service';
import { Claim } from '../../../../core/models/claim.model';
import { Surveyor } from '../../../../features/admin/services/admin.service';
import { DocumentService } from '../../../../core/services/document.service';
import { Document } from '../../../../core/models/document.model';

@Component({
  selector: 'app-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './claims.html'
})
export class ClaimsComponent implements OnInit {
  private adminService = inject(AdminService);
  
  claims = signal<Claim[]>([]);
  filteredClaims = signal<Claim[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  searchTerm = signal<string>('');

  surveyors = signal<Surveyor[]>([]);
  selectedSurveyors: Record<number, number> = {};

  underwriters = signal<Underwriter[]>([]);
  selectedUnderwriters: Record<number, number> = {};
  
  // Document Viewer logic
  private documentService = inject(DocumentService);
  selectedClaimForDocs = signal<Claim | null>(null);
  claimDocuments = signal<Document[]>([]);
  isDocsLoading = signal(false);
  successMessage = signal('');

  ngOnInit(): void {
    this.loadClaims();
    this.loadSurveyors();
    this.loadUnderwriters();
  }

  loadSurveyors(): void {
    this.adminService.getAllSurveyors().subscribe({
      next: (data) => this.surveyors.set(data),
      error: (err) => console.error('Error loading surveyors', err)
    });
  }

  loadUnderwriters(): void {
    this.adminService.getAllUnderwriters().subscribe({
      next: (data) => this.underwriters.set(data || []),
      error: (err) => console.error('Error loading underwriters', err)
    });
  }

  loadClaims(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getAllClaims().subscribe({
      next: (data) => {
        this.claims.set(data);
        this.filteredClaims.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading claims:', error);
        this.errorMessage.set('Failed to load claims');
        this.isLoading.set(false);
      }
    });
  }

  searchClaims(term: string): void {
    this.searchTerm.set(term);
    const lowerTerm = term.toLowerCase();
    
    if (!lowerTerm) {
      this.filteredClaims.set(this.claims());
      return;
    }

    const filtered = this.claims().filter(claim =>
      claim.claimId.toString().includes(lowerTerm) ||
      claim.status.toLowerCase().includes(lowerTerm) ||
      claim.description.toLowerCase().includes(lowerTerm)
    );

    this.filteredClaims.set(filtered);
  }

  updateClaimStatus(id: number, status: string): void {
    this.adminService.updateClaimStatus(id, status).subscribe({
      next: () => {
        const updated = this.claims().map(c => 
          c.claimId === id ? { ...c, status: status as any } : c
        );
        this.claims.set(updated);
        this.filteredClaims.set(updated);
      },
      error: (error) => {
        console.error('Error updating claim:', error);
        alert('Failed to update claim status');
      }
    });
  }

  approveClaim(id: number): void {
    if (!confirm('Are you sure you want to approve this claim?')) {
      return;
    }

    this.adminService.approveClaim(id).subscribe({
      next: (updatedClaim) => {
        const updated = this.claims().map(c => 
          c.claimId === id ? updatedClaim : c
        );
        this.claims.set(updated);
        this.filteredClaims.set(updated);
        alert('Claim approved successfully!');
      },
      error: (error) => {
        console.error('Error approving claim:', error);
        alert('Failed to approve claim. Please try again.');
      }
    });
  }

  rejectClaim(id: number): void {
    if (!confirm('Are you sure you want to reject this claim?')) {
      return;
    }

    this.adminService.rejectClaim(id).subscribe({
      next: (updatedClaim) => {
        const updated = this.claims().map(c => 
          c.claimId === id ? updatedClaim : c
        );
        this.claims.set(updated);
        this.filteredClaims.set(updated);
        alert('Claim rejected successfully!');
      },
      error: (error) => {
        console.error('Error rejecting claim:', error);
        alert('Failed to reject claim. Please try again.');
      }
    });
  }

  assignInspection(claim: Claim): void {
    const claimId = claim.claimId;
    const surveyorId = this.selectedSurveyors[claimId];
    if (!surveyorId) {
      alert('Please select a surveyor first');
      return;
    }
    
    this.adminService.assignClaimToSurveyor(claimId, surveyorId).subscribe({
      next: (insResponse) => {
        // Update local claim state visually 
        const updated = this.claims().map(c => {
          if (c.claimId === claimId) {
            return { ...c, status: 'INSPECTING' as any }; 
          }
          return c;
        });
        
        this.claims.set(updated);
        this.filteredClaims.set(updated);
        alert('Claim inspection assigned successfully');
      },
      error: (err) => {
        console.error('Error assigning claim inspection:', err);
        const msg = err.error?.message || err.message || 'Failed to assign claim inspection';
        alert(msg);
      }
    });
  }

  assignUnderwriter(claim: Claim): void {
    const claimId = claim.claimId;
    const underwriterId = this.selectedUnderwriters[claimId];
    if (!underwriterId) {
      this.successMessage.set('');
      alert('Please select an underwriter first');
      return;
    }
    this.adminService.assignUnderwriterToClaim(claimId, underwriterId).subscribe({
      next: () => {
        this.successMessage.set('Underwriter assigned to claim successfully!');
        
        // Update local state to hide the assign button immediately
        const updatedClaims = this.claims().map(c => {
          if (c.claimId === claimId) {
            return { ...c, underwriterId: underwriterId };
          }
          return c;
        });
        this.claims.set(updatedClaims);
        this.filteredClaims.set(updatedClaims);

        setTimeout(() => this.successMessage.set(''), 3500);
      },
      error: (err) => {
        console.error('Error assigning underwriter to claim:', err);
        const msg = err.error?.message || 'Failed to assign underwriter.';
        alert(msg);
      }
    });
  }

  viewDocuments(claim: Claim): void {
    this.selectedClaimForDocs.set(claim);
    this.isDocsLoading.set(true);
    // Fetch specifically CLAIM docs by claimId
    this.documentService.getDocumentsForEntity(claim.claimId, 'CLAIM').subscribe({
      next: (docs) => {
        // filter to claim stage docs
        const claimLevelDocs = docs.filter((d: any) => 
          d.documentStage === 'CLAIM_STAGE' || 
          d.documentType === 'CLAIM_FORM' || 
          d.documentType === 'SPOT_SURVEY_REPORT' ||
          d.documentType === 'FIRE_BRIGADE_REPORT' ||
          d.documentType === 'DAMAGE_PHOTOS' ||
          d.documentType === 'REPAIR_ESTIMATE' ||
          d.documentType === 'OTHER'
        );
        this.claimDocuments.set(claimLevelDocs);
        this.isDocsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load claim documents:', err);
        this.isDocsLoading.set(false);
      }
    });
  }

  closeDocsModal(): void {
    this.selectedClaimForDocs.set(null);
    this.claimDocuments.set([]);
  }

  downloadDocument(doc: Document): void {
    this.documentService.downloadDocument(doc.documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.fileName || 'Document';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Download failed', err)
    });
  }
}
