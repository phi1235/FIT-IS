import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Router } from '@angular/router';
import { UserInfo } from '../models/api.models';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
    private userInfoSubject = new BehaviorSubject<UserInfo | null>(null);

    public isAuthenticated$: Observable<boolean> = this.isAuthenticatedSubject.asObservable();
    public userInfo$: Observable<UserInfo | null> = this.userInfoSubject.asObservable();

    constructor(private router: Router) {
        this.checkStoredAuth();
    }

    private checkStoredAuth(): void {
        const token = localStorage.getItem('access_token');
        const userInfo = localStorage.getItem('user_info');

        if (token && userInfo) {
            try {
                const user = JSON.parse(userInfo) as UserInfo;
                this.isAuthenticatedSubject.next(true);
                this.userInfoSubject.next(user);
            } catch (e) {
                this.clearAuth();
            }
        }
    }

    get isAuthenticated(): boolean {
        return this.isAuthenticatedSubject.value;
    }

    get userInfo(): UserInfo | null {
        return this.userInfoSubject.value;
    }

    get isAdmin(): boolean {
        const roles = this.userInfo?.roles || [];
        return roles.some(role =>
            role === 'admin' || role === 'ROLE_admin' || role.toLowerCase().includes('admin')
        );
    }

    get canAccessAdmin(): boolean {
        if (this.isAdmin) return true;
        // Specifically check for Admin Portal Access permission
        if (this.hasPermission('SYSTEM_ADMIN_ACCESS')) return true;
        
        // Fallback for current permissions
        const permissions = this.userInfo?.permissions || [];
        console.log('[canAccessAdmin] permissions:', permissions);
        const adminModules = ['USER', 'ROLE', 'EMAIL', 'REPORT', 'TICKET', 'AUTH', 'SYSTEM'];
        const result = permissions.some(p => adminModules.some(mod => p.startsWith(mod)));
        console.log('[canAccessAdmin] result:', result);
        return result;
    }

    getUsername(): string {
        return this.userInfo?.username || '';
    }

    getDisplayName(): string {
        const user = this.userInfo;
        if (user?.firstName && user?.lastName) {
            return `${user.firstName} ${user.lastName}`;
        }
        return user?.username || 'User';
    }

    // fe luu token
    setAuth(token: string, refreshToken: string, userInfo: UserInfo): void {
        localStorage.setItem('access_token', token);
        localStorage.setItem('refresh_token', refreshToken);
        localStorage.setItem('user_info', JSON.stringify(userInfo));

        // IMPORTANT: Update userInfo FIRST so canAccessAdmin works correctly when isAuthenticated$ emits
        this.userInfoSubject.next(userInfo);
        this.isAuthenticatedSubject.next(true);
    }

    getToken(): string | null {
        return localStorage.getItem('access_token');
    }

    getRefreshToken(): string | null {
        return localStorage.getItem('refresh_token');
    }

    logout(): void {
        this.clearAuth();
        this.router.navigate(['/home']);
    }

    private clearAuth(): void {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('user_info');

        this.isAuthenticatedSubject.next(false);
        this.userInfoSubject.next(null);
    }

    hasRole(role: string): boolean {
        const roles = this.userInfo?.roles || [];
        return roles.some(r => r === role || r === `ROLE_${role}` || r.toLowerCase().includes(role.toLowerCase()));
    }

    hasPermission(permission: string): boolean {
        if (this.isAdmin) return true;
        const permissions = this.userInfo?.permissions || [];
        return permissions.includes(permission);
    }

    hasAnyPermission(permissions: string[]): boolean {
        if (this.isAdmin) return true;
        const userPermissions = this.userInfo?.permissions || [];
        return permissions.some(p => userPermissions.includes(p));
    }
}
