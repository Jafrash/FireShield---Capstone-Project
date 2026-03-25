import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-case-files',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 p-6">
      <div class="max-w-7xl mx-auto">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <span class="material-icons text-red-600 text-4xl">folder_shared</span>
            Case Files
          </h1>
          <p class="text-gray-600 mt-2">Access and manage investigation case files</p>
        </div>
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
          <span class="material-icons text-6xl text-gray-300 mb-4">folder_shared</span>
          <h3 class="text-xl font-semibold text-gray-900 mb-2">Case Files Module</h3>
          <p class="text-gray-600">This section will provide access to all investigation case files and documentation.</p>
        </div>
      </div>
    </div>
  `
})
export class CaseFilesComponent {}