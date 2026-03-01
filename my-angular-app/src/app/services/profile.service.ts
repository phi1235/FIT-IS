import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MeDTO {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  createdAt: string;
  roles: string[];
  department: string;
  position: string;
}

export interface UpdateMeRequest {
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = '/api/users/me';

  constructor(private http: HttpClient) {}

  getMe(): Observable<MeDTO> {
    return this.http.get<MeDTO>(this.apiUrl);
  }

  updateMe(data: UpdateMeRequest): Observable<MeDTO> {
    return this.http.put<MeDTO>(this.apiUrl, data);
  }
}
