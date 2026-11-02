import { Routes } from '@angular/router';
import { HostPage } from './lobby/host-page';
import { JoinPage } from './lobby/join-page';
import { StartPage } from './lobby/start-page';

export const routes: Routes = [
  { path: '', component: StartPage, title: 'Yahoot' },
  { path: 'host', component: HostPage, title: 'Yahoot · Host' },
  { path: 'join', component: JoinPage, title: 'Yahoot · Mitspielen' },
  { path: '**', redirectTo: '' },
];
