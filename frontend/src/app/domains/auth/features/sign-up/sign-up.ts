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
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '@/app/core/auth/auth-api.service';

@Component({
  selector: 'auth-sign-up',
  templateUrl: './sign-up.html',
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
export default class AuthSignUp {
  // Dependencies
  private router = inject(Router);
  private authApi = inject(AuthApiService);

  // State
  protected signUpFormModel = signal({
    name: '',
    email: '',
    password: '',
    company: '',
  });
  protected signUpForm = form(this.signUpFormModel, (form) => {
    required(form.name, { message: 'You must enter your name' });
    required(form.email, { message: 'You must enter an email address' });
    email(form.email, { message: 'You must enter a valid email address' });
    required(form.password, { message: 'You must enter a password' });
    required(form.company, { message: 'You must enter your company name' });
  });

  protected error = signal<string | null>(null);

  signUp(event: Event) {
    event.preventDefault();
    this.error.set(null);

    submit(this.signUpForm, async () => {
      const datos = this.signUpFormModel();
      try {
        await firstValueFrom(
          this.authApi.registrar({
            nombreCompleto: datos.name,
            email: datos.email,
            password: datos.password,
          })
        );
        this.router.navigateByUrl('/auth/sign-in');
      } catch (e: any) {
        this.error.set(e?.error?.mensaje ?? 'No se pudo crear la cuenta. Intenta de nuevo.');
      }
    });
  }
}

