import { Component, OnInit, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { CustomerService } from '../../services/customer.service';
import { Property } from '../../../../core/models/property.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';
import { DocumentUploadComponent } from '../../../../shared/components/ui/document-upload/document-upload.component';
import * as L from 'leaflet';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-properties',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DocumentUploadComponent, RouterModule],
  templateUrl: './properties.html',
  styleUrls: ['./properties.css']
})
export class PropertiesComponent implements OnInit, OnDestroy {
  private readonly customerService = inject(CustomerService);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  properties = signal<Property[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showAddModal = signal(false);
  showEditModal = signal(false);
  editingProperty = signal<Property | null>(null);

  // Map state
  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  isMapLoading = signal(false);
  isSearchLoading = signal(false);
  searchQuery = signal('');
  searchResults = signal<any[]>([]);
  showSearchDropdown = signal(false);

  private searchSubject = new Subject<string>();

  addPropertyForm: FormGroup = this.fb.group({
    propertyType: ['', [Validators.required]],
    address: ['', [Validators.required, Validators.minLength(5)]],
    latitude: [null, [Validators.required]],
    longitude: [null, [Validators.required]],
    zipCode: [''],
    areaSqft: [0, [Validators.required, Validators.min(100), Validators.max(1000000), CustomValidators.positiveNumber()]],
    constructionType: ['', [Validators.required]]
  });

  editPropertyForm: FormGroup = this.fb.group({
    propertyType: ['', [Validators.required]],
    address: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(200), CustomValidators.noWhitespace()]],
    areaSqft: [0, [Validators.required, Validators.min(100), Validators.max(1000000), CustomValidators.positiveNumber()]],
    constructionType: ['', [Validators.required]]
  });

  propertyTypes = [
    'RESIDENTIAL',
    'COMMERCIAL',
    'INDUSTRIAL',
    'APARTMENT'
  ];

  constructionTypes = [
    'BRICK',
    'WOOD',
    'CONCRETE',
    'STEEL',
    'MIXED'
  ];

  ngOnInit(): void {
    this.loadProperties();

    // Wire up debounced search pipeline
    this.searchSubject.pipe(
      debounceTime(800),
      distinctUntilChanged(),
      switchMap((query) => {
        if (!query || query.trim().length < 5) {
          this.searchResults.set([]);
          this.showSearchDropdown.set(false);
          this.isSearchLoading.set(false);
          return of([]);
        }
        this.isSearchLoading.set(true);
        const url = `/nominatim/search?format=jsonv2&q=${encodeURIComponent(query)}&limit=5&addressdetails=1`;
        return this.http.get<any[]>(url).pipe(
          catchError(() => {
            this.isSearchLoading.set(false);
            return of([]);
          })
        );
      })
    ).subscribe((results) => {
      this.isSearchLoading.set(false);
      this.searchResults.set(results || []);
      this.showSearchDropdown.set((results && results.length > 0));
    });
  }

  ngOnDestroy(): void {
    this.destroyMap();
    this.searchSubject.complete();
  }

  loadProperties(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.getMyProperties().subscribe({
      next: (properties) => {
        this.properties.set(properties);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load properties');
        console.error('Error loading properties:', err);
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.showAddModal.set(true);
    this.addPropertyForm.reset({
      propertyType: '',
      address: '',
      latitude: null,
      longitude: null,
      zipCode: '',
      areaSqft: 0,
      constructionType: ''
    });
    this.errorMessage.set('');
    this.successMessage.set('');
    
    // Initialize map after angular renders the modal DOM
    setTimeout(() => this.initMap(), 150);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
    this.addPropertyForm.reset();
    this.destroyMap();
  }

  private initMap(): void {
    if (this.map) return;
    
    // Default config for Leaflet icons (solves missing marker issue)
    const iconRetinaUrl = 'assets/marker-icon-2x.png';
    const iconUrl = 'assets/marker-icon.png';
    const shadowUrl = 'assets/marker-shadow.png';
    const DefaultIcon = L.icon({
      iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
      iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
      shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      tooltipAnchor: [16, -28],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = DefaultIcon;

    this.map = L.map('propertyMap').setView([40.7128, -74.0060], 13); // Default to NY
    
    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>',
      maxZoom: 19
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.placeMarker(e.latlng);
      this.reverseGeocode(e.latlng.lat, e.latlng.lng);
    });
    
    // Try to get current user location
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((position) => {
        if (this.map) {
          const latlng = L.latLng(position.coords.latitude, position.coords.longitude);
          this.map.setView(latlng, 15);
        }
      });
    }
  }

  private destroyMap(): void {
    if (this.map) {
      this.map.off();
      this.map.remove();
      this.map = null;
      this.marker = null;
    }
  }

  private placeMarker(latlng: L.LatLng): void {
    if (!this.map) return;
    
    if (this.marker) {
      this.marker.setLatLng(latlng);
    } else {
      this.marker = L.marker(latlng).addTo(this.map);
    }
    
    this.addPropertyForm.patchValue({
      latitude: latlng.lat,
      longitude: latlng.lng
    });
  }

  private reverseGeocode(lat: number, lng: number): void {
    this.isMapLoading.set(true);
    const url = `/nominatim/reverse?format=jsonv2&lat=${lat}&lon=${lng}`;
    
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.isMapLoading.set(false);
        if (res && res.address) {
          const addr = res.address;
          const street = addr.road || addr.pedestrian || addr.suburb || '';
          const city = addr.city || addr.town || addr.village || addr.county || '';
          const state = addr.state || '';
          const postCode = addr.postcode || '';
          
          let formatted = [];
          if (street) formatted.push(street);
          if (city) formatted.push(city);
          if (state) formatted.push(state);
          
          this.addPropertyForm.patchValue({
            address: formatted.join(', '),
            zipCode: postCode
          });
        }
      },
      error: (err) => {
        console.error('Reverse Geoding Error', err);
        this.isMapLoading.set(false);
      }
    });
  }

  searchAddress(query: string): void {
    this.searchSubject.next(query);
  }

  selectSearchResult(result: any): void {
    const lat = parseFloat(result.lat);
    const lng = parseFloat(result.lon);
    const latlng = L.latLng(lat, lng);

    if (this.map) {
      this.map.flyTo(latlng, 17, { animate: true, duration: 1 });
      this.placeMarker(latlng);
    }

    // Auto-fill address from display_name
    const addr = result.address || {};
    const street = addr.road || addr.pedestrian || addr.suburb || '';
    const city = addr.city || addr.town || addr.village || addr.county || '';
    const state = addr.state || '';
    const postCode = addr.postcode || '';

    let formatted: string[] = [];
    if (street) formatted.push(street);
    if (city) formatted.push(city);
    if (state) formatted.push(state);

    this.addPropertyForm.patchValue({
      address: formatted.join(', ') || result.display_name,
      zipCode: postCode,
      latitude: lat,
      longitude: lng
    });

    this.searchQuery.set(result.display_name);
    this.showSearchDropdown.set(false);
    this.searchResults.set([]);
  }

  closeSearchDropdown(): void {
    setTimeout(() => this.showSearchDropdown.set(false), 200);
  }

  /**
   * Called when user presses Enter in the search box.
   * If dropdown results are already available, picks the first one immediately.
   * Otherwise triggers an immediate search and waits for results.
   */
  selectFirstResult(): void {
    const results = this.searchResults();
    if (results && results.length > 0) {
      this.selectSearchResult(results[0]);
      return;
    }

    // No cached results yet — fire an immediate search bypassing debounce
    const query = this.searchQuery();
    if (!query || query.trim().length < 5) return;

    this.isSearchLoading.set(true);
    const url = `/nominatim/search?format=jsonv2&q=${encodeURIComponent(query)}&limit=1&addressdetails=1`;
    this.http.get<any[]>(url).subscribe({
      next: (res) => {
        this.isSearchLoading.set(false);
        if (res && res.length > 0) {
          this.selectSearchResult(res[0]);
        }
      },
      error: () => this.isSearchLoading.set(false)
    });
  }

  openEditModal(property: Property): void {
    this.editingProperty.set(property);
    this.showEditModal.set(true);
    this.editPropertyForm.patchValue({
      propertyType: property.propertyType,
      address: property.address,
      areaSqft: property.areaSqft,
      constructionType: property.constructionType
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingProperty.set(null);
    this.editPropertyForm.reset();
  }

  updateProperty(): void {
    if (this.editPropertyForm.invalid || !this.editingProperty()) {
      this.editPropertyForm.markAllAsTouched();
      return;
    }

    const propertyId = this.editingProperty()!.propertyId;
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.updateProperty(propertyId, this.editPropertyForm.value).subscribe({
      next: (updatedProperty) => {
        this.properties.update(props => 
          props.map(p => p.propertyId === propertyId ? updatedProperty : p)
        );
        this.successMessage.set('Property updated successfully!');
        this.isLoading.set(false);
        this.closeEditModal();
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to update property');
        console.error('Error updating property:', err);
        this.isLoading.set(false);
      }
    });
  }

  addProperty(): void {
    if (this.addPropertyForm.invalid) {
      this.addPropertyForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.addProperty(this.addPropertyForm.value).subscribe({
      next: (newProperty) => {
        this.properties.update(props => [...props, newProperty]);
        this.successMessage.set('Property added successfully!');
        this.isLoading.set(false);
        this.closeAddModal();
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to add property');
        console.error('Error adding property:', err);
        this.isLoading.set(false);
      }
    });
  }

  deleteProperty(propertyId: number): void {
    if (!confirm('Are you sure you want to delete this property?')) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.deleteProperty(propertyId).subscribe({
      next: () => {
        this.properties.update(props => props.filter(p => p.propertyId !== propertyId));
        this.successMessage.set('Property deleted successfully!');
        this.isLoading.set(false);
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to delete property');
        console.error('Error deleting property:', err);
        this.isLoading.set(false);
      }
    });
  }

  getStatusClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'PENDING': 'status-pending',
      'SCHEDULED': 'status-scheduled',
      'COMPLETED': 'status-completed',
      'APPROVED': 'status-approved',
      'REJECTED': 'status-rejected'
    };
    return statusClasses[status] || 'status-default';
  }

  getFieldError(fieldName: string): string {
    const field = this.addPropertyForm.get(fieldName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  getEditFieldError(fieldName: string): string {
    const field = this.editPropertyForm.get(fieldName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.addPropertyForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.addPropertyForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  isEditFieldInvalid(fieldName: string): boolean {
    const field = this.editPropertyForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isEditFieldValid(fieldName: string): boolean {
    const field = this.editPropertyForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }
}
