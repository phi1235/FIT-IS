import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AdminService, User, PagedResponse } from '../services/admin.service';
import { AuthService } from '../services/auth.service';
import { RolePermissionService, Role } from '../services/role-permission.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'app-user-management',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './user-management.component.html',
    styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit, OnDestroy {
    users: User[] = [];
    loading = false;
    error: string | null = null;
    availableRoles: Role[] = [];

    // Pagination
    currentPage = 0;
    pageSize = 20;
    totalElements = 0;
    totalPages = 0;
    pageSizeOptions = [10, 20, 50, 100];

    // Search
    searchQuery = '';
    private searchSubject = new Subject<string>();
    private destroy$ = new Subject<void>();

    // User Modal (Create/Edit)
    selectedUser: User | null = null;
    showUserModal = false;
    isEditMode = false;
    userForm: Partial<User> = {
        username: '',
        email: '',
        firstName: '',
        lastName: '',
        role: 'user'
    };

    // Export
    exporting = false;
    exportMessage: string | null = null;

    constructor(
        private adminService: AdminService,
        private http: HttpClient,
        private authService: AuthService,
        private rolePermissionService: RolePermissionService
    ) { }

    ngOnInit() {
        // Setup search debounce
        this.searchSubject.pipe(
            debounceTime(300),
            distinctUntilChanged(),
            takeUntil(this.destroy$)
        ).subscribe(query => {
            this.searchQuery = query;
            this.currentPage = 0;
            this.loadUsers();
        });

        this.loadUsers();
        this.loadAvailableRoles();
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadAvailableRoles() {
        this.rolePermissionService.getRoles().subscribe({
            next: (roles) => {
                // Filter out admin role from user management selection
                this.availableRoles = roles.filter(r => r.code.toLowerCase() !== 'admin');
            },
            error: (err) => console.error('Failed to load roles:', err)
        });
    }

    loadUsers() {
        this.loading = true;
        this.adminService.getUsersPaginated(this.currentPage, this.pageSize, this.searchQuery).subscribe({
            next: (response: PagedResponse<User>) => {
                this.users = response.content;
                this.totalElements = response.totalElements;
                this.totalPages = response.totalPages;
                this.loading = false;
            },
            error: (err) => {
                console.error('Failed to load users:', err);
                this.error = `Không thể tải danh sách users. Lỗi: ${err.status || 0}`;
                this.loading = false;
            }
        });
    }

    // Search handler
    onSearch(event: Event) {
        const query = (event.target as HTMLInputElement).value;
        this.searchSubject.next(query);
    }

    // Pagination handlers
    goToPage(page: number) {
        if (page >= 0 && page < this.totalPages) {
            this.currentPage = page;
            this.loadUsers();
        }
    }

    nextPage() {
        if (this.currentPage < this.totalPages - 1) {
            this.currentPage++;
            this.loadUsers();
        }
    }

    previousPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.loadUsers();
        }
    }

    onPageSizeChange(event: Event) {
        this.pageSize = Number((event.target as HTMLSelectElement).value);
        this.currentPage = 0;
        this.loadUsers();
    }

    getPageNumbers(): number[] {
        const pages: number[] = [];
        const maxVisiblePages = 5;
        let start = Math.max(0, this.currentPage - Math.floor(maxVisiblePages / 2));
        let end = Math.min(this.totalPages, start + maxVisiblePages);

        if (end - start < maxVisiblePages) {
            start = Math.max(0, end - maxVisiblePages);
        }

        for (let i = start; i < end; i++) {
            pages.push(i);
        }
        return pages;
    }

    // User Modal CRUD logic

    openCreateModal() {
        this.selectedUser = null;
        this.isEditMode = false;
        this.userForm = {
            username: '',
            email: '',
            firstName: '',
            lastName: '',
            role: 'user'
        };
        this.showUserModal = true;
    }

    openEditModal(user: User) {
        this.selectedUser = user;
        this.isEditMode = true;
        this.userForm = { ...user };
        this.showUserModal = true;
    }

    closeUserModal() {
        this.showUserModal = false;
        this.selectedUser = null;
    }

    saveUser() {
        this.loading = true;
        this.error = null;

        if (this.isEditMode && this.selectedUser?.id) {
            this.adminService.updateUser(this.selectedUser.id, this.userForm).subscribe({
                next: () => {
                    this.closeUserModal();
                    this.loadUsers();
                },
                error: (err) => {
                    console.error('Update failed:', err);
                    this.error = 'Không thể cập nhật user.';
                    this.loading = false;
                }
            });
        } else {
            this.adminService.createUser(this.userForm).subscribe({
                next: () => {
                    this.closeUserModal();
                    this.loadUsers();
                },
                error: (err) => {
                    console.error('Create failed:', err);
                    this.error = 'Không thể tạo user.';
                    this.loading = false;
                }
            });
        }
    }

    deleteUser(user: User) {
        if (!user.id || !confirm(`Bạn có chắc chắn muốn xóa user ${user.username}?`)) return;

        this.loading = true;
        this.adminService.deleteUser(user.id).subscribe({
            next: () => this.loadUsers(),
            error: (err) => {
                console.error('Delete failed:', err);
                this.error = 'Không thể xóa user.';
                this.loading = false;
            }
        });
    }

    toggleUserStatus(user: User) {
        if (!user.id) return;

        this.loading = true;
        const action = user.enabled ? this.adminService.lockUser(user.id) : this.adminService.unlockUser(user.id);

        action.subscribe({
            next: () => this.loadUsers(),
            error: (err) => {
                console.error('Status toggle failed:', err);
                this.error = 'Không thể thay đổi trạng thái user.';
                this.loading = false;
            }
        });
    }

    // Avatar color based on username
    private readonly AVATAR_COLORS = [
        '#b94500','#1d4ed8','#7c3aed','#0891b2','#059669','#d97706','#dc2626','#0d9488',
    ];
    getAvatarColor(username: string): string {
        let hash = 0;
        for (let i = 0; i < username.length; i++) hash = username.charCodeAt(i) + ((hash << 5) - hash);
        return this.AVATAR_COLORS[Math.abs(hash) % this.AVATAR_COLORS.length];
    }

    // Role badge colors
    private readonly ROLE_BG: Record<string, string> = {
        ADMIN:   '#f3e8ff', CHECKER: '#e0f2fe',
        MAKER:   '#fef3c7', USER:    '#dbeafe',
    };
    private readonly ROLE_FG: Record<string, string> = {
        ADMIN:   '#7c3aed', CHECKER: '#0891b2',
        MAKER:   '#b45309', USER:    '#1d4ed8',
    };
    getRoleBg(role: string): string {
        return this.ROLE_BG[(role || '').toUpperCase()] ?? '#f3f4f6';
    }
    getRoleColor(role: string): string {
        return this.ROLE_FG[(role || '').toUpperCase()] ?? '#6b7280';
    }

    // Role update backup (still mapping to old UI if needed)
    openRoleModal(user: User) {
        this.selectedUser = user;
        this.userForm = { ...user };
        this.showUserModal = true;
        this.isEditMode = true;
    }

    // Export state
    exportProgress = 0;
    private pollingInterval: any = null;

    downloadReport(format: string) {
        if (this.exporting) return;

        this.exporting = true;
        this.exportProgress = 0;
        this.exportMessage = `Đang khởi tạo xuất báo cáo ${format.toUpperCase()}...`;
        this.error = null;

        const token = this.authService.getToken();
        const headers = new HttpHeaders({
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        });

        // Step 1: Start async generation
        this.http.post<any>('/api/reports/users/generate', null, {
            headers,
            params: { format }
        }).subscribe({
            next: (response) => {
                const jobId = response.jobId;
                this.exportMessage = `Đang xử lý... 0%`;
                this.pollJobStatus(jobId, format, headers);
            },
            error: (err) => {
                console.error('Error starting report:', err);
                this.error = `Không thể bắt đầu xuất báo cáo. Lỗi: ${err.status}`;
                this.exporting = false;
                this.exportMessage = null;
            }
        });
    }

    private pollJobStatus(jobId: string, format: string, headers: HttpHeaders) {
        let retryCount = 0;
        const maxRetries = 3;
        const statusUrl = `/api/reports/status/${jobId}`;

        this.pollingInterval = setInterval(() => {
            this.http.get<any>(statusUrl, { headers }).subscribe({
                next: (status) => {
                    retryCount = 0; // Reset on success
                    this.exportProgress = status.progress || 0;
                    this.exportMessage = `Đang xử lý... ${this.exportProgress}%`;

                    if (status.status === 'COMPLETED') {
                        clearInterval(this.pollingInterval);
                        this.downloadFile(jobId, format, headers);
                    } else if (status.status === 'FAILED') {
                        clearInterval(this.pollingInterval);
                        this.error = `Xuất báo cáo thất bại: ${status.errorMessage}`;
                        this.exporting = false;
                        this.exportMessage = null;
                    }
                },
                error: (err) => {
                    // Handle 429 Too Many Requests - just skip this poll
                    if (err.status === 429) {
                        retryCount++;
                        if (retryCount >= maxRetries) {
                            this.exportMessage = `Đang xử lý... (chờ server)`;
                        }
                        return; // Don't stop polling, just skip this cycle
                    }

                    clearInterval(this.pollingInterval);
                    this.error = `Lỗi kiểm tra trạng thái: ${err.status || 0}`;
                    this.exporting = false;
                    this.exportMessage = null;
                }
            });
        }, 10000); // Poll every 10 seconds
    }

    private downloadFile(jobId: string, format: string, headers: HttpHeaders) {
        this.exportMessage = `Đang tải file...`;

        this.http.get(`/api/reports/download/${jobId}`, {
            headers,
            responseType: 'blob'
        }).subscribe({
            next: (blob) => {
                const extension = format === 'xlsx' ? 'xlsx' : 'pdf';
                const mimeType = format === 'xlsx'
                    ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
                    : 'application/pdf';
                const file = new Blob([blob], { type: mimeType });
                const fileUrl = window.URL.createObjectURL(file);
                const link = document.createElement('a');
                link.href = fileUrl;
                link.download = `users_report.${extension}`;
                link.click();
                window.URL.revokeObjectURL(fileUrl);

                this.exporting = false;
                this.exportProgress = 100;
                this.exportMessage = `Đã xuất ${format.toUpperCase()} thành công! File được lưu vào thư mục Downloads.`;
                setTimeout(() => this.exportMessage = null, 5000);
            },
            error: (err) => {
                console.error('Error downloading file:', err);
                this.error = `Không thể tải file. Lỗi: ${err.status}`;
                this.exporting = false;
                this.exportMessage = null;
            }
        });
    }
}
