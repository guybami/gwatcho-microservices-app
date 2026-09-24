import { provideZoneChangeDetection } from "@angular/core";
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { AuthService } from './app/core/auth/auth.service';

async function bootstrap() {

  const authService = new AuthService();

  await authService.init();

  await bootstrapApplication(
    AppComponent,
    {...appConfig, providers: [provideZoneChangeDetection(), ...appConfig.providers]}
  );
}

bootstrap().catch(error => {
  console.error('Angular bootstrap failed', error);
});
