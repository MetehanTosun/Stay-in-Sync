import { Pipe, PipeTransform } from '@angular/core';
import { AnyArc } from '../../script-editor/models/arc.models';

@Pipe({
  name: 'filterByType',
  standalone: true
})
export class FilterByTypePipe implements PipeTransform {

  /**
   * Filters an array of ARCs (AnyArc[]) to include only those of a specific type.
   *
   * @param arcs The array of mixed ARC types (ApiRequestConfiguration | AasArc).
   * @param type The type to filter by, either 'REST' or 'AAS'.
   * @returns A new array containing only the ARCs that match the specified type.
   */
  transform(arcs: AnyArc[] | null | undefined, type: 'REST' | 'AAS'): AnyArc[] {
    if (!arcs) {
      return [];
    }

    return arcs.filter(arc => arc.arcType === type);
  }

}
