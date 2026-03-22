/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      // 🔥 FIRE THEME COLOR SYSTEM
      colors: {
        // Primary Fire Palette
        'fire-red': {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#C72B32', // Primary fire-red
          600: '#b91c1c',
          700: '#991b1b',
          800: '#7f1d1d',
          900: '#701a1a',
        },
        'fire-orange': {
          50: '#fff7ed',
          100: '#ffedd5',
          200: '#fed7aa',
          300: '#fdba74',
          400: '#fb923c',
          500: '#FF6B35', // Primary fire-orange
          600: '#ea580c',
          700: '#c2410c',
          800: '#9a3412',
          900: '#7c2d12',
        },
        'fire-yellow': {
          50: '#fefce8',
          100: '#fef9c3',
          200: '#fef08a',
          300: '#fde047',
          400: '#FFDD44', // Primary fire-yellow
          500: '#eab308',
          600: '#ca8a04',
          700: '#a16207',
          800: '#854d0e',
          900: '#713f12',
        },
        'fire-ember': {
          50: '#fdf4f3',
          100: '#fce7e6',
          200: '#f9d3d1',
          300: '#f4b2ae',
          400: '#E2725B', // Primary fire-ember
          500: '#dc2626',
          600: '#b91c1c',
          700: '#991b1b',
          800: '#7f1d1d',
          900: '#701a1a',
        },
        'fire-charcoal': {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#2C2E39', // Primary fire-charcoal
          900: '#0f172a',
        },
        'fire-cream': {
          50: '#F2F0EA', // Primary fire-cream
          100: '#f7fafc',
          200: '#edf2f7',
          300: '#e2e8f0',
          400: '#cbd5e1',
          500: '#a0aec0',
          600: '#718096',
          700: '#4a5568',
          800: '#2d3748',
          900: '#1a202c',
        },

        // Functional Color Mappings for Systematic Replacement
        primary: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#C72B32', // fire-red
          600: '#b91c1c',
          700: '#991b1b',
          800: '#7f1d1d',
          900: '#701a1a',
        },
        secondary: {
          50: '#fff7ed',
          100: '#ffedd5',
          200: '#fed7aa',
          300: '#fdba74',
          400: '#fb923c',
          500: '#FF6B35', // fire-orange
          600: '#ea580c',
          700: '#c2410c',
          800: '#9a3412',
          900: '#7c2d12',
        },
        accent: {
          50: '#fefce8',
          100: '#fef9c3',
          200: '#fef08a',
          300: '#fde047',
          400: '#FFDD44', // fire-yellow
          500: '#eab308',
          600: '#ca8a04',
          700: '#a16207',
          800: '#854d0e',
          900: '#713f12',
        },

        // Status Colors with Fire Theme
        success: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          300: '#86efac',
          400: '#4ade80',
          500: '#22c55e',
          600: '#16a34a',
          700: '#15803d',
          800: '#166534',
          900: '#14532d',
        },
        warning: {
          50: '#fefce8',
          100: '#fef9c3',
          200: '#fef08a',
          300: '#fde047',
          400: '#FFDD44', // fire-yellow for warnings
          500: '#eab308',
          600: '#ca8a04',
          700: '#a16207',
          800: '#854d0e',
          900: '#713f12',
        },
        danger: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#C72B32', // fire-red for danger
          600: '#b91c1c',
          700: '#991b1b',
          800: '#7f1d1d',
          900: '#701a1a',
        },
        info: {
          50: '#fff7ed',
          100: '#ffedd5',
          200: '#fed7aa',
          300: '#fdba74',
          400: '#fb923c',
          500: '#FF6B35', // fire-orange for info
          600: '#ea580c',
          700: '#c2410c',
          800: '#9a3412',
          900: '#7c2d12',
        },

        // Warm Gray System (replacing cold grays)
        warmgray: {
          50: '#f5f6fa', // warmgray from styles.css
          100: '#f7f8fc',
          200: '#e3e6ef',
          300: '#c8cdd9',
          400: '#9ca3b3',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#2C2E39', // fire-charcoal
          900: '#111827',
        },
      },

      // 🎨 BACKGROUND GRADIENTS
      backgroundImage: {
        'fire-gradient': 'linear-gradient(135deg, #C72B32 0%, #FF6B35 100%)',
        'fire-gradient-reverse': 'linear-gradient(135deg, #FF6B35 0%, #C72B32 100%)',
        'fire-glow': 'radial-gradient(circle at center, #C72B32 0%, #FF6B35 50%, transparent 100%)',
        'ember-gradient': 'linear-gradient(135deg, #E2725B 0%, #FFDD44 100%)',
      },

      // 🎯 FOCUS RING SYSTEM
      ringColor: {
        DEFAULT: '#C72B32', // fire-red focus rings
        'fire': '#C72B32',
        'ember': '#E2725B',
      },

      // 📦 BOX SHADOWS WITH FIRE THEME
      boxShadow: {
        'fire': '0 10px 25px -5px rgba(199, 43, 50, 0.1), 0 4px 6px -2px rgba(199, 43, 50, 0.05)',
        'fire-lg': '0 25px 50px -12px rgba(199, 43, 50, 0.25)',
        'ember': '0 10px 25px -5px rgba(226, 114, 91, 0.1), 0 4px 6px -2px rgba(226, 114, 91, 0.05)',
      },

      // 🌟 ANIMATIONS AND TRANSITIONS
      animation: {
        'fire-pulse': 'fire-pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'ember-glow': 'ember-glow 3s ease-in-out infinite',
      },

      keyframes: {
        'fire-pulse': {
          '0%, 100%': {
            opacity: '1',
          },
          '50%': {
            opacity: '.5',
          }
        },
        'ember-glow': {
          '0%, 100%': {
            boxShadow: '0 0 20px rgba(226, 114, 91, 0.5)',
          },
          '50%': {
            boxShadow: '0 0 40px rgba(226, 114, 91, 0.8), 0 0 60px rgba(199, 43, 50, 0.3)',
          }
        }
      },

      // 📏 SPACING FOR FIRE-THEMED LAYOUTS
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
      },

      // 🔤 FONT ENHANCEMENTS
      fontFamily: {
        'fire': ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [
    // Fire Theme Plugin for Component Classes
    function({ addComponents, theme }) {
      addComponents({
        // 🔴 PRIMARY BUTTON SYSTEM
        '.btn-fire': {
          '@apply bg-fire-red text-white px-6 py-3 rounded-lg font-semibold transition-all duration-300 hover:bg-fire-red-600 focus:outline-none focus:ring-2 focus:ring-fire-red focus:ring-offset-2 shadow-fire active:scale-95': {},
        },
        '.btn-ember': {
          '@apply bg-fire-ember text-white px-6 py-3 rounded-lg font-semibold transition-all duration-300 hover:bg-fire-ember-600 focus:outline-none focus:ring-2 focus:ring-fire-ember focus:ring-offset-2 shadow-ember': {},
        },
        '.btn-fire-gradient': {
          '@apply bg-fire-gradient text-white px-6 py-3 rounded-lg font-semibold transition-all duration-300 hover:scale-105 focus:outline-none focus:ring-2 focus:ring-fire-red focus:ring-offset-2 shadow-fire-lg active:scale-95': {},
        },

        // 📋 CARD SYSTEM
        '.card-fire': {
          '@apply bg-white border border-fire-cream-300 rounded-2xl shadow-fire hover:shadow-fire-lg transition-all duration-300 overflow-hidden': {},
        },
        '.card-ember': {
          '@apply bg-gradient-to-br from-fire-ember-50 to-fire-orange-50 border border-fire-ember-200 rounded-2xl shadow-ember hover:shadow-fire transition-all duration-300': {},
        },

        // 🎯 STATUS INDICATORS
        '.status-fire': {
          '@apply inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-fire-red-100 text-fire-red-800 border border-fire-red-200': {},
        },
        '.status-ember': {
          '@apply inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-fire-ember-100 text-fire-ember-800 border border-fire-ember-200': {},
        },
        '.status-success': {
          '@apply inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-success-100 text-success-800 border border-success-200': {},
        },

        // 📝 FORM ELEMENTS
        '.input-fire': {
          '@apply w-full px-4 py-3 border border-warmgray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-fire-red focus:border-fire-red transition-all duration-200 bg-white': {},
        },

        // 🏗️ LAYOUT CONTAINERS
        '.container-fire': {
          '@apply max-w-7xl mx-auto px-4 sm:px-6 lg:px-8': {},
        },

        // 🎨 BACKGROUND PATTERNS
        '.bg-fire-pattern': {
          'background-image': 'radial-gradient(circle at 25% 25%, rgba(199, 43, 50, 0.1) 0%, transparent 50%), radial-gradient(circle at 75% 75%, rgba(255, 107, 53, 0.1) 0%, transparent 50%)',
        },
      })
    }
  ],
}