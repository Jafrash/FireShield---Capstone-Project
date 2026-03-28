import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoadingService } from '../../../../core/services/loading.service';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (loadingService.isLoading()) {
      <div class="fixed inset-0 z-9999 flex items-center justify-center bg-black/30 backdrop-blur-sm">
        <div class="bg-white rounded-2xl shadow-2xl p-8 flex flex-col items-center space-y-4">
          <!-- Spinner -->
          <div class="relative w-16 h-16">
            <div class="absolute inset-0 border-4 border-gray-200 rounded-full"></div>
            <div class="absolute inset-0 border-4 border-[#C72B32] border-t-transparent rounded-full animate-spin"></div>
          </div>
          
          <!-- Loading Text -->
          <div class="text-center">
            <p class="text-lg font-semibold text-gray-900">Loading...</p>
            <p class="text-sm text-gray-500 mt-1">Please wait</p>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .animate-spin {
      animation: spin 1s linear infinite;
    }
  `]
})
export class LoadingComponent {
  loadingService = inject(LoadingService);
}
