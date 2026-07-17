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
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '@/app/core/auth/auth-api.service';

@Component({
  selector: 'auth-forgot-password',
  templateUrl: './forgot-password.html',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    FormField,
  ],
})
export default class AuthForgotPassword {
  // Dependencies
  private authApi = inject(AuthApiService);

  // State
  protected forgotPasswordFormModel = signal({
    email: '',
  });
  protected forgotPasswordForm = form(this.forgotPasswordFormModel, (form) => {
    required(form.email, { message: 'Debes ingresar un correo electronico' });
    email(form.email, { message: 'Debes ingresar un correo electronico valido' });
  });

  protected enviado = signal(false);

  forgotPassword(event: Event) {
    event.preventDefault();

    submit(this.forgotPasswordForm, async () => {
      // La respuesta es siempre generica (no revela si el correo existe),
      // asi que aqui simplemente mostramos el mensaje de confirmacion.
      await firstValueFrom(
        this.authApi.olvidarPassword({ email: this.forgotPasswordFormModel().email })
      );
      this.enviado.set(true);
    });
  }
}

