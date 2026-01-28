import { Directive, Input, TemplateRef, ViewContainerRef, OnInit, OnDestroy } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Subscription } from 'rxjs';

/**
 * Structural directive to conditionally show/hide elements based on user permissions.
 * Usage: *appPermission="'PERMISSION_CODE'"
 */
@Directive({
  standalone: true,
  selector: '[appPermission]'
})
export class PermissionDirective implements OnInit, OnDestroy {
  private permission: string = '';
  private subscription: Subscription | null = null;
  private hasView = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  @Input() set appPermission(permission: string) {
    this.permission = permission;
    this.updateView();
  }

  ngOnInit() {
    this.subscription = this.authService.userInfo$.subscribe(() => {
      this.updateView();
    });
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  private updateView() {
    const isAuthorized = this.authService.hasPermission(this.permission);

    if (isAuthorized && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!isAuthorized && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
