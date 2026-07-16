import { Observable, of } from 'rxjs';

// Envuelve el stream() de un rxResource que llama a un endpoint
// protegido del backend. En el servidor (SSR) nunca hay access_token
// (no existe localStorage ahi -- ver AuthService), asi que la llamada
// real siempre responde 401. Sin este guard, ese 401 deja al rxResource
// en estado de error y eso se propaga como una excepcion NO CONTROLADA
// que tumba el render SSR entero (uncaughtException en la terminal de
// `ng serve`) -- no solo para ese componente, para la pagina completa.
//
// En el servidor, en vez de llamar al backend, resolvemos de inmediato
// con `vacio`; en cuanto el cliente hidrata (con el token real en
// memoria), el propio rxResource vuelve a evaluar su stream() -- ahora
// con esNavegador=true -- y dispara la llamada real.
//
// Uso: cada componente calcula `esNavegador` UNA VEZ en su propio campo
// (no dentro de esta funcion) porque inject() solo es valido en
// contexto de inyeccion -- si se llamara aqui adentro fallaria con
// NG0203 en cuanto rxResource reevalue stream() fuera de ese contexto.
//
//   private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
//   protected sedes = rxResource({
//     stream: () => ssrSeguro(this.esNavegador, () => this.api.getSedes(), []),
//   });
export function ssrSeguro<T>(esNavegador: boolean, factory: () => Observable<T>, vacio: T): Observable<T> {
  return esNavegador ? factory() : of(vacio);
}
