import { Component } from '@angular/core';
import { Location } from '@angular/common';

@Component({
  selector: 'app-edit-rule',
  imports: [],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.css'
})
export class EditRuleComponent {
  constructor(private location: Location) { }

  return() {
    this.location.back();
  }
}
