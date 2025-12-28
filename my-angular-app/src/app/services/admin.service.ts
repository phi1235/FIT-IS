import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KeycloakService } from './keycloak.service';
import { AuthService } from './auth.service';

export interface User {
  id?: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: string;
  enabled: boolean;
}

export interface RoleUpdateRequest {
  username: string;
  role: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  // Dùng proxy Angular: /api -> http://localhost:8082
  private apiUrl = '/api/users';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService,
    private authService: AuthService
  ) { }

  /**
   * Lấy danh sách tất cả users (chỉ admin) - Deprecated: use getUsersPaginated
   */
  getAllUsers(): Observable<User[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<User[]>(`${this.apiUrl}/admin/all`, { headers });
  }

  /**
   * Lấy danh sách users với phân trang
   */
  getUsersPaginated(page: number = 0, size: number = 20, search: string = ''): Observable<PagedResponse<User>> {
    const headers = this.getAuthHeaders();
    return this.http.get<PagedResponse<User>>(
      `${this.apiUrl}/admin/list?page=${page}&size=${size}&search=${encodeURIComponent(search)}`,
      { headers }
    );
  }

  /**
   * Lấy role của user
   */
  getUserRole(username: string): Observable<{ username: string; role: string }> {
    const headers = this.getAuthHeaders();
    return this.http.get<{ username: string; role: string }>(
      `${this.apiUrl}/admin/role?username=${username}`,
      { headers }
    );
  }

  /**
   * Cập nhật role cho user
   */
  updateUserRole(username: string, role: string): Observable<string> {
    const headers = this.getAuthHeaders();
    const body: RoleUpdateRequest = { username, role };
    return this.http.post(`${this.apiUrl}/admin/role`, body, {
      headers,
      responseType: 'text'
    });
  }

  /**
   * Lấy role của chính mình
   */
  getMyRole(): Observable<{ username: string; role: string }> {
    const headers = this.getAuthHeaders();
    return this.http.get<{ username: string; role: string }>(
      `${this.apiUrl}/me/role`,
      { headers }
    );
  }

  /**
   * Tạo HTTP headers với token
   * Ưu tiên token từ AuthService (custom auth), fallback sang KeycloakService
   */
  private getAuthHeaders(): HttpHeaders {
    // Try custom auth token first (database/remote login)
    let token: string | null = this.authService.getToken();

    // Fallback to Keycloak token
    if (!token) {
      token = this.keycloakService.getToken() || null;
    }

    return new HttpHeaders({
      'Authorization': `Bearer ${token || ''}`,
      'Content-Type': 'application/json'
    });
  }
}


