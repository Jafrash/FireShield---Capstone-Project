import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './unauthorized.html',
  styleUrls: ['./unauthorized.css']
})
export class UnauthorizedComponent {
  private router = inject(Router);
  private authService = inject(AuthService);

  /**
   * Navigate back to user's appropriate dashboard
   */
  goToDashboard(): void {
    this.authService.redirectToDashboard();
  }

  /**
   * Logout and go to login page
   */
  logout(): void {
    this.authService.logout();
  }
}
