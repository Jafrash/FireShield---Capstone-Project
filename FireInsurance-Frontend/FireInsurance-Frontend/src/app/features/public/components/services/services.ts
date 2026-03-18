import { Component } from '@angular/core';

@Component({
  selector: 'app-services',
  standalone: true,
  templateUrl: './services.html'
})
export class ServicesComponent {
  services = [
    { title: 'Property Insurance', desc: 'Secure your residential or commercial property from fire damage.', icon: 'house' },
    { title: 'Risk Inspection', desc: 'Expert surveyors assess property risks to minimize potential hazards.', icon: 'search' },
    { title: 'Claim Management', desc: 'Seamlessly submit, track, and process your insurance claims online.', icon: 'assignment' },
    { title: 'Damage Assessment', desc: 'Accurate inspections to quickly determine loss and speed up payouts.', icon: 'analytics' }
  ];
}
