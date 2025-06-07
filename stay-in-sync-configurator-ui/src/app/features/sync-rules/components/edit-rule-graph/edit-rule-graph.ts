import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-edit-rule-graph',
  imports: [],
  templateUrl: './edit-rule-graph.html',
  styleUrl: './edit-rule-graph.css'
})

export class EditRuleGraph {
  constructor(private router: Router) {}

  return() {
    this.router.navigate(['/']);
  }
}
