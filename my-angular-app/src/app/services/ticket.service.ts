import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum TicketStatus {
    DRAFT = 'DRAFT',
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

@Injectable({
    providedIn: 'root'
})
export class TicketService {
    private apiUrl = '/api/tickets';

    constructor(private http: HttpClient) { }

    getAllTickets(): Observable<TicketDTO[]> {
        return this.http.get<TicketDTO[]>(this.apiUrl);
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

    downloadReport(format: string): void {
        const url = `/api/reports/tickets?format=${format}`;
        window.open(url, '_blank');
    }
}
