import { Component, effect, input, signal } from '@angular/core';
import { toString as qrToSvg } from 'qrcode';

/** QR-Code zum Beitreten, damit niemand den Code abtippen muss. */
@Component({
  selector: 'app-join-qr',
  template: `
    @if (image(); as image) {
      <img [src]="image" alt="QR-Code zum Beitreten" width="220" height="220" />
    }
  `,
  styles: `
    img { background: #fff; border-radius: .6rem; padding: .4rem; }
  `,
})
export class JoinQr {
  readonly url = input.required<string>();
  protected readonly image = signal<string | null>(null);

  constructor() {
    effect(() => {
      void qrToSvg(this.url(), { type: 'svg', margin: 1 }).then((svg) =>
        this.image.set(`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`),
      );
    });
  }
}
