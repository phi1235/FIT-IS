import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditEvent {
  id: string;
  eventType: string;
  eventTime: string;
  userId?: string;
  username?: string;
  sourceIp?: string;
  userAgent?: string;
  httpMethod?: string;
  requestUrl?: string;
  statusCode?: number;
  errorMessage?: string;
  metadata?: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private apiUrl = '/api/audit/events';

  constructor(private http: HttpClient) {}

  getEvents(page: number = 0, size: number = 20, filters?: {
    eventType?: string;
    username?: string;
    fromDate?: string;
    toDate?: string;
  }): Observable<Page<AuditEvent>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters?.eventType) params = params.set('eventType', filters.eventType);
    if (filters?.username) params = params.set('username', filters.username);
    if (filters?.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters?.toDate) params = params.set('toDate', filters.toDate);

    return this.http.get<Page<AuditEvent>>(this.apiUrl, { params });
  }
}
