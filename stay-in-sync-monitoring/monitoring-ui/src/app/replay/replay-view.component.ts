// src/app/replay/replay-view.component.ts
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-replay-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './replay-view.component.html',
  styleUrl: './replay-view.component.css',
})
export class ReplayViewComponent {
  // tabs: 'source' | 'vars' | 'term'
  activeTab: 'source' | 'vars' | 'term' = 'source';

  // demo script string (read-only box)
  demoScript = `// read-only demo
function transform(source) {
  const userName = source.user?.name ?? 'unknown';
  if (!source.system) throw new Error('system missing');
  return { ok: true, userName };
}`;

  // sample data to render
  sourceData = {
    system: 'DemoSystem',
    user: { id: '42', name: 'Matthias' },
  };

  variableSnapshot = {
    userName: 'Matthias',
    system: 'DemoSystem',
    ok: false,
    errorLine: 3,
  };

  terminalLines = [
    'PolyglotException: Error: system missing',
    ' at transform (line 3, column 10)',
  ];

  setTab(tab: 'source' | 'vars' | 'term') {
    this.activeTab = tab;
  }
}
