import {Component} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {SidebarMenuComponent} from './features/sidebar-menu/sidebar-menu.component';
import {ToastModule} from 'primeng/toast';
import {NgIf, NgStyle} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {filter} from 'rxjs/operators';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SidebarMenuComponent, ToastModule, NgIf, FormsModule, NgStyle],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})

export class AppComponent {

  shouldShowSidebar = true;

  constructor(private router: Router, private activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.shouldShowSidebar = !this.getRouteData('hideSidebar');
    });
  }

  private getRouteData(key: string): any {
    let route = this.activatedRoute;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route.snapshot.data[key];
  }

}
