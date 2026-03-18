import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PublicService, PublicPolicy } from '../../services/public.service';

@Component({
  selector: 'app-coverage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './coverage.html'
})
export class CoverageComponent implements OnInit {
  private publicService = inject(PublicService);
  policies: PublicPolicy[] = [];
  isLoading = signal(true);
  errorMessage = signal('');

  ngOnInit() {
    this.publicService.getPolicies().subscribe({
      next: (data) => {
        this.policies = data.slice(0, 3);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load policies', err);
        this.errorMessage.set('Could not load coverage options at this moment.');
        this.isLoading.set(false);
      }
    });
  }
}
