import { Component } from '@angular/core';
import { HeroComponent } from '../../components/hero/hero';
import { ServicesComponent } from '../../components/services/services';
import { CoverageComponent } from '../../components/coverage/coverage';
import { PublicNavbarComponent } from '../../../../shared/components/navigation/public-navbar/public-navbar';
import { FooterComponent } from '../../../../shared/components/layout/footer/footer';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    PublicNavbarComponent,
    HeroComponent,
    ServicesComponent,
    CoverageComponent,
    FooterComponent
  ],
  templateUrl: './home.html',
})
export class HomeComponent {}
