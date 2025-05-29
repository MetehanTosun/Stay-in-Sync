// biome-ignore lint/style/useImportType: <explanation>
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
// biome-ignore lint/style/useImportType: <explanation>
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

export interface AasShell {
  identification: any;
  idShort: string;
  id: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class AasService {
  private baseUrl = 'http://localhost:4001/aasServer'; // wird per Proxy auf localhost:4001 geleitet

  constructor(private http: HttpClient) {
    console.log(`🔧 AasService initialized, baseUrl = ${this.baseUrl}`);
  }

  /** Holt alle registrierten AAS-Instanzen */
  getAll(): Observable<AasShell[]> {
    const url = `${this.baseUrl}/shells`;
    console.log(`👉 AasService.getAll() → GET ${url}`);

    return this.http
      // observe: 'response' gibt uns das gesamte HttpResponse-Objekt
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
            .get<any[]>(url, { observe: 'response' })
      .pipe(
        tap((resp: HttpResponse<any[]>) =>
          console.log('📰 Vollständige Response:', resp)
        ),
        map(resp => resp.body || []),
        tap(raw => console.log('🌱 Rohdaten:', raw)),
        map(rawArray =>
          rawArray.map(item => ({
            id:   item.identification?.id   || item.idShort,
            name: item.idShort
          } as AasShell))
        ),
        tap(mapped => console.log('🌿 Gemappt:', mapped)),
        catchError(err => {
          // Nur echte Fehler (Status ≠ 2xx) landen hier
          console.error('⛔ AasService.getAll() failed:', err);
          return throwError(() => err);
        })
      );
  }
}