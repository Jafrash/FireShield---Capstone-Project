import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TokenService } from '../../../../core/services';

@Component({
  selector: 'app-siu-investigator-profile',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 p-6">
      <div class="max-w-7xl mx-auto">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">My Profile</h1>
          <p class="text-gray-600 mt-2">Manage your investigator profile information</p>
        </div>

        <div class="glass-card p-6">
          <h2 class="text-xl font-bold text-gray-900 mb-4">Profile Information</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Name</label>
              <p class="text-gray-900">{{ username() }}</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Badge Number</label>
              <p class="text-gray-900">SIU-001</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Specialization</label>
              <p class="text-gray-900">General Investigation</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Experience</label>
              <p class="text-gray-900">5 Years</p>
            </div>
          </div>
          <div class="mt-6">
            <p class="text-gray-600">Profile management features will be available here.</p>
          </div>
        </div>
      </div>
    </div>
  `
})
export class SiuInvestigatorProfileComponent implements OnInit {
  private tokenService = inject(TokenService);

  username = signal('');

  ngOnInit(): void {
    this.username.set(this.tokenService.getUsername() || 'SIU Investigator');
  }
}