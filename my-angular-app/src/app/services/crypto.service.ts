import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import JSEncrypt from 'jsencrypt';

/**
 * Service for cryptographic operations
 * Uses RSA encryption for secure password transmission
 */
@Injectable({
    providedIn: 'root'
})
export class CryptoService {
    private publicKey: string | null = null;
    private encryptor: JSEncrypt | null = null;

    constructor(private http: HttpClient) {
        this.initializeRsa();
    }

    /**
     * Initialize RSA by fetching public key from server
     */
    private initializeRsa(): void {
        this.fetchPublicKey().subscribe({
            next: (key) => {
                console.log('RSA public key loaded successfully');
            },
            error: (err) => {
                console.error('Failed to load RSA public key:', err);
            }
        });
    }

    /**
     * Fetch RSA public key from server
     */
    fetchPublicKey(): Observable<string> {
        if (this.publicKey) {
            return of(this.publicKey);
        }

        return this.http.get<{ publicKey: string; algorithm: string; keySize: number }>(
            '/api/auth/public-key'
        ).pipe(
            map(response => response.publicKey),
            tap(key => {
                this.publicKey = key;
                this.encryptor = new JSEncrypt();
                this.encryptor.setPublicKey(key);
                console.log('RSA encryptor initialized');
            }),
            catchError(err => {
                // Silently log error - don't propagate to global error handler
                console.warn('Failed to fetch RSA public key, will use plain password:', err.status);
                return of(''); // Return empty string, encryption will fail and fallback to plain
            })
        );
    }

    /**
     * Encrypt password using RSA
     * @param password Plain text password
     * @returns RSA encrypted password (Base64 encoded)
     */
    encryptPassword(password: string): Observable<string> {
        // Ensure we have the public key
        if (this.encryptor && this.publicKey) {
            const encrypted = this.encryptor.encrypt(password);
            if (encrypted) {
                return of(encrypted);
            }
        }

        // If not initialized, fetch key first then encrypt
        return this.fetchPublicKey().pipe(
            map(() => {
                if (!this.encryptor) {
                    throw new Error('RSA encryptor not initialized');
                }
                const encrypted = this.encryptor.encrypt(password);
                if (!encrypted) {
                    throw new Error('RSA encryption failed');
                }
                return encrypted;
            })
        );
    }

    /**
     * Synchronous encrypt - use only if key is already loaded
     * Falls back to plain password if RSA not available
     */
    encryptPasswordSync(password: string): string {
        if (this.encryptor && this.publicKey) {
            const encrypted = this.encryptor.encrypt(password);
            if (encrypted) {
                return encrypted;
            }
        }
        console.warn('RSA not available, returning plain password');
        return password;
    }

    /**
     * Check if RSA encryption is ready
     */
    isReady(): boolean {
        return this.publicKey !== null && this.encryptor !== null;
    }
}
