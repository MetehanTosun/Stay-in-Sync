// src/polyfills.ts

/***************************************************************************************************
 * BROWSER-POLYFILLS (wird vor App-Code geladen)
 *
 * 1) window.process           (damit util.js und JSON-Schema-Parser nicht meckern)
 * 2) window.util.inherits     (util.inherits wird von Swagger-Parser gebraucht)
 * 3) window.url.resolve/cwd   (json-schema-ref-parser benötigt url.resolve und url.cwd)
 **************************************************************************************************/

// --- 1) Polyfill für window.process ---
(window as any).process = {
    env: { DEBUG: undefined },
    versions: {},
    cwd: (): string => {
      // Im Browser muss nur ein String zurückgegeben werden; leer ist ok.
      return '';
    }
  };
  
  // --- 2) Polyfill für util.inherits ---
  // Wir verwenden require('util'), weil webpack das auf util-Polyfill umleitet (npm install util),
  // und TypeScript akzeptiert es dank "allowJs" in tsconfig.app.json.
  declare const require: any;
  const utilNode = require('util');
  (window as any).util = {
    inherits: utilNode.inherits
    // Falls du später andere util-Funktionen brauchst, kannst du sie hier ergänzen:
    // isBuffer: utilNode.isBuffer,
    // format: utilNode.format, ... etc.
  };
  
  // Polyfill für Buffer
  import { Buffer } from 'buffer';
  (window as any).Buffer = Buffer;
  
  // --- 3) Polyfill für url.resolve() und url.cwd() ---
  // Wir nutzen das Browser-Global `window.URL`, anstatt aus 'url' zu importieren,
  // weil die npm-Polyfill-Bibliothek "url" unter Umständen keine named export 'URL' bietet.
  //
  // json-schema-ref-parser ruft intern `url.resolve(from, to)` und `url.cwd()` auf.
  // Diesen Code hier fangen wir ab und implementieren es browserseitig.
  
  (window as any).url = {
    resolve: (from: string, to: string): string => {
      try {
        // Wir erstellen zwei neue URLs im Browser-Kontext:
        // - Basis: new window.URL(from, window.location.href)
        // - Target: new window.URL(to, Basis)
        const base = new window.URL(from, window.location.href);
        const resolved = new window.URL(to, base);
        if (resolved.protocol === 'file:') {
          // Bei file:-URLs evtl. Pfad-Normalisierung (z. B. Windows dp).
          let path = resolved.pathname;
          if (path.startsWith('/') && path.charAt(2) === ':') {
            path = path.substring(1);
          }
          return path;
        }
        return resolved.href;
      } catch {
        // Fallback, falls from keine valide URL ist (z. B. einfacher relativer Pfad):
        if (to.startsWith('/')) {
          return to;
        }
        let basePath = from.endsWith('/') ? from : from + '/';
        let normalizedTo = to.replace(/^\.\//, '');
        while (normalizedTo.startsWith('../')) {
          const idx = basePath.lastIndexOf('/', basePath.length - 2);
          basePath = idx >= 0 ? basePath.substring(0, idx + 1) : '';
          normalizedTo = normalizedTo.substring(3);
        }
        return basePath + normalizedTo;
      }
    },
    cwd: (): string => {
      // Gibt im Browser den Pfad der aktuellen Seite (ohne Datei) zurück:
      const path = window.location.pathname;
      return path.substring(0, path.lastIndexOf('/') + 1);
    }
  };
  
  /***************************************************************************************************
   * Standard-Angular-Polyfill: Zone.js
   **************************************************************************************************/
  import 'zone.js';