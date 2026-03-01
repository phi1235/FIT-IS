import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApprovalStep {
  id: string;
  stepNumber: number;
  stepName: string;
  approverType: string;
  approverRoleCode?: string;
  approverUserId?: string;
  status: string;
  comments?: string;
  actionBy?: string;
  actionAt?: string;
  dueDate?: string;
}

export interface ApprovalHistory {
  id: string;
  stepId: string;
  action: string;
  performedBy: string;
  comments?: string;
  oldStatus?: string;
  newStatus?: string;
  performedAt: string;
}

export interface ApprovalRequest {
  id: string;
  requestType: string;
  businessKey?: string;
  referenceId?: string;
  status: string;
  initiatorUserId: string;
  currentStep: number;
  totalSteps: number;
  payload?: string;
  metadata?: string;
  createdAt: string;
  updatedAt?: string;
  completedAt?: string;
  steps?: ApprovalStep[];
  history?: ApprovalHistory[];
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
export class WorkflowService {
  private apiUrl = '/api/workflow/requests';

  constructor(private http: HttpClient) {}

  getRequests(page: number = 0, size: number = 20, status?: string, requestType?: string): Observable<Page<ApprovalRequest>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) params = params.set('status', status);
    if (requestType) params = params.set('requestType', requestType);
    return this.http.get<Page<ApprovalRequest>>(this.apiUrl, { params });
  }

  getRequestById(id: string): Observable<ApprovalRequest> {
    return this.http.get<ApprovalRequest>(`${this.apiUrl}/${id}`);
  }

  createRequest(data: any): Observable<ApprovalRequest> {
    return this.http.post<ApprovalRequest>(this.apiUrl, data);
  }

  processStep(requestId: string, stepId: string, action: { action: string; comments?: string }): Observable<ApprovalRequest> {
    return this.http.post<ApprovalRequest>(`${this.apiUrl}/${requestId}/steps/${stepId}/action`, action);
  }
}
