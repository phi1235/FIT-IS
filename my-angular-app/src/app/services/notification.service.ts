import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificationDTO {
  id: string;
  title: string;
  content: string;
  type: string; // info | warning | success | danger
  createdBy: string;
  createdAt: string;
  active: boolean;
  expiresAt?: string;
}

export interface CreateNotificationRequest {
  title: string;
  content: string;
  type: string;
  expiresAt?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private base = '/api/audit/notifications';

  constructor(private http: HttpClient) {}

  getActive(): Observable<NotificationDTO[]> {
    return this.http.get<NotificationDTO[]>(`${this.base}/active`);
  }

  getAll(page = 0, size = 20): Observable<Page<NotificationDTO>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<NotificationDTO>>(this.base, { params });
  }

  create(req: CreateNotificationRequest): Observable<NotificationDTO> {
    return this.http.post<NotificationDTO>(this.base, req);
  }

  update(id: string, req: CreateNotificationRequest): Observable<NotificationDTO> {
    return this.http.put<NotificationDTO>(`${this.base}/${id}`, req);
  }

  toggle(id: string): Observable<NotificationDTO> {
    return this.http.patch<NotificationDTO>(`${this.base}/${id}/toggle`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
