import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudService } from '../../../../core/services/fraud.service';
import { BlacklistEntry, BlacklistRequest, BlacklistType } from '../../../../core/models/claim.model';

@Component({
  selector: 'app-blacklist',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './blacklist.html'
})
export class BlacklistComponent implements OnInit {
  private readonly fraudService = inject(FraudService);

  // Data signals
  blacklistEntries = signal<BlacklistEntry[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  // Filter signals
  selectedType = signal<BlacklistType | 'ALL'>('ALL');
  searchQuery = signal('');

  // Modal signals
  showAddModal = signal(false);
  showDeleteModal = signal(false);
  selectedEntryForDelete = signal<BlacklistEntry | null>(null);
  isSubmitting = signal(false);

  // New entry form
  newEntry = signal<BlacklistRequest>({
    type: 'USER',
    value: '',
    reason: ''
  });

  // Blacklist types for filter
  blacklistTypes: (BlacklistType | 'ALL')[] = ['ALL', 'USER', 'PHONE', 'ADDRESS', 'EMAIL'];

  // Filtered entries
  filteredEntries = computed(() => {
    let entries = this.blacklistEntries();
    const type = this.selectedType();
    const query = this.searchQuery().toLowerCase().trim();

    if (type !== 'ALL') {
      entries = entries.filter(e => e.type === type);
    }

    if (query) {
      entries = entries.filter(e =>
        e.value.toLowerCase().includes(query) ||
        (e.reason && e.reason.toLowerCase().includes(query))
      );
    }

    return entries;
  });

  // Stats computed
  totalCount = computed(() => this.blacklistEntries().length);
  userCount = computed(() => this.blacklistEntries().filter(e => e.type === 'USER').length);
  phoneCount = computed(() => this.blacklistEntries().filter(e => e.type === 'PHONE').length);
  addressCount = computed(() => this.blacklistEntries().filter(e => e.type === 'ADDRESS').length);
  emailCount = computed(() => this.blacklistEntries().filter(e => e.type === 'EMAIL').length);

  ngOnInit(): void {
    this.loadBlacklist();
  }

  loadBlacklist(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.fraudService.getAllBlacklist().subscribe({
      next: (entries) => {
        this.blacklistEntries.set(entries);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load blacklist:', err);
        this.error.set('Failed to load blacklist entries');
        this.isLoading.set(false);
      }
    });
  }

  filterByType(type: BlacklistType | 'ALL'): void {
    this.selectedType.set(type);
  }

  onSearchChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  // Add Entry Modal
  openAddModal(): void {
    this.newEntry.set({
      type: 'USER',
      value: '',
      reason: ''
    });
    this.showAddModal.set(true);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
  }

  updateNewEntryType(type: BlacklistType): void {
    this.newEntry.update(entry => ({ ...entry, type }));
  }

  updateNewEntryValue(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.newEntry.update(entry => ({ ...entry, value: input.value }));
  }

  updateNewEntryReason(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.newEntry.update(entry => ({ ...entry, reason: textarea.value }));
  }

  submitNewEntry(): void {
    const entry = this.newEntry();
    if (!entry.value.trim()) {
      return;
    }

    this.isSubmitting.set(true);

    this.fraudService.addToBlacklist(entry).subscribe({
      next: (newEntry) => {
        this.blacklistEntries.update(entries => [newEntry, ...entries]);
        this.closeAddModal();
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Failed to add to blacklist:', err);
        this.isSubmitting.set(false);
      }
    });
  }

  // Delete Entry Modal
  openDeleteModal(entry: BlacklistEntry): void {
    this.selectedEntryForDelete.set(entry);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.selectedEntryForDelete.set(null);
  }

  confirmDelete(): void {
    const entry = this.selectedEntryForDelete();
    if (!entry) return;

    this.isSubmitting.set(true);

    this.fraudService.removeFromBlacklist(entry.id).subscribe({
      next: () => {
        this.blacklistEntries.update(entries =>
          entries.filter(e => e.id !== entry.id)
        );
        this.closeDeleteModal();
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Failed to remove from blacklist:', err);
        this.isSubmitting.set(false);
      }
    });
  }

  // Helper methods
  getTypeIcon(type: BlacklistType): string {
    const icons: Record<BlacklistType, string> = {
      USER: 'person_off',
      PHONE: 'phone_disabled',
      ADDRESS: 'location_off',
      EMAIL: 'mail_lock'
    };
    return icons[type];
  }

  getTypeColor(type: BlacklistType): string {
    const colors: Record<BlacklistType, string> = {
      USER: 'bg-red-100 text-red-700 border-red-200',
      PHONE: 'bg-orange-100 text-orange-700 border-orange-200',
      ADDRESS: 'bg-purple-100 text-purple-700 border-purple-200',
      EMAIL: 'bg-blue-100 text-blue-700 border-blue-200'
    };
    return colors[type];
  }

  getFilterClass(type: BlacklistType | 'ALL'): string {
    const isSelected = this.selectedType() === type;
    if (type === 'ALL') {
      return isSelected
        ? 'bg-gray-800 text-white'
        : 'bg-gray-100 text-gray-600 hover:bg-gray-200';
    }
    const colorMap: Record<BlacklistType, { active: string; inactive: string }> = {
      USER: {
        active: 'bg-red-600 text-white',
        inactive: 'bg-red-50 text-red-600 hover:bg-red-100'
      },
      PHONE: {
        active: 'bg-orange-600 text-white',
        inactive: 'bg-orange-50 text-orange-600 hover:bg-orange-100'
      },
      ADDRESS: {
        active: 'bg-purple-600 text-white',
        inactive: 'bg-purple-50 text-purple-600 hover:bg-purple-100'
      },
      EMAIL: {
        active: 'bg-blue-600 text-white',
        inactive: 'bg-blue-50 text-blue-600 hover:bg-blue-100'
      }
    };
    return isSelected ? colorMap[type].active : colorMap[type].inactive;
  }

  getCountForType(type: BlacklistType | 'ALL'): number {
    if (type === 'ALL') return this.totalCount();
    const countMap: Record<BlacklistType, () => number> = {
      USER: () => this.userCount(),
      PHONE: () => this.phoneCount(),
      ADDRESS: () => this.addressCount(),
      EMAIL: () => this.emailCount()
    };
    return countMap[type]();
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getValuePlaceholder(): string {
    const type = this.newEntry().type;
    const placeholders: Record<BlacklistType, string> = {
      USER: 'Enter username or user ID',
      PHONE: 'Enter phone number (e.g., +1234567890)',
      ADDRESS: 'Enter full address',
      EMAIL: 'Enter email address'
    };
    return placeholders[type];
  }
}
