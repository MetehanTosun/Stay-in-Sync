import { Directive, ElementRef, EventEmitter, OnDestroy, OnInit, Output, Renderer2 } from '@angular/core';

/**
 * Directive: srClickOutside
 * Emits an event when the user clicks outside the canvas element.
 *
 * Usage in a template:
 *   <div srClickOutside (srClickOutside)="onOutside($event)">...</div>
 */
@Directive({ selector: '[srClickOutside]', standalone: true })
export class ClickOutsideDirective implements OnInit, OnDestroy {
  /**
   * Emitted when a click occurs outside the canvas element.
   * Payload: the MouseEvent.
   */
  @Output('srClickOutside') outside = new EventEmitter<MouseEvent>();

  // Holds the remover function returned by renderer.listen to unregister the global listener in ngOnDestroy.
  private remover: (() => void) | null = null;

  constructor(private el: ElementRef, private renderer: Renderer2) { }

  /**
   * Initializes the document-level mousedown listener. If the clicked target is
   * not contained within the canvas element, the directive emits the `outside`
   * event.
   */
  ngOnInit(): void {
    this.remover = this.renderer.listen('document', 'mousedown', (event: MouseEvent) => {
      try {
        if (!this.el.nativeElement.contains(event.target)) {
          this.outside.emit(event);
        }
      } catch (_) {
        // ignore errors when reading DOM
      }
    });
  }

  /**
   * Clean up the registered listener when the directive is destroyed.
   */
  ngOnDestroy(): void {
    if (this.remover) {
      this.remover();
      this.remover = null;
    }
  }
}
