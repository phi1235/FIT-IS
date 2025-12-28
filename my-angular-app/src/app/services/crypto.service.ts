import { Injectable } from '@angular/core';
import * as CryptoJS from 'crypto-js';

/**
 * Service for cryptographic operations
 * Used for client-side password hashing before transmission
 */
@Injectable({
    providedIn: 'root'
})
export class CryptoService {

    /**
     * Hash password using SHA256
     * @param password Plain text password
     * @returns SHA256 hash as hex string (64 characters)
     */
    hashPassword(password: string): string {
        return CryptoJS.SHA256(password).toString(CryptoJS.enc.Hex);
    }

    /**
     * Check if a string is a valid SHA256 hash
     * @param hash String to check
     * @returns true if valid SHA256 hash
     */
    isValidSha256(hash: string): boolean {
        return !!hash && hash.length === 64 && /^[a-f0-9]+$/i.test(hash);
    }
}
