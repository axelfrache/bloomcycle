import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:9090/api/v1';

  constructor(private http: HttpClient) {}

  private getHeaders(isMultipart: boolean = false): HttpHeaders {
    const headers: { [key: string]: string } = {
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    };

    if (!isMultipart) {
      headers['Content-Type'] = 'application/json';
    }

    return new HttpHeaders(headers);
  }

  get<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}/${endpoint}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  post<T>(endpoint: string, data: any): Observable<T> {
    const isMultipart = data instanceof FormData;
    return this.http.post<T>(`${this.baseUrl}/${endpoint}`, data, { headers: this.getHeaders(isMultipart) })
      .pipe(catchError(this.handleError));
  }

  put<T>(endpoint: string, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${endpoint}`, data, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}/${endpoint}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    console.error(`Error occurred: ${error.message}`);
    return throwError(() => new Error('Une erreur est survenue, veuillez réessayer plus tard.'));
  }
}
