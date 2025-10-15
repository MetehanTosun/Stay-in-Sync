import { Injectable } from '@angular/core';
import { TargetSystemDTO } from '../models/targetSystemDTO';

@Injectable({
  providedIn: 'root'
})
export class AasUtilityService {

  constructor() {}

  /**
   * Check if system is AAS type
   */
  isAasSystem(system: TargetSystemDTO | null): boolean {
    return system?.apiType === 'AAS';
  }

  /**
   * Get AAS ID with fallback logic
   */
  getAasId(system: TargetSystemDTO | null): string {
    if (!system) return '-';
    
    if (system.aasId && system.aasId.trim() !== '') {
      return system.aasId;
    }
    
    if (system.apiUrl) {
      try {
        const url = new URL(system.apiUrl);
        
        if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
          const pathMatch = url.pathname.match(/\/aas\/([^\/]+)/);
          if (pathMatch && pathMatch[1]) {
            return pathMatch[1];
          }
          
          const aasIdParam = url.searchParams.get('aasId') || url.searchParams.get('id');
          if (aasIdParam) {
            return aasIdParam;
          }
          
          return 'AAS ID not found';
        }
        
        return url.hostname;
      } catch (e) {
        return 'AAS ID not found';
      }
    }
    
    return '-';
  }

  /**
   * Get parent path from element path
   */
  getParentPath(idShortPath: string): string {
    return idShortPath.includes('/') ? idShortPath.substring(0, idShortPath.lastIndexOf('/')) : '';
  }

  /**
   * Encode ID to Base64
   */
  encodeIdToBase64(id: string): string {
    return btoa(id);
  }
}
