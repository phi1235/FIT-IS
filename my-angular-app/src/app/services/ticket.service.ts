import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export enum TicketStatus {
    DRAFT = 'DRAFT',
    PENDING = 'PENDING',
    SUBMITTED = 'SUBMITTED',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    COMPLETED = 'COMPLETED'
}

export interface PriorityDTO {
    id: string;
    code: string;
    name: string;
    slaDurationHours?: number;
}

export interface TicketRequest {
    title: string;
    description?: string;
    amount?: number;
    priorityId?: string;
    categoryId?: string;
    saveDraft?: boolean;
}

export type SlaStatus = 'ON_TIME' | 'WARNING' | 'BREACHED' | 'COMPLETED';

export interface TicketDTO {
    id: string;
    code: string;
    title: string;
    description?: string;
    status: TicketStatus;
    amount?: number;
    makerUserId: string;
    checkerUserId?: string;
    makerName: string;
    checkerName?: string;
    rejectionReason?: string;
    priorityCode?: string;
    priorityName?: string;
    slaDeadline?: string;
    slaStatus?: SlaStatus;
    slaRemainingMinutes?: number;
    createdAt: string;
    updatedAt: string;
}

export interface ReportJobResponse {
    jobId: string;
    status: string;
    message: string;
}

export interface ReportStatusResponse {
    jobId: string;
    status: string;
    progress: number;
    filePath?: string;
    error?: string;
}

export interface PagedTicketResponse {
    content: TicketDTO[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number; // Spring Data 'Page' uses 'number' for current page
    first: boolean;
    last: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class TicketService {
    private apiUrl = '/api/tickets';
    private reportApiUrl = '/api/reports';

    constructor(private http: HttpClient) { }

    // Backend trả UTC không có 'Z' → Angular date pipe + new Date() hiểu sai múi giờ.
    // Normalize: thêm 'Z' vào các trường timestamp để parse đúng UTC.
    private fixTimestamps(t: TicketDTO): TicketDTO {
        const z = (s?: string) => s && !s.endsWith('Z') && !s.includes('+') ? s + 'Z' : s;
        return { ...t, createdAt: z(t.createdAt)!, updatedAt: z(t.updatedAt)!, slaDeadline: z(t.slaDeadline) };
    }

    getAllTickets(): Observable<TicketDTO[]> {
        return this.http.get<TicketDTO[]>(this.apiUrl).pipe(
            map(list => list.map(t => this.fixTimestamps(t)))
        );
    }

    getTicketsPaginated(page: number, size: number, search: string = '', status: string = ''): Observable<PagedTicketResponse> {
        let params: any = { page: page.toString(), size: size.toString(), search };
        if (status && status !== 'ALL') {
            params.status = status;
        }
        return this.http.get<PagedTicketResponse>(`${this.apiUrl}/paginated`, { params }).pipe(
            map(res => ({ ...res, content: res.content.map(t => this.fixTimestamps(t)) }))
        );
    }

    getTicketById(id: string): Observable<TicketDTO> {
        return this.http.get<TicketDTO>(`${this.apiUrl}/${id}`).pipe(
            map(t => this.fixTimestamps(t))
        );
    }

    getTicketsByStatus(status: TicketStatus): Observable<TicketDTO[]> {
        return this.http.get<TicketDTO[]>(`${this.apiUrl}/status/${status}`);
    }

    createTicket(request: TicketRequest): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(this.apiUrl, request);
    }

    updateTicket(id: string, request: TicketRequest): Observable<TicketDTO> {
        return this.http.put<TicketDTO>(`${this.apiUrl}/${id}`, request);
    }

    submitTicket(id: string): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(`${this.apiUrl}/${id}/submit`, {});
    }

    approveTicket(id: string): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(`${this.apiUrl}/${id}/approve`, {});
    }

    rejectTicket(id: string, reason: string): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(`${this.apiUrl}/${id}/reject`, { reason });
    }

    // Async report generation
    generateReport(format: string, status: string = 'ALL'): Observable<ReportJobResponse> {
        return this.http.post<ReportJobResponse>(
            `${this.reportApiUrl}/tickets/generate`,
            null,
            { params: { format, status } }
        );
    }

    getReportStatus(jobId: string): Observable<ReportStatusResponse> {
        return this.http.get<ReportStatusResponse>(`${this.reportApiUrl}/status/${jobId}`);
    }

    downloadReportFile(jobId: string): Observable<Blob> {
        return this.http.get(`${this.reportApiUrl}/download/${jobId}`, {
            responseType: 'blob'
        });
    }

    getPriorities(): Observable<PriorityDTO[]> {
        return this.http.get<PriorityDTO[]>(`${this.apiUrl}/priorities`);
    }
}

