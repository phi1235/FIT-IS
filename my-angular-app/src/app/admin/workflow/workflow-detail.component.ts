import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApprovalRequest, ApprovalStep, WorkflowService } from '../../services/workflow.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-workflow-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workflow-detail.component.html',
  styleUrl: './workflow-detail.component.css'
})
export class WorkflowDetailComponent implements OnInit {
  request: ApprovalRequest | null = null;
  loading = false;
  actionLoading = false;

  actionForm = {
    comments: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private workflowService: WorkflowService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadRequest(id);
  }

  loadRequest(id: string): void {
    this.loading = true;
    this.workflowService.getRequestById(id).subscribe({
      next: (req) => {
        this.request = req;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load request', err);
        this.toastService.error('Không thể tải thông tin yêu cầu');
        this.loading = false;
      }
    });
  }

  getCurrentStep(): ApprovalStep | undefined {
    if (!this.request?.steps) return undefined;
    return this.request.steps.find(s => s.stepNumber === this.request!.currentStep && s.status === 'PENDING');
  }

  processAction(action: 'APPROVE' | 'REJECT'): void {
    if (!this.request) return;
    const currentStep = this.getCurrentStep();
    if (!currentStep) return;

    this.actionLoading = true;
    this.workflowService.processStep(this.request.id, currentStep.id, {
      action,
      comments: this.actionForm.comments
    }).subscribe({
      next: (updated) => {
        this.request = updated;
        this.actionForm.comments = '';
        this.actionLoading = false;
        this.toastService.success(action === 'APPROVE' ? 'Đã phê duyệt thành công' : 'Đã từ chối thành công');
      },
      error: (err) => {
        console.error('Failed to process step', err);
        this.toastService.error('Có lỗi xảy ra khi xử lý yêu cầu');
        this.actionLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/workflow']);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'APPROVED':
      case 'COMPLETED': return 'status-completed';
      case 'REJECTED': return 'status-rejected';
      case 'SKIPPED': return 'status-skipped';
      default: return '';
    }
  }
}
