import { Component, inject, signal } from '@angular/core';
import {
  form,
  FormField,
  required,
  submit,
  validate,
} from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '@/app/core/auth/auth-api.service';

@Component({
  selector: 'auth-reset-password',
  templateUrl: './reset-password.html',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    FormField,
    MatCard,
  ],
})
export default class AuthResetPassword {
  // Dependencies
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authApi = inject(AuthApiService);

  // El enlace del correo trae ?token=... (ver AuthService.solicitarRecuperacion
  // del backend, que arma esta misma URL)
  private token = this.route.snapshot.queryParamMap.get('token');

  // State
  protected resetPasswordFormModel = signal({
    password: '',
    passwordValidation: '',
  });
  protected resetPasswordForm = form(this.resetPasswordFormModel, (form) => {
    required(form.password, { message: 'You must enter a password' });
    required(form.passwordValidation, {
      message: 'You must enter a password',
    });
    validate(form.passwordValidation, (ctx) => {
      const password = ctx.valueOf(form.password);
      const passwordValidation = ctx.value();

      if (!password || !passwordValidation) return null;

      if (password !== passwordValidation) {
        return {
          kind: 'mismatch',
          message: 'The passwords do not match',
        };
      }

      return null;
    });
  });

  protected error = signal<string | null>(null);

  resetPassword(event: Event) {
    event.preventDefault();
    this.error.set(null);

    if (!this.token) {
      this.error.set('El enlace no incluye un token valido. Solicita uno nuevo desde "Forgot password".');
      return;
    }

    submit(this.resetPasswordForm, async () => {
      try {
        await firstValueFrom(
          this.authApi.restablecerPassword({
            token: this.token!,
            nuevaPassword: this.resetPasswordFormModel().password,
          })
        );
        this.router.navigateByUrl('/auth/sign-in');
      } catch (e: any) {
        this.error.set(e?.error?.mensaje ?? 'No se pudo restablecer la contrasena.');
      }
    });
  }
}

