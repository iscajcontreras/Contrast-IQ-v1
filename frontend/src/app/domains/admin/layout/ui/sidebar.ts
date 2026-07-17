import { Component } from '@angular/core';
import { Navigation } from '@/app/domains/admin/layout/ui/navigation';
import { User } from '@/app/domains/admin/layout/ui/user';

@Component({
  selector: 'admin-sidebar',
  imports: [Navigation, User],
  host: {
    class: 'flex w-full flex-auto flex-col',
  },
  template: `
    <!-- Header -->
    <div class="relative flex items-center gap-x-2.5 pt-5 pr-4 pb-0 pl-6">
      <!-- Logo -->
      <img
        src="/images/logo/contrastiq-icon.png"
        class="size-8"
        alt="ContrastIQ logo"
      />

      <div class="flex flex-col">
        <div class="text-lg leading-none font-bold tracking-wider">
          <span class="text-[#022984] dark:text-[#4d8fff]">Contrast</span
          ><span class="text-[#01c9bd]">&gt;IQ</span>
        </div>
        <div class="font-mono text-2xs leading-3 font-medium tracking-tighter">
          Inteligencia que optimiza cada inyeccion
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <navigation class="mt-8 mb-4 flex-auto" />

    <!-- Spacer -->
    <div class="flex-auto"></div>

    <!-- Footer -->
    <div class="p-2">
      <user />
    </div>
  `,
})
export class AdminSidebar {}
