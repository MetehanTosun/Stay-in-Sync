import { definePreset } from '@primeng/themes';
import Lara from '@primeng/themes/lara';
import Aura from '@primeng/themes/aura';

const MyPreset = definePreset(Lara, {
  semantic: {
    primary: {
      50: '#e0f0ff',
      100: '#b3d8ff',
      200: '#80c0ff',
      300: '#4da8ff',
      400: '#2696ff',
      500: '#66b2ff',
      600: '#1a86ff',
      700: '#0066cc',
      800: '#0052a3',
      900: '#003d7a',
      950: '#002d5c'
    },
    success: {
      500: '#28a745'
    },
    error: {
      500: '#dc3545'
    }
  },
  tokens: {
    colors: {
      surface: '#ffffff',
      background: '#ffffff',
      text: {
        primary: '#212529',
        secondary: '#495057'
      }
    },
    spacing: {
      xs: '0.25rem',   // 4px
      sm: '0.5rem',    // 8px
      md: '1rem',      // 16px
      lg: '1.5rem',    // 24px
      xl: '2rem'       // 32px
    },
    typography: {
      fontFamily: "'Inter', sans-serif",
      fontSize: {
        base: '1rem',    // 16px
        sm: '0.875rem',  // 14px
        lg: '1.125rem'   // 18px
      },
      fontWeight: {
        normal: '400',
        bold: '600'
      },
      lineHeight: {
        base: '1.5'
      }
    },
    borderRadius: {
      md: '0.25rem' // 4px
    }
  }
});

export { MyPreset };
