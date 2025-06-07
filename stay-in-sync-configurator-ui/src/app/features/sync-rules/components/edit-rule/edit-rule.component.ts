import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-edit-rule',
  imports: [],
  templateUrl: './edit-rule.html',
  styleUrl: './edit-rule.css'
})

export class EditRuleComponent {
  constructor(private router: Router) {}

  return() {
    this.router.navigate(['/']);
  }
}
