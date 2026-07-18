// Entorno de produccion. Este archivo reemplaza a environment.ts durante
// `ng build --configuration production` (ver "fileReplacements" en angular.json).
// Ajusta apiBaseUrl a la URL real donde quede publicado el backend.
export const environment = {
  production: true,
  apiBaseUrl: 'https://contrast-iq-v1.onrender.com/api',
};
