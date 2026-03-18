import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CustomerService } from '../../services/customer.service';
import { Policy } from '../../../../core/models/policy.model';
import { Property } from '../../../../core/models/property.model';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-policy-details',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './policy-details.html'
})
export class PolicyDetailsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private customerService = inject(CustomerService);
  private fb = inject(FormBuilder);

  policy = signal<Policy | null>(null);
  properties = signal<Property[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  showSubscribeModal = signal(false);

  subscribeForm: FormGroup = this.fb.group({
    propertyId: [0, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPolicy(Number(id));
      this.loadProperties();
    }
  }

  loadPolicy(id: number): void {
    this.isLoading.set(true);
    this.customerService.getAllPolicies().subscribe({
      next: (policies) => {
        const found = policies.find(p => p.policyId === id);
        this.policy.set(found || null);
        this.isLoading.set(false);
      },
      error: (err: Error) => {
        console.error('Error loading policy:', err);
        this.errorMessage.set('Failed to load policy details');
        this.isLoading.set(false);
      }
    });
  }

  loadProperties(): void {
    this.customerService.getMyProperties().subscribe({
      next: (props) => this.properties.set(props),
      error: () => {}
    });
  }

  openSubscribeModal(): void {
    this.showSubscribeModal.set(true);
    this.subscribeForm.reset({ propertyId: 0 });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeSubscribeModal(): void {
    this.showSubscribeModal.set(false);
  }

  submitSubscription(): void {
    if (this.subscribeForm.invalid || !this.policy()) return;

    this.isLoading.set(true);
    const payload = {
      propertyId: Number(this.subscribeForm.value.propertyId),
      policyId: this.policy()!.policyId
    };
    this.customerService.subscribeToPolicy(payload).subscribe({
      next: () => {
        this.successMessage.set('Subscription request submitted! Awaiting admin approval.');
        this.isLoading.set(false);
        this.closeSubscribeModal();
        setTimeout(() => this.successMessage.set(''), 5000);
      },
      error: (err: any) => {
        console.error('Error subscribing:', err);
        this.errorMessage.set('Failed to submit subscription. You may need to add a property first.');
        this.isLoading.set(false);
      }
    });
  }

  getDurationYears(): string {
    const months = this.policy()?.durationMonths || 12;
    if (months % 12 === 0) return `${months / 12} Year${months / 12 > 1 ? 's' : ''}`;
    return `${months} Months`;
  }

  goBack(): void {
    this.router.navigate(['/customer/subscriptions']);
  }
}
