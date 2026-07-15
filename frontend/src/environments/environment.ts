// Entorno de desarrollo (usado por defecto con `npm start` / `ng serve`).
// El backend Spring Boot debe correr en esta URL y tener habilitado
// el origen http://localhost:4200 en su configuracion de CORS
// (ver application.properties: app.cors.origenes-permitidos).
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
};
