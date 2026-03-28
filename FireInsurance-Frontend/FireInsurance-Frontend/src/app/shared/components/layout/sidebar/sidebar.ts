import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TokenService } from '../../../../core/services';
import { UserRole } from '../../../../core/models';

interface NavLink {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  // Styles are provided via Tailwind utility classes in templates
})
export class SidebarComponent implements OnInit {
  private tokenService = inject(TokenService);
  private router = inject(Router);

  userRole: UserRole | null = null;
  navLinks: NavLink[] = [];
  isCollapsed = false;

  ngOnInit(): void {
    this.userRole = this.tokenService.getRole();
    this.loadNavLinks();
  }

  /**
   * Load navigation links based on user role
   */
  private loadNavLinks(): void {
    if (!this.userRole) return;

    switch (this.userRole) {
      case 'ADMIN':
        this.navLinks = [
          { label: 'Dashboard', path: '/admin-dashboard/dashboard', icon: 'dashboard' },
          { label: 'SIU Dashboard', path: '/admin-dashboard/fraud-monitoring', icon: 'security' },
          { label: 'Customers', path: '/admin-dashboard/customers', icon: 'people' },
          { label: 'Surveyors', path: '/admin-dashboard/surveyors', icon: 'badge' },
          { label: 'Underwriters', path: '/admin-dashboard/underwriters', icon: 'manage_accounts' },
          { label: 'SIU Investigators', path: '/admin-dashboard/siu-investigators', icon: 'gavel' },
          { label: 'Blacklist Management', path: '/admin-dashboard/blacklist', icon: 'block' },
          { label: 'Policies', path: '/admin-dashboard/policies', icon: 'description' },
          { label: 'Subscriptions', path: '/admin-dashboard/subscriptions', icon: 'card_membership' },
          { label: 'Claims', path: '/admin-dashboard/claims', icon: 'assignment' }
        ];
        break;

      case 'SIU_INVESTIGATOR':
        this.navLinks = [
          { label: 'Dashboard', path: '/siu-dashboard/dashboard', icon: 'dashboard' }
        ];
        break;

      case 'CUSTOMER':
        this.navLinks = [
          { label: 'Dashboard', path: '/customer/dashboard', icon: 'dashboard' },
          { label: 'My Profile', path: '/customer/profile', icon: 'person' },
          { label: 'My Properties', path: '/customer/properties', icon: 'home' },
          { label: 'Browse Policies', path: '/customer/policies', icon: 'policy' },
          { label: 'My Subscriptions', path: '/customer/subscriptions', icon: 'card_membership' },
          { label: 'My Claims', path: '/customer/claims', icon: 'assignment' }
        ];
        break;

      case 'SURVEYOR':
        this.navLinks = [
          { label: 'Dashboard', path: '/surveyor/dashboard', icon: 'dashboard' },
          { label: 'My Profile', path: '/surveyor/profile', icon: 'person' },
          { label: 'Property Inspections', path: '/surveyor/property-inspections', icon: 'home' },
          { label: 'Claim Inspections', path: '/surveyor/claim-inspections', icon: 'search' }
        ];
        break;

      case 'UNDERWRITER':
        this.navLinks = [
          { label: 'Dashboard', path: '/underwriter-dashboard/dashboard', icon: 'dashboard' },
          { label: 'Policy Subscriptions', path: '/underwriter-dashboard/subscriptions', icon: 'card_membership' },
          { label: 'Claims', path: '/underwriter-dashboard/claims', icon: 'assignment' },
          { label: 'Property Inspections', path: '/underwriter-dashboard/property-inspections', icon: 'home' },
          { label: 'Claim Inspections', path: '/underwriter-dashboard/claim-inspections', icon: 'search' }
        ];
        break;
    }
  }

  /**
   * Toggle sidebar collapsed state
   */
  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  /**
   * Check if link is active
   */
  isActive(path: string): boolean {
    return this.router.url.startsWith(path);
  }
}
