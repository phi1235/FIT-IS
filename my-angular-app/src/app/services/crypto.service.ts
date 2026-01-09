import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import JSEncrypt from 'jsencrypt';

/**
 * Service for cryptographic operations
 * Uses RSA encryption for secure password transmission
 * 
 * Format: KEY|PASSWORD|USERNAME (có thể đảo thứ tự theo config)
 * - KEY: Random key để tăng độ phức tạp
 * - PASSWORD: Mật khẩu người dùng  
 * - USERNAME: Tên đăng nhập
 * Được mã hóa RSA thành 1 chuỗi duy nhất
 */
@Injectable({
    providedIn: 'root'
})
export class CryptoService {
    private publicKey: string | null = null;
    private encryptor: JSEncrypt | null = null;

    // Delimiter để tách các phần (có thể thay đổi)
    private readonly DELIMITER = '|';
    
    // Độ dài random key
    private readonly KEY_LENGTH = 16;

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
                console.warn('Failed to fetch RSA public key, will use plain password:', err.status);
                return of('');
            })
        );
    }

    /**
     * Generate random key for encryption
     */
    private generateRandomKey(length: number = this.KEY_LENGTH): string {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        const cryptoObj = window.crypto || (window as any).msCrypto;
        if (cryptoObj) {
            const values = new Uint32Array(length);
            cryptoObj.getRandomValues(values);
            for (let i = 0; i < length; i++) {
                result += chars[values[i] % chars.length];
            }
        } else {
            for (let i = 0; i < length; i++) {
                result += chars.charAt(Math.floor(Math.random() * chars.length));
            }
        }
        return result;
    }

    /**
     * Encrypt login credentials using RSA
     * Format: KEY|PASSWORD|USERNAME
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @returns RSA encrypted string (Base64 encoded)
     */
    encryptCredentials(username: string, password: string): Observable<string> {
        if (this.encryptor && this.publicKey) {
            const encrypted = this.doEncryptCredentials(username, password);
            if (encrypted) {
                return of(encrypted);
            }
        }

        return this.fetchPublicKey().pipe(
            map(() => {
                if (!this.encryptor) {
                    throw new Error('RSA encryptor not initialized');
                }
                const encrypted = this.doEncryptCredentials(username, password);
                if (!encrypted) {
                    throw new Error('RSA encryption failed');
                }
                return encrypted;
            })
        );
    }

    /**
     * Internal method to perform encryption
     */
    private doEncryptCredentials(username: string, password: string): string | false {
        if (!this.encryptor) return false;

        const key = this.generateRandomKey();
        
        // Định dạng chuẩn duy nhất: KEY|PASSWORD|USERNAME
        const combined = `${key}${this.DELIMITER}${password}${this.DELIMITER}${username}`;

        return this.encryptor.encrypt(combined);
    }

    /**
     * Encrypt single value using RSA (backward compatible)
     * @param value Plain text value
     * @returns RSA encrypted string (Base64 encoded)
     */
    encryptPassword(value: string): Observable<string> {
        if (this.encryptor && this.publicKey) {
            const encrypted = this.encryptor.encrypt(value);
            if (encrypted) {
                return of(encrypted);
            }
        }

        return this.fetchPublicKey().pipe(
            map(() => {
                if (!this.encryptor) {
                    throw new Error('RSA encryptor not initialized');
                }
                const encrypted = this.encryptor.encrypt(value);
                if (!encrypted) {
                    throw new Error('RSA encryption failed');
                }
                return encrypted;
            })
        );
    }

    /**
     * Synchronous encrypt - use only if key is already loaded
     */
    encryptPasswordSync(value: string): string {
        if (this.encryptor && this.publicKey) {
            const encrypted = this.encryptor.encrypt(value);
            if (encrypted) {
                return encrypted;
            }
        }
        console.warn('RSA not available, returning plain value');
        return value;
    }

    /**
     * Check if RSA encryption is ready
     */
    isReady(): boolean {
        return this.publicKey !== null && this.encryptor !== null;
    }
}
