import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({ name: 'safeUrl' })
export class SafeUrlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  /**
   * Transforms a string URL into a SafeResourceUrl.
   * This bypasses Angular's built-in security to allow embedding
   * trusted resource URLs (e.g., iframes, media sources).
   *
   * ⚠️ Only use with trusted URLs to avoid security risks.
   *
   * @param url The raw URL string.
   * @returns A SafeResourceUrl that Angular considers safe to bind.
   */
  transform(url: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
