import { definePreset } from '@primeng/themes';
import Lara from '@primeng/themes/lara';

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
      xs: '0.25rem',
      sm: '0.5rem',
      md: '1rem',
      lg: '1.5rem',
      xl: '2rem'
    },

    // 👉 Richtig platzierte Typography-Tokens
    fontFamily: "'Inter', sans-serif",
    fontSize: {
      base: '1rem',
      sm: '0.875rem',
      lg: '1.125rem'
    },
    fontWeight: {
      normal: '400',
      bold: '600'
    },
    lineHeight: {
      base: '1.5'
    },

    borderRadius: {
      md: '0.25rem'
    }
  }
});

export { MyPreset };
