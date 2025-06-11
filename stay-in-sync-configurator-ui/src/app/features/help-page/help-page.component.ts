/**
 * @fileoverview HelpPageComponent - Angular-Komponente für die Anzeige von Hilfeseiten.
 * Diese Komponente lädt Markdown-Dateien basierend auf der Route und zeigt ein Panel-Menü
 * sowie eine Liste von Überschriften an, die aus der Markdown-Datei extrahiert werden.
 */

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {MenuItem} from 'primeng/api';
import {PanelMenu} from 'primeng/panelmenu';
import {MarkdownComponent} from 'ngx-markdown';
import {Button} from 'primeng/button';
import {HttpClient} from '@angular/common/http';
import {Listbox, ListboxChangeEvent} from 'primeng/listbox';
import {NgIf} from '@angular/common';

/**
 * @class HelpPageComponent
 * @description Diese Komponente verwaltet die Anzeige von Hilfeseiten und deren Navigation.
 */
@Component({
  selector: 'app-help-page',
  templateUrl: './help-page.component.html',
  imports: [
    PanelMenu,
    MarkdownComponent,
    NgIf,
    Button,
    Listbox,
  ],
  styleUrl: './help-page.component.css'
})
export class HelpPageComponent implements OnInit {
  /** @property {string} markdownPath - Pfad zur aktuellen Markdown-Datei. */
  markdownPath = '';

  /** @property {MenuItem[]} items - Menüeinträge für das Panel-Menü. */
  items: MenuItem[] = [];

  /** @property {{ id: string; text: string }[]} headings - Liste der Überschriften aus der Markdown-Datei. */
  headings: { id: string; text: string }[] = [];

  /**
   * @constructor
   * @param {ActivatedRoute} route - Aktivierte Route für die Navigation.
   * @param {Router} router - Router für die Navigation.
   * @param {HttpClient} http - HTTP-Client für das Laden von Markdown-Dateien.
   */
  constructor(private route: ActivatedRoute, private router: Router, private http: HttpClient) {}

  /**
   * Navigiert zurück zur Hauptseite der Hilfe und entfernt die Anzeige der Markdown-Datei.
   */
  goBack(): void {
    this.markdownPath = ''; // Kein Markdown-File anzeigen
    this.router.navigate(['/help']); // Nur das PanelMenu anzeigen
  }

  /**
   * Lädt die Überschriften aus einer Markdown-Datei und setzt deren IDs für die Navigation.
   * @param {string} path - Pfad zur Markdown-Datei.
   */
  loadHeadings(path: string): void {
    this.http.get(path, { responseType: 'text' }).subscribe((md: string) => {
      this.headings = [];
      const lines = md.split('\n');
      for (const line of lines) {
        const match = line.match(/^##\s+(.*)/);
        if (match) {
          const text = match[1].trim();
          const id = text
            .toLowerCase()
            .replace(/[^\w äöüÄÖÜ\-ß]+/g, '') // Sonderzeichen entfernen
            .replace(/\s+/g, '-');
          this.headings.push({ id, text });
        }
      }
      console.log(`Headings loaded from ${path}:`, this.headings);

      // Warte, bis die `markdown`-Komponente vollständig gerendert ist
      setTimeout(() => {
        this.headings.forEach(heading => {
          const element = Array.from(document.querySelectorAll('markdown h2')).find(
            el => el.textContent?.trim() === heading.text
          );
          if (element) {
            element.id = heading.id; // Setze das `id`-Attribut
            console.log(`ID ${heading.id} erfolgreich zugewiesen.`);
          } else {
            console.warn(`Element für Heading ${heading.text} nicht gefunden.`);
          }
        });
      }, 500); // Wartezeit für das Rendern
    });
  }

  /**
   * Event-Handler für die Auswahl einer Überschrift aus der Listbox.
   * Scrollt zur ausgewählten Überschrift im DOM.
   * @param {ListboxChangeEvent} $event - Event-Objekt der Listbox.
   */
  onHeadingSelect($event: ListboxChangeEvent) {
    const selectedHeading = $event.value;
    if (selectedHeading) {
      console.log(`Selected heading: ${selectedHeading.text} with id: ${selectedHeading.id}`);
      const element = document.getElementById(selectedHeading.id);
      if (element) {
        console.log(`Scrolling to heading: ${selectedHeading.text} with id: ${selectedHeading.id}`);
        element.scrollIntoView({ behavior: 'smooth' });
      } else {
        console.warn(`Element with id ${selectedHeading.id} not found in DOM.`);
      }
    }
  }

  /**
   * Initialisiert die Komponente und lädt die Menüeinträge sowie die Markdown-Datei basierend auf der Route.
   */
  ngOnInit(): void {
    this.route.paramMap.subscribe(paramMap => {
      const topic = paramMap.get('topic');
      console.log(`Current topic from route: ${topic}`);
      if (topic) {
        this.markdownPath = `assets/docs/${topic}.md`;
      } else {
        this.markdownPath = ''; // Kein Markdown anzeigen
      }
      console.log(`Markdown path set to: ${this.markdownPath}`);
      if (this.markdownPath) {
        this.loadHeadings(this.markdownPath);
      } else {
        this.headings = []; // Leere Liste, wenn kein Markdown angezeigt wird
      }
    });

    this.items = [
      {
        label: 'Einführung',
        icon: 'pi pi-home',
        command: () => { this.router.navigate(['/help', 'index']); }
      },
      {
        label: 'Guides',
        icon: 'pi pi-book',
        items: [
          {
            label: 'Getting Started',
            command: () => { this.router.navigate(['/help', 'getting-started']); }
          },
          {
            label: 'Features',
            command: () => { this.router.navigate(['/help', 'features']); }
          },
          {
            label: 'Authentication',
            command: () => { this.router.navigate(['/help', 'auth-guide']); }
          }
        ]
      }
    ];
  }
}
