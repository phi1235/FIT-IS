import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EmailTemplate {
  id?: string;
  templateCode: string;
  templateName: string;
  subject: string;
  htmlBody: string;
  textBody?: string;
  requiredVariables?: string[];
  version?: number;
  active?: boolean;
  createdBy?: string;
  updatedBy?: string;
  createdAt?: string;
  updatedAt?: string;
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
export class EmailTemplateService {
  private apiUrl = '/api/auth/admin/email-templates';

  constructor(private http: HttpClient) { }

  getAllTemplates(page: number = 0, size: number = 10): Observable<Page<EmailTemplate>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<EmailTemplate>>(this.apiUrl, { params });
  }

  getTemplateById(id: string): Observable<EmailTemplate> {
    return this.http.get<EmailTemplate>(`${this.apiUrl}/${id}`);
  }

  createTemplate(template: EmailTemplate): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(this.apiUrl, template);
  }

  updateTemplate(id: string, template: EmailTemplate): Observable<EmailTemplate> {
    return this.http.put<EmailTemplate>(`${this.apiUrl}/${id}`, template);
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  testTemplate(templateCode: string, variables: any, recipientEmail: string): Observable<any> {
    // Note: This endpoint is proposed but might need implementation in EmailTemplateController
    return this.http.post(`${this.apiUrl}/test`, { templateCode, variables, recipientEmail });
  }
}
