import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum TicketStatus {
    DRAFT = 'DRAFT',
    PENDING = 'PENDING',
    SUBMITTED = 'SUBMITTED',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    COMPLETED = 'COMPLETED'
}

export interface TicketRequest {
    title: string;
    description?: string;
    amount?: number;
}

export interface TicketDTO {
    id: number;
    title: string;
    description?: string;
    status: TicketStatus;
    amount?: number;
    maker: string;
    checker?: string;
    rejectionReason?: string;
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
    number: number;
}

@Injectable({
    providedIn: 'root'
})
export class TicketService {
    private apiUrl = '/api/tickets';
    private reportApiUrl = '/api/reports';

    constructor(private http: HttpClient) { }

    getAllTickets(): Observable<TicketDTO[]> {
        return this.http.get<TicketDTO[]>(this.apiUrl);
    }

    getTicketsPaginated(page: number, size: number, search: string = ''): Observable<PagedTicketResponse> {
        return this.http.get<PagedTicketResponse>(`${this.apiUrl}/paginated`, {
            params: { page: page.toString(), size: size.toString(), search }
        });
    }

    getTicketById(id: number): Observable<TicketDTO> {
        return this.http.get<TicketDTO>(`${this.apiUrl}/${id}`);
    }

    getTicketsByStatus(status: TicketStatus): Observable<TicketDTO[]> {
        return this.http.get<TicketDTO[]>(`${this.apiUrl}/status/${status}`);
    }

    createTicket(request: TicketRequest): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(this.apiUrl, request);
    }

    submitTicket(id: number): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(`${this.apiUrl}/${id}/submit`, {});
    }

    approveTicket(id: number): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(`${this.apiUrl}/${id}/approve`, {});
    }

    rejectTicket(id: number, reason: string): Observable<TicketDTO> {
        return this.http.post<TicketDTO>(`${this.apiUrl}/${id}/reject`, { reason });
    }

    // Async report generation
    generateReport(format: string): Observable<ReportJobResponse> {
        return this.http.post<ReportJobResponse>(
            `${this.reportApiUrl}/tickets/generate`,
            null,
            { params: { format } }
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
}

