// API Response Types
export interface ApiResponse<T = any> {
    success: boolean;
    message: string;
    data?: T;
}

export interface LoginResponse {
    success: boolean;
    message: string;
    user: UserResponse;
    token: TokenResponse;
    metadata?: MetadataResponse;
}

export interface UserResponse {
    id?: string;
    username: string;
    email?: string | null;
    firstName?: string;
    lastName?: string;
    role?: string;
}

export interface TokenResponse {
    accessToken: string;
    refreshToken?: string;
    tokenType?: string;
    expiresIn?: number;
    refreshExpiresIn?: number;
}

export interface MetadataResponse {
    authProvider?: string;
    issuedAt?: string;
    expiresAt?: string;
}

// Error Response
export interface ErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    errorCode?: string;
    path?: string;
    details?: Record<string, string>;
}

// User Info for local storage
export interface UserInfo {
    username: string;
    email?: string;
    firstName?: string;
    lastName?: string;
    roles: string[];
}

// Ticket Types
export interface Ticket {
    id: number;
    title: string;
    description: string;
    status: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED';
    createdBy: string;
    createdAt: string;
    updatedAt?: string;
    approvedBy?: string;
    approvedAt?: string;
}

export interface CreateTicketRequest {
    title: string;
    description: string;
}
