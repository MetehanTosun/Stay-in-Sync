import { Pipe, PipeTransform } from '@angular/core';
import { EndpointSuggestion } from '../features/script-editor/models/target-system.models';

@Pipe({
  name: 'filterByHttpMethod',
  standalone: true,
})
export class FilterByHttpMethodPipe implements PipeTransform {
  transform(
    suggestions: EndpointSuggestion[] | null,
    method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  ): EndpointSuggestion[] {
    if (!suggestions) {
      return [];
    }
    return suggestions.filter(s => s.httpRequestType.toUpperCase() === method);
  }
}
