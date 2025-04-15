/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts,css,scss}",
    "./src/**/*.{html,js,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'primary': '#3498db',
        'secondary': '#e74c3c',
      },
    },
  },
  plugins: [
    require('daisyui')
  ],
}
