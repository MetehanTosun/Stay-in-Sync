import { Pipe, PipeTransform } from '@angular/core';
import { AasArc, AnyArc } from '../../script-editor/models/arc.models';

@Pipe({
  name: 'filterBySubmodel',
  standalone: true,
})
export class FilterBySubmodelPipe implements PipeTransform {

  /**
   * Filters an array of ARCs to include only those matching a specific submodelId.
   * It safely handles a mixed array of AnyArc[] and only considers AAS ARCs.
   *
   * @param arcs The array of mixed ARC types.
   * @param submodelId The ID of the AasSubmodelLite entity to filter by.
   * @returns A new array containing only the AAS ARCs that match the submodelId.
   */
  transform(arcs: AnyArc[] | null | undefined, submodelId: number): AasArc[] {
    if (!arcs || !submodelId) {
      return [];
    }

    return arcs.filter(
      (arc): arc is AasArc =>
        arc.arcType === 'AAS' && arc.submodelId === submodelId
    );
  }

}
