import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SearchBarComponent } from './search-bar.component';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

describe('SearchBarComponent', () => {
  let fixture: ComponentFixture<SearchBarComponent>;
  let component: SearchBarComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, SearchBarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // --- ✅ 1. Smoke test
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --- ✅ 2. should emit search term on input event
  it('should emit value when user types in input', () => {
    spyOn(component.search, 'emit');

    const inputEl = fixture.debugElement.query(By.css('input')).nativeElement as HTMLInputElement;
    inputEl.value = 'error logs';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.search.emit).toHaveBeenCalledWith('error logs');
  });

  // --- ✅ 3. should emit search term when clicking search button
  it('should emit value when clicking search button', () => {
    spyOn(component.search, 'emit');

    const inputEl = fixture.debugElement.query(By.css('input')).nativeElement as HTMLInputElement;
    inputEl.value = 'metrics';
    fixture.detectChanges();

    const buttonEl = fixture.debugElement.query(By.css('button')).nativeElement;
    buttonEl.click();
    fixture.detectChanges();

    expect(component.search.emit).toHaveBeenCalledWith('metrics');
  });

  // --- ✅ 4. should log search events (optional)
  it('should log when search is triggered (console check)', () => {
    const consoleSpy = spyOn(console, 'log');
    component.onSearch('xyz');
    expect(consoleSpy).toHaveBeenCalledWith('Search button clicked:', 'xyz');
  });
});
