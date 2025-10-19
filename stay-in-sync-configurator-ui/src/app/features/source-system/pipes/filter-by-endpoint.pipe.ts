import { Pipe, PipeTransform } from '@angular/core';
import { AnyArc, ApiRequestConfiguration } from '../../script-editor/models/arc.models';

@Pipe({
  name: 'filterByEndpoint',
  standalone: true,
})
export class FilterByEndpointPipe implements PipeTransform {

  /**
   * Filters an array of ARCs to include only those matching a specific endpointId.
   * It safely handles a mixed array of AnyArc[] and only considers REST ARCs.
   *
   * @param arcs The array of mixed ARC types.
   * @param endpointId The ID of the endpoint to filter by.
   * @returns A new array containing only the REST ARCs that match the endpointId.
   */
  transform(arcs: AnyArc[] | null | undefined, endpointId: number): ApiRequestConfiguration[] {
    if (!arcs || !endpointId) {
      return [];
    }

    return arcs.filter(
      (arc): arc is ApiRequestConfiguration =>
        arc.arcType === 'REST' && arc.endpointId === endpointId
    );
  }
}
