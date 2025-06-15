import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, PLATFORM_ID, signal } from '@angular/core';
import { $t } from '@primeng/themes';
import Aura from '@primeng/themes/aura';
import Lara from '@primeng/themes/lara';
import Material from '@primeng/themes/material';
import Nora from '@primeng/themes/nora';
import { PrimeNG } from 'primeng/config';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';

// Define a type for the preset names based on the keys of the presets object
const presets = {
  Aura,
  Material,
  Lara,
  Nora
} as const; // Use 'as const' for stricter typing of keys and values

// Derive a type for preset names from the keys of the presets object
type PresetName = keyof typeof presets;

export interface ThemeState {
  preset: PresetName; // Make preset non-optional and use the PresetName type
  darkTheme: boolean; // Make darkTheme non-optional
}

@Component({
  selector: 'theme-switcher',
  standalone: true,
  imports: [CommonModule],
  template: ` <div class="card flex justify-end p-2 mb-4">
    <ul class="flex list-none m-0 p-0 gap-2 items-center">
      <li>
        <button type="button" class="inline-flex w-8 h-8 p-0 items-center justify-center surface-0 dark:surface-800 border border-surface-200 dark:border-surface-600 rounded" (click)="onThemeToggler()" [title]="themeState().darkTheme ? 'Activate light mode' : 'Activate dark mode'">
          <i [ngClass]="'pi ' + iconClass()" class="dark:text-white"></i>
        </button>
      </li>
    </ul>
  </div>`
})
export class ThemeSwitcher {
  private readonly STORAGE_KEY = 'themeSwitcherState';

  document = inject(DOCUMENT);
  platformId = inject(PLATFORM_ID);
  config: PrimeNG = inject(PrimeNG);

  // Initialize the signal directly with a valid ThemeState object
  themeState = signal<ThemeState>(this.loadthemeState());

  iconClass = computed(() =>
    this.themeState().darkTheme ? 'pi-sun' : 'pi-moon'
  );

  constructor() {
    effect(() => {
      const state = this.themeState();
      console.log('Effect running with state:', state); // Debug
      this.savethemeState(state);
      this.handleDarkModeTransition(state);
    });
  }


  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      // The preset is already loaded and applied via the constructor's signal initialization
      // and the effect. However, explicitly applying it here ensures
      // that if the effect somehow runs before ngOnInit (unlikely but for safety)
      // or if there are other initial setup needs for the preset, they are met.
      // The themeState().preset will always be a valid PresetName here.
      this.applyPreset(this.themeState().preset);
    }
  }

  onThemeToggler() {
    console.log('onThemeToggler called'); // Debug
    this.themeState.update((state) => {
      const newState = { ...state, darkTheme: !state.darkTheme };
      console.log('Previous state:', state); // Debug
      console.log('New state:', newState); // Debug
      return newState;
    });
  }


  startViewTransition(state: ThemeState): void {
    const transition = (this.document as any).startViewTransition?.(() => {
      this.toggleDarkMode(state);
    });

    if (transition) {
      transition.ready.then(() => this.onTransitionEnd());
    } else {
      this.toggleDarkMode(state);
      this.onTransitionEnd();
    }
  }

  toggleDarkMode(state: ThemeState): void {
    console.log('toggleDarkMode called. darkTheme:', state.darkTheme); // Debug
    if (state.darkTheme) {
      console.log('Adding p-dark class'); // Debug
      this.document.documentElement.classList.add('p-dark');
    } else {
      console.log('Removing p-dark class'); // Debug
      this.document.documentElement.classList.remove('p-dark');
    }
    console.log('documentElement classes:', this.document.documentElement.className); // Debug
  }


  onTransitionEnd() {
    // This method can be kept for future use or removed if truly empty
  }

  handleDarkModeTransition(state: ThemeState): void {
    console.log('handleDarkModeTransition called with state:', state); // Debug
    if (isPlatformBrowser(this.platformId)) {
      if ((this.document as any).startViewTransition) {
        console.log('Using startViewTransition'); // Debug
        this.startViewTransition(state);
      } else {
        console.log('Using toggleDarkMode directly'); // Debug
        this.toggleDarkMode(state);
        this.onTransitionEnd();
      }
    }
  }


  private applyPreset(presetName: PresetName) { // Use PresetName type
    // No need to check if presetConfig is undefined if presetName is always a valid key
    // const presetConfig = presets[presetName];

    if (presetName === 'Material') {
      this.document.body.classList.add('material');
      this.config.ripple.set(true);
    } else {
      this.document.body.classList.remove('material');
      this.config.ripple.set(false);
    }

    $t()
      .preset(presets[presetName]) // This is now type-safe
      .use({ useDefaultOptions: true });
  }

  loadthemeState(): ThemeState {
    let loadedPreset: PresetName = 'Aura'; // Default preset
    let loadedDarkTheme: boolean = false;

    if (isPlatformBrowser(this.platformId)) {
      const storedState = localStorage.getItem(this.STORAGE_KEY);
      if (storedState) {
        try {
          const parsedState = JSON.parse(storedState);
          // Validate the loaded preset
          if (parsedState.preset && presets.hasOwnProperty(parsedState.preset)) {
            loadedPreset = parsedState.preset as PresetName;
          }
          if (typeof parsedState.darkTheme === 'boolean') {
            loadedDarkTheme = parsedState.darkTheme;
          }
        } catch (e) {
          console.error("ThemeSwitcher: Error parsing stored theme state.", e);
          // Stick to defaults if parsing fails
        }
      }
    }
    return {
      preset: loadedPreset,
      darkTheme: loadedDarkTheme,
    };
  }

  savethemeState(state: ThemeState): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(state));
    }
  }
}
