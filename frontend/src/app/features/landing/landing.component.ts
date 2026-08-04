import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnimateOnScrollDirective } from '../../shared/directives/animate-on-scroll.directive';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, AnimateOnScrollDirective],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  @ViewChild('statisticheSection') statisticheSection?: ElementRef<HTMLElement>;

  navbarSolida = false;
  statsAnimate = false;

  segnalazioniGestite = 0;
  problemiRisolti = 0;

  private statsObserver?: IntersectionObserver;

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.navbarSolida = window.scrollY > 50;
  }

  ngAfterViewInit(): void {
    if (!this.statisticheSection) {
      return;
    }

    this.statsObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !this.statsAnimate) {
            this.statsAnimate = true;
            this.animaContatori();
            this.statsObserver?.disconnect();
          }
        });
      },
      { threshold: 0.1 },
    );

    this.statsObserver.observe(this.statisticheSection.nativeElement);
  }

  ngOnDestroy(): void {
    this.statsObserver?.disconnect();
  }

  scrollToComeFunziona(event: Event): void {
    event.preventDefault();
    document.getElementById('come-funziona')?.scrollIntoView({ behavior: 'smooth' });
  }

  private animaContatori(): void {
    const durata = 2000;
    const inizio = performance.now();
    const targetSegnalazioni = 1200;
    const targetRisolti = 98;

    const step = (ora: number) => {
      const progresso = Math.min((ora - inizio) / durata, 1);
      this.segnalazioniGestite = Math.round(targetSegnalazioni * progresso);
      this.problemiRisolti = Math.round(targetRisolti * progresso);

      if (progresso < 1) {
        requestAnimationFrame(step);
      }
    };

    requestAnimationFrame(step);
  }
}
