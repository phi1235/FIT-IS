import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Permission {
  id: string;
  code: string;
  name: string;
  module: string;
  description?: string;
}

export interface Role {
  id: string;
  code: string;
  name: string;
  description?: string;
  isSystem: boolean;
  permissions: Permission[];
}

@Injectable({
  providedIn: 'root'
})
export class RolePermissionService {
  private apiUrl = `${environment.apiUrl}/roles`;
  private authApiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  // Role Operations
  getRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(this.apiUrl);
  }

  getRoleById(id: string): Observable<Role> {
    return this.http.get<Role>(`${this.apiUrl}/${id}`);
  }

  createRole(role: Partial<Role>): Observable<Role> {
    return this.http.post<Role>(this.apiUrl, role);
  }

  updateRole(id: string, role: Partial<Role>): Observable<Role> {
    return this.http.put<Role>(`${this.apiUrl}/${id}`, role);
  }

  deleteRole(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  getAllPermissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>(`${this.apiUrl}/permissions`);
  }

  addPermissionToRole(roleId: string, permissionCode: string): Observable<Role> {
    return this.http.post<Role>(`${this.apiUrl}/${roleId}/permissions/${permissionCode}`, {});
  }

  removePermissionFromRole(roleId: string, permissionCode: string): Observable<Role> {
    return this.http.delete<Role>(`${this.apiUrl}/${roleId}/permissions/${permissionCode}`);
  }
}
