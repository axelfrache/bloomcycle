import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { AppComponent } from './app.component';
import { HomeModule } from './features/home/home.module';
import { routes } from './app.routes';

@NgModule({
  imports: [
    BrowserModule,
    HomeModule,
    RouterModule.forRoot(routes),
    AppComponent
  ]
})
export class AppModule {}

