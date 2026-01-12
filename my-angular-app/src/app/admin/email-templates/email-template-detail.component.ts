import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EmailTemplate, EmailTemplateService } from '../../services/email-template.service';
import { ToastService } from '../../services/toast.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-email-template-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './email-template-detail.component.html',
  styleUrl: './email-template-detail.component.css'
})
export class EmailTemplateDetailComponent implements OnInit {
  templateForm!: FormGroup;
  isEditMode = false;
  templateId: string | null = null;
  loading = false;
  saving = false;
  activeTab: 'edit' | 'preview' | 'html' = 'edit';
  previewHtml: SafeHtml = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private emailTemplateService: EmailTemplateService,
    private toastService: ToastService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
    this.initForm();
    this.templateId = this.route.snapshot.paramMap.get('id');
    if (this.templateId && this.templateId !== 'new') {
      this.isEditMode = true;
      this.loadTemplate(this.templateId);
    }
  }

  initForm(): void {
    this.templateForm = this.fb.group({
      templateCode: ['', [Validators.required]],
      templateName: ['', [Validators.required]],
      subject: ['', [Validators.required]],
      htmlBody: ['', [Validators.required]],
      textBody: [''],
      requiredVariables: [['userName', 'resetCode']]
    });
  }

  loadTemplate(id: string): void {
    this.loading = true;
    this.emailTemplateService.getTemplateById(id).subscribe({
      next: (template) => {
        this.templateForm.patchValue({
          templateCode: template.templateCode,
          templateName: template.templateName,
          subject: template.subject,
          htmlBody: template.htmlBody,
          textBody: template.textBody,
          requiredVariables: template.requiredVariables
        });
        if (this.isEditMode) {
          this.templateForm.get('templateCode')?.disable();
        }
        this.updatePreview();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load template', err);
        this.toastService.error('Không thể tải thông tin template');
        this.router.navigate(['/admin/email-templates']);
      }
    });
  }

  updatePreview(): void {
    let body = this.templateForm.value.htmlBody || '';
    // Simple placeholder substitution for preview
    const variables = this.templateForm.value.requiredVariables || [];
    variables.forEach((variable: string) => {
      body = body.replace(new RegExp(`{{${variable}}}`, 'g'), `<span style="background: #fff9c4; padding: 2px 4px; border-radius: 4px; font-weight: bold;">[${variable}]</span>`);
    });
    this.previewHtml = this.sanitizer.bypassSecurityTrustHtml(body);
  }

  onTabChange(tab: 'edit' | 'preview' | 'html'): void {
    this.activeTab = tab;
    if (tab === 'preview') {
      this.updatePreview();
    }
  }

  onSubmit(): void {
    if (this.templateForm.invalid) return;

    this.saving = true;
    const templateData = this.templateForm.getRawValue();

    if (this.isEditMode && this.templateId) {
      this.emailTemplateService.updateTemplate(this.templateId, templateData).subscribe({
        next: () => {
          this.toastService.success('Cập nhật template thành công');
          this.saving = false;
          this.router.navigate(['/admin/email-templates']);
        },
        error: (err) => {
          console.error('Update failed', err);
          this.toastService.error('Cập nhật thất bại');
          this.saving = false;
        }
      });
    } else {
      this.emailTemplateService.createTemplate(templateData).subscribe({
        next: () => {
          this.toastService.success('Tạo template mới thành công');
          this.saving = false;
          this.router.navigate(['/admin/email-templates']);
        },
        error: (err) => {
          console.error('Creation failed', err);
          this.toastService.error('Tạo mới thất bại');
          this.saving = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/admin/email-templates']);
  }
}
