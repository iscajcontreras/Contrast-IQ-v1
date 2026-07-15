import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Injectable, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { environment } from '@/environments/environment';
import { AuthService } from '@/app/core/auth/auth.service';

export interface Alerta {
  id: number;
  tipo: string;
  severidad: 'INFO' | 'ADVERTENCIA' | 'CRITICA';
  inyector: string | null;
  sala: string | null;
  mensaje: string;
  fechaHora: string;
  resuelta: boolean;
}

interface PaginaAlertas {
  content: Alerta[];
  totalElements: number;
}

// Alertas en tiempo real: al conectar, se suscribe a /topic/alertas via
// WebSocket (STOMP sobre SockJS) para recibir cada alerta nueva sin
// tener que recargar el dashboard. El historial (lo ya ocurrido) se
// sigue consultando por HTTP normal.
@Injectable({ providedIn: 'root' })
export class AlertasService implements OnDestroy {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  private stompClient: Client | null = null;

  // Alertas recibidas en vivo desde que se abrio la pagina (mas
  // recientes primero). No reemplaza el historial completo, solo lo
  // que llega mientras la conexion esta activa.
  alertasEnVivo = signal<Alerta[]>([]);
  conectado = signal(false);

  conectar(): void {
    if (!this.esNavegador || this.stompClient) return;

    // WebSocket nativo del navegador (sin SockJS): http://host -> ws://host,
    // https://host -> wss://host.
    const wsBase = environment.apiBaseUrl.replace(/\/api$/, '').replace(/^http/, 'ws');
    this.stompClient = new Client({
      brokerURL: `${wsBase}/ws`,
      reconnectDelay: 5000,
      onConnect: () => {
        this.conectado.set(true);
        this.stompClient?.subscribe('/topic/alertas', (mensaje) => {
          const alerta: Alerta = JSON.parse(mensaje.body);
          this.alertasEnVivo.update((actual) => [alerta, ...actual].slice(0, 50));
        });
      },
      onDisconnect: () => this.conectado.set(false),
      onStompError: () => this.conectado.set(false),
    });

    this.stompClient.activate();
  }

  desconectar(): void {
    this.stompClient?.deactivate();
    this.stompClient = null;
    this.conectado.set(false);
  }

  ngOnDestroy(): void {
    this.desconectar();
  }

  // --- Historial por HTTP (paginado) ---
  listar(filtros: { resuelta?: boolean; severidad?: string; tipo?: string; page?: number; size?: number } = {}) {
    let params: Record<string, string> = {};
    Object.entries(filtros).forEach(([k, v]) => {
      if (v !== undefined && v !== null) params[k] = String(v);
    });
    return this.http.get<PaginaAlertas>(`${environment.apiBaseUrl}/alertas`, { params });
  }

  resolver(id: number) {
    return this.http.patch<Alerta>(`${environment.apiBaseUrl}/alertas/${id}/resolver`, {});
  }
}
