/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Design tokens from the ui-ux-pro-max "Vibrant & Block-based" system.
        primary: '#EC4899',
        'primary-foreground': '#FFFFFF',
        secondary: '#DB2777',
        accent: '#2563EB',
        background: '#0F172A',
        surface: '#151C31',
        muted: '#201A32',
        'muted-foreground': '#94A3B8',
        foreground: '#FFFFFF',
        border: 'rgba(255,255,255,0.08)',
        destructive: '#DC2626',
        success: '#22C55E',
      },
      fontFamily: {
        sans: ['"Fira Sans"', 'system-ui', 'sans-serif'],
        mono: ['"Fira Code"', 'ui-monospace', 'monospace'],
      },
      boxShadow: {
        glow: '0 0 40px -10px rgba(236,72,153,0.45)',
        card: '0 10px 30px -12px rgba(0,0,0,0.6)',
      },
      borderRadius: {
        xl: '1rem',
        '2xl': '1.5rem',
      },
    },
  },
  plugins: [],
};
