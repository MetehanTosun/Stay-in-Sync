import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-search-bar',
  imports: [],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.css'
})
export class SearchBarComponent {
  @Output() search = new EventEmitter<string>();

  onInput(event: Event) {
    console.log('Search input changed:', event);
    const value = (event.target as HTMLInputElement).value;
    this.search.emit(value);
  }

  onSearch(value: string) {
    console.log('Search button clicked:', value);
    this.search.emit(value);
  }
}
