import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-investigation-reports',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 p-6">
      <div class="max-w-7xl mx-auto">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Investigation Reports</h1>
          <p class="text-gray-600 mt-2">Create and manage investigation reports</p>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          <div class="glass-card p-6">
            <div class="flex items-center">
              <span class="material-icons text-3xl text-blue-500">description</span>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-600">Draft Reports</p>
                <p class="text-2xl font-bold text-gray-900">3</p>
              </div>
            </div>
          </div>

          <div class="glass-card p-6">
            <div class="flex items-center">
              <span class="material-icons text-3xl text-green-500">check_circle</span>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-600">Submitted Reports</p>
                <p class="text-2xl font-bold text-gray-900">15</p>
              </div>
            </div>
          </div>

          <div class="glass-card p-6">
            <div class="flex items-center">
              <span class="material-icons text-3xl text-orange-500">pending</span>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-600">Under Review</p>
                <p class="text-2xl font-bold text-gray-900">2</p>
              </div>
            </div>
          </div>
        </div>

        <div class="glass-card p-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-xl font-bold text-gray-900">Recent Reports</h2>
            <button class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
              New Report
            </button>
          </div>

          <div class="space-y-4">
            <div class="border border-gray-200 rounded-lg p-4 bg-white">
              <div class="flex justify-between items-start mb-2">
                <h3 class="text-lg font-semibold text-gray-900">Investigation Report - CLAIM-2024-001</h3>
                <span class="px-2 py-1 bg-green-100 text-green-800 rounded-full text-sm">Completed</span>
              </div>
              <p class="text-gray-600 mb-2">Final investigation report for fire damage claim with fraud indicators</p>
              <div class="flex justify-between items-center text-sm text-gray-500">
                <span>Created: March 22, 2024</span>
                <span>Status: Submitted</span>
              </div>
            </div>

            <div class="border border-gray-200 rounded-lg p-4 bg-white">
              <div class="flex justify-between items-start mb-2">
                <h3 class="text-lg font-semibold text-gray-900">Preliminary Report - CLAIM-2024-002</h3>
                <span class="px-2 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm">Draft</span>
              </div>
              <p class="text-gray-600 mb-2">Initial findings for property theft claim investigation</p>
              <div class="flex justify-between items-center text-sm text-gray-500">
                <span>Created: March 21, 2024</span>
                <span>Status: In Progress</span>
              </div>
            </div>
          </div>

          <div class="mt-6">
            <p class="text-gray-600">Report creation and management tools will be available here.</p>
          </div>
        </div>
      </div>
    </div>
  `
})
export class InvestigationReportsComponent {
}