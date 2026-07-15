import { Component, inject, signal } from '@angular/core';
import {
  email,
  form,
  FormField,
  required,
  submit,
} from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '@/app/core/auth/auth.service';

@Component({
  selector: 'auth-sign-in',
  templateUrl: './sign-in.html',
  imports: [
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    FormField,
  ],
})
export default class AuthSignIn {
  // Dependencies
  private auth = inject(AuthService);
  private router = inject(Router);

  // State
  protected signInFormModel = signal({
    email: '',
    password: '',
  });
  protected signInForm = form(this.signInFormModel, (form) => {
    required(form.email, { message: 'You must enter an email address' });
    email(form.email, { message: 'You must enter a valid email address' });

    required(form.password, { message: 'You must enter a password' });
  });

  protected error = signal<string | null>(null);
  protected enviando = signal(false);

  // Login directo: el formulario manda email+password a POST
  // /api/auth/login (ver AuthService.iniciarSesion) y el backend regresa
  // un access token + refresh token en la misma respuesta -- sin
  // redirect a ninguna pantalla externa, sin doble autenticacion.
  signIn(event: Event) {
    event.preventDefault();
    if (this.enviando()) return;

    submit(this.signInForm, async () => {
      this.error.set(null);
      this.enviando.set(true);
      try {
        const { email, password } = this.signInFormModel();
        await this.auth.iniciarSesion(email, password);
        await this.router.navigateByUrl('/admin');
      } catch (e: any) {
        this.error.set(
          e?.status === 401
            ? 'Correo o contrasena incorrectos.'
            : 'No se pudo iniciar sesion. Intenta de nuevo.'
        );
      } finally {
        this.enviando.set(false);
      }
    });
  }
}
