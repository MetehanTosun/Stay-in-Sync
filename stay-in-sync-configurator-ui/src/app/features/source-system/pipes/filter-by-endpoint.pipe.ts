import { Pipe, PipeTransform } from '@angular/core';
import { ApiRequestConfiguration } from '../../script-editor/models/arc.models';

@Pipe({
  name: 'filterByEndpoint',
  standalone: true,
})
export class FilterByEndpointPipe implements PipeTransform {

  /**
   * Filters an array of ApiRequestConfiguration objects to only include
   * those that match the provided endpointId.
   *
   * @param arcs The array of ARCs to filter. Can be null or undefined.
   * @param endpointId The ID of the endpoint to filter by.
   * @returns A new array containing only the matching ARCs.
   */
  transform(arcs: ApiRequestConfiguration[] | null | undefined, endpointId: number): ApiRequestConfiguration[] {
    if (!arcs || !endpointId){
      return [];
    }

    return arcs.filter(arc => arc.endpointId === endpointId);
  }
}
