/**
 * HelpPageComponent
 *
 * This Angular component represents the help page of the application. It provides navigation
 * through various help topics using a menu and displays markdown content dynamically based on
 * the selected topic. It also allows users to scroll to specific headings within the markdown content.
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
  /** Path to the markdown file to be displayed */
  markdownPath = '';

  /** Menu items for the help topics */
  items: MenuItem[] = [];

  /** List of headings extracted from the markdown file */
  headings: { id: string; text: string }[] = [];

  /**
   * Constructor
   * @param route - ActivatedRoute for accessing route parameters
   * @param router - Router for navigation
   * @param http - HttpClient for making HTTP requests
   */
  constructor(private route: ActivatedRoute, private router: Router, private http: HttpClient) {}

  /**
   * Navigates back to the main help page, hiding the markdown content.
   */
  goBack(): void {
    this.markdownPath = ''; // Clear the markdown file path
    this.router.navigate(['/help']); // Navigate to the help page
  }

  /**
   * Loads headings from the specified markdown file and assigns IDs to them.
   * @param path - Path to the markdown file
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
            .replace(/[^\w äöüÄÖÜ\-ß]+/g, '') // Remove special characters
            .replace(/\s+/g, '-'); // Replace spaces with hyphens
          this.headings.push({ id, text });
        }
      }
      console.log(`Headings loaded from ${path}:`, this.headings);

      // Wait for the markdown component to render completely
      setTimeout(() => {
        this.headings.forEach(heading => {
          const element = Array.from(document.querySelectorAll('markdown h2')).find(
            el => el.textContent?.trim() === heading.text
          );
          if (element) {
            element.id = heading.id; // Assign the ID attribute
            console.log(`ID ${heading.id} successfully assigned.`);
          } else {
            console.warn(`Element for heading ${heading.text} not found.`);
          }
        });
      }, 500); // Wait time for rendering
    });
  }

  /**
   * Handles the selection of a heading from the listbox and scrolls to the corresponding element.
   * @param $event - ListboxChangeEvent containing the selected heading
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
   * Lifecycle hook that initializes the component.
   * Subscribes to route parameters and loads the corresponding markdown file and headings.
   */
  ngOnInit(): void {
    this.route.paramMap.subscribe(paramMap => {
      const topic = paramMap.get('topic');
      console.log(`Current topic from route: ${topic}`);
      if (topic) {
        this.markdownPath = `assets/docs/${topic}.md`;
      } else {
        this.markdownPath = ''; // Clear markdown path if no topic is provided
      }
      console.log(`Markdown path set to: ${this.markdownPath}`);
      if (this.markdownPath) {
        this.loadHeadings(this.markdownPath);
      } else {
        this.headings = []; // Clear headings if no markdown is displayed
      }
    });

    // Initialize menu items for navigation
    this.items = [
      {
        label: 'Introduction',
        icon: 'pi pi-home',
        command: () => { this.router.navigate(['/help', 'introduction']); }
      },
      {
        label: 'Guides',
        icon: 'pi pi-book',
        items: [
          {
            label: 'Getting Started',
            command: () => { this.router.navigate(['/help', 'guides/getting-started']); }
          },
          {
            label: 'Configuration Management',
            command: () => { this.router.navigate(['/help', 'guides/configuration']); }
          },
          {
            label: 'Working with Sync Rules',
            command: () => { this.router.navigate(['/help', 'guides/sync-rules']); }
          },
          {
            label: 'Using Transformation Scripts',
            command: () => { this.router.navigate(['/help', 'guides/transformation-scripts']); }
          },
          {
            label: 'Managing Sync Jobs',
            command: () => { this.router.navigate(['/help', 'guides/sync-jobs']); }
          }
        ]
      },
      {
        label: 'System Management',
        icon: 'pi pi-server',
        items: [
          {
            label: 'Source System Management',
            command: () => { this.router.navigate(['/help', 'system-management/source-systems']); }
          },
          {
            label: 'EDC Integration',
            command: () => { this.router.navigate(['/help', 'system-management/edc-management']); }
          },
          {
            label: 'AAS Integration',
            command: () => { this.router.navigate(['/help', 'system-management/aas-management']); }
          }
        ]
      },
      {
        label: 'Features',
        icon: 'pi pi-cog',
        items: [
          {
            label: 'Authentication & Interfaces',
            command: () => { this.router.navigate(['/help', 'features/auth-and-interfaces']); }
          },
          {
            label: 'Sync Logic & Conditions',
            command: () => { this.router.navigate(['/help', 'features/sync-logic']); }
          },
          {
            label: 'Transformation Engine',
            command: () => { this.router.navigate(['/help', 'features/transformation-engine']); }
          },
          {
            label: 'Error Handling & Logging',
            command: () => { this.router.navigate(['/help', 'features/error-logging']); }
          },
          {
            label: 'Scaling & Modes',
            command: () => { this.router.navigate(['/help', 'features/scaling-modes']); }
          }
        ]
      },
      {
        label: 'Monitoring',
        icon: 'pi pi-chart-line',
        items: [
          {
            label: 'Graph View & Metrics',
            command: () => { this.router.navigate(['/help', 'monitoring/monitoring-graph']); }
          },
          {
            label: 'Log Aggregation & Filtering',
            command: () => { this.router.navigate(['/help', 'monitoring/monitoring-logs']); }
          },
          {
            label: 'ErrorSnapshots & Replay',
            command: () => { this.router.navigate(['/help', 'monitoring/monitoring-replay']); }
          }
        ]
      },
      {
        label: 'Testing & Deployment',
        icon: 'pi pi-send',
        items: [
          {
            label: 'Test Scenarios',
            command: () => { this.router.navigate(['/help', 'deployment/testing']); }
          },
          {
            label: 'Deployment Options',
            command: () => { this.router.navigate(['/help', 'deployment/deployment']); }
          }
        ]
      },
      {
        label: 'Developer Reference',
        icon: 'pi pi-code',
        items: [
          {
            label: 'Technical Details',
            command: () => { this.router.navigate(['/help', 'technical-details']); }
          },
          {
            label: 'Developer Guide',
            command: () => { this.router.navigate(['/help', 'developer-guide']); }
          }
        ]
      }
    ];
  }
}
