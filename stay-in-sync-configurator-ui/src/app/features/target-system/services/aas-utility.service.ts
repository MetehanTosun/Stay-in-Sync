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
    
    // If aasId is explicitly set, use it
    if (system.aasId && system.aasId.trim() !== '') {
      return system.aasId;
    }
    
    // Fallback: try to extract from API URL or use a default
    if (system.apiUrl) {
      try {
        const url = new URL(system.apiUrl);
        
        // For localhost, try to extract AAS ID from path or use system name
        if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
          // Try to extract AAS ID from path (e.g., /aas/12345/...)
          const pathMatch = url.pathname.match(/\/aas\/([^\/]+)/);
          if (pathMatch && pathMatch[1]) {
            return pathMatch[1];
          }
          
          // Try to extract from query parameters
          const aasIdParam = url.searchParams.get('aasId') || url.searchParams.get('id');
          if (aasIdParam) {
            return aasIdParam;
          }
          
          // No AAS ID found in URL
          return 'AAS ID not found';
        }
        
        // For other hosts, use hostname as AAS ID
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
