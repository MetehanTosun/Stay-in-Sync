import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { By } from '@angular/platform-browser';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, FormsModule, ToggleSwitch, RouterOutlet] // standalone import
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    htmlElement = document.querySelector('html') as HTMLElement;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it(`should have title 'monitoring-ui'`, () => {
    expect(component.title).toEqual('monitoring-ui');
  });

  it('should toggle dark mode on html element', () => {
    htmlElement.classList.remove('my-app-dark');
    expect(htmlElement.classList.contains('my-app-dark')).toBeFalse();

    component.toggleDarkMode();
    expect(htmlElement.classList.contains('my-app-dark')).toBeTrue();

    component.toggleDarkMode();
    expect(htmlElement.classList.contains('my-app-dark')).toBeFalse();
  });

  it('should render router-outlet', () => {
    const routerOutlet = fixture.debugElement.query(By.directive(RouterOutlet));
    expect(routerOutlet).toBeTruthy();
  });
});
