# FIS Portal — Roadmap Phát Triển

> Cập nhật: 2026-03-11

## Trạng Thái Hiện Tại

| Module | Trạng thái | Ghi chú |
|---|---|---|
| Auth / JWT | ✅ Hoàn thành | HS256, Redis, password management |
| User Management | ✅ Hoàn thành | Profile, department, branch |
| Ticket System | ✅ Core xong | Thiếu: SLA, assignment, template, subtask |
| Workflow | ✅ Core xong | Thiếu: dynamic builder, conditional |
| Audit Logs | ✅ Core xong | Thiếu: change tracking, UI filter nâng cao |
| Report / JasperSoft | ✅ Backend xong | UI cần hoàn thiện |
| Email Templates | ✅ Hoàn thành | CRUD, send |
| Notification | ⚠️ Nửa chừng | Entity có, Angular service có, thiếu bell icon + WebSocket |
| Maker/Checker Workspace | ⚠️ Nửa chừng | Component có nhưng chưa link route |
| Dashboard Analytics | ❌ Chưa có | Chỉ có admin panel cơ bản |

---

## PHASE 1 — Hoàn thiện tính năng còn dang dở (Ưu tiên cao nhất)

> Mục tiêu: Hoàn thành những thứ đã làm một nửa. Nhanh, giá trị cao.

### 1.1 Maker Workspace & Checker Hub
**Thời gian:** ~1 ngày
**Why now:** Component đã có (`maker-workspace.component.ts`, `checker-hub.component.ts`), chỉ thiếu link route và styling.

**Việc cần làm:**
- Thêm route `/tickets/maker` → `MakerWorkspaceComponent`
- Thêm route `/tickets/checker` → `CheckerHubComponent`
- Thêm link vào sidebar của AdminLayout
- Xem xét phân quyền: Maker → role MAKER, Checker → role CHECKER

---

### 1.2 Notification System (In-app)
**Thời gian:** ~2-3 ngày
**Why now:** `SystemNotification` entity đã có trong audit-service, `notification.service.ts` đã có trong Angular. Permissions `AUDIT_VIEW` có thể dùng tạm.

**Backend (audit-service):**
- API `GET /api/audit/notifications?userId=&read=false` — đã có endpoint?
- API `PUT /api/audit/notifications/{id}/read`
- API `PUT /api/audit/notifications/read-all`

**Frontend:**
- Bell icon trong AdminLayout header với badge đếm unread
- Dropdown list notification khi click
- Polling mỗi 30s (đơn giản, không cần WebSocket giai đoạn này)
- Click notification → navigate đến link

**Trigger notification khi:**
- Ticket được tạo → notify checker
- Ticket được approve/reject → notify maker
- Workflow step đến lượt → notify approver

---

### 1.3 SLA cho Ticket
**Thời gian:** ~2 ngày
**Why now:** Column `priority.sla_duration_hours` đã có sẵn trong DB schema.

**Backend (ticket-service):**
- Thêm field `sla_deadline` (timestamp) vào bảng `ticket.ticket`
- Tính `sla_deadline = created_at + priority.sla_duration_hours`
- Scheduler `@Scheduled` chạy mỗi 15 phút: check ticket quá hạn → cập nhật status, tạo notification
- API trả về `sla_status: ON_TIME | WARNING | BREACHED` và `sla_remaining_hours`

**Frontend:**
- Hiển thị SLA countdown trong ticket list và detail
- Badge màu: xanh (OK), vàng (< 20% thời gian còn), đỏ (quá hạn)

---

## PHASE 2 — Core Enhancements (Giá trị cao, khả thi)

### 2.1 Dashboard Analytics
**Thời gian:** ~3 ngày
**Why now:** Dữ liệu đã có, chỉ cần aggregate và visualize. Quản lý cần visibility.

**Backend:** Thêm analytics endpoints trong ticket-service và audit-service:
```
GET /api/tickets/stats → { total, open, inProgress, closed, byPriority, byDepartment }
GET /api/tickets/stats/sla → { onTime, warning, breached }
GET /api/audit/stats → { loginCount, ticketActions, topUsers }
```

**Frontend:** Tạo component `DashboardComponent` với:
- Dùng **Chart.js** (nhẹ, dễ dùng với Angular): `npm install chart.js ng2-charts`
- Cards: Total tickets, Open, SLA violations, Pending approvals
- Bar chart: Tickets by department
- Pie chart: Tickets by status / priority
- Line chart: Tickets created per week

---

### 2.2 Ticket Assignment (Assignee + Watcher)
**Thời gian:** ~3-4 ngày

**DB (thêm vào init-db):**
```sql
CREATE TABLE ticket.ticket_assignee (
    ticket_id UUID REFERENCES ticket.ticket(id),
    user_id UUID,
    assigned_at TIMESTAMP DEFAULT NOW(),
    assigned_by UUID,
    PRIMARY KEY (ticket_id, user_id)
);

CREATE TABLE ticket.ticket_watcher (
    ticket_id UUID REFERENCES ticket.ticket(id),
    user_id UUID,
    PRIMARY KEY (ticket_id, user_id)
);
```

**Backend (ticket-service):**
- `POST /api/tickets/{id}/assign` — assign user
- `POST /api/tickets/{id}/watch` / `DELETE /api/tickets/{id}/watch` — watch/unwatch
- Khi assign → tạo notification cho assignee

**Frontend:**
- Trong ticket detail: dropdown chọn assignee từ user list
- Button "Watch" / "Unwatch"
- Hiển thị danh sách watcher (avatars)

---

### 2.3 Audit Change Tracking
**Thời gian:** ~1-2 ngày
**Why now:** Đã có `audit_event.metadata JSONB`. Chỉ cần thêm logic ghi change.

**Pattern metadata:**
```json
{
  "entity": "ticket",
  "entityId": "uuid",
  "changes": [
    { "field": "status", "from": "OPEN", "to": "IN_PROGRESS" },
    { "field": "priority", "from": "P2", "to": "P1" }
  ]
}
```

**Backend:** Trong ticket-service, sau mỗi update → gọi audit-service để log change.

**Frontend:** Trong AuditLogList, render changes dạng diff view (from → to).

---

## PHASE 3 — Feature Expansion (Medium priority)

### 3.1 Ticket Template
**Thời gian:** ~3 ngày

**DB:**
```sql
CREATE TABLE ticket.ticket_template (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id UUID REFERENCES ticket.ticket_category(id),
    default_priority_id UUID REFERENCES ticket.priority(id),
    form_schema JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Dùng khi:** Tạo ticket → chọn template → form tự điền category, priority, và các field mặc định.

---

### 3.2 Subtask / Parent Ticket
**Thời gian:** ~2 ngày

**DB:** Thêm column vào `ticket.ticket`:
```sql
ALTER TABLE ticket.ticket ADD COLUMN parent_ticket_id UUID REFERENCES ticket.ticket(id);
```

**Frontend:** Trong ticket detail, section "Subtasks" với danh sách ticket con + progress bar.

---

### 3.3 Workflow nâng cao — Conditional Steps
**Thời gian:** ~4-5 ngày

**Thay vì** rebuild toàn bộ dynamic builder (phức tạp), thêm **conditional routing**:

**DB:**
```sql
CREATE TABLE workflow.workflow_definition (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    entity_type VARCHAR(100),  -- 'ticket', 'request'
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE workflow.workflow_step_definition (
    id UUID PRIMARY KEY,
    definition_id UUID REFERENCES workflow.workflow_definition(id),
    step_number INT,
    approver_role VARCHAR(100),
    condition_field VARCHAR(100),  -- e.g., 'amount'
    condition_operator VARCHAR(20), -- 'gt', 'lt', 'eq'
    condition_value VARCHAR(255)
);
```

---

## PHASE 4 — Infrastructure (Sau khi core ổn định)

### 4.1 API Gateway — Rate Limiting
**Thời gian:** ~1-2 ngày
**Stack:** Redis (đã có) + Bucket4j hoặc custom filter trong gateway-service.

```java
// RateLimitFilter.java trong gateway-service
// 100 requests/minute per IP
// 1000 requests/minute per user
```

### 4.2 File Service riêng (MinIO)
**Thời gian:** ~4-5 ngày
**Why:** Attachment hiện lưu path local, không scale. MinIO là S3-compatible, self-hosted.

**Stack:** MinIO + Spring Boot file-service (port 8088)

```
POST /api/files/upload → presigned URL hoặc direct upload
GET /api/files/{id} → serve file hoặc redirect presigned URL
DELETE /api/files/{id}
```

### 4.3 Organization Tree UI
**Thời gian:** ~2 ngày
Hiển thị org tree dạng cây (`usr.department.parent_id` đã có). Dùng Angular CDK hoặc simple recursive component.

---

## PHASE 5 — Tính năng nâng cao (Sau khi stable)

| Feature | Effort | Notes |
|---|---|---|
| Search Engine (Elasticsearch) | High | Chỉ nên khi ticket > 10k records |
| Webhook Integration | Medium | Trigger khi ticket/workflow event |
| Slack/Teams Notification | Medium | Qua webhook |
| PWA / Offline | Medium | Angular service worker |
| AI Auto-categorize | High | Cần model hoặc API key |
| Knowledge Base | Medium | FAQ articles, link vào ticket |
| LDAP / AD Login | High | Chỉ nếu enterprise requirement |
| Multi-tenant | Very High | Redesign toàn bộ DB schema |
| Feature Flags | Low | Chỉ nếu cần A/B test |

---

## THỨ TỰ ƯU TIÊN TÓM TẮT

```
Phase 1 (Tuần 1-2):
  ✦ Maker/Checker Workspace → link routes (1 ngày)
  ✦ Notification bell icon + polling (2-3 ngày)
  ✦ SLA countdown + scheduler (2 ngày)

Phase 2 (Tuần 3-5):
  ✦ Dashboard Analytics + Chart.js (3 ngày)
  ✦ Ticket Assignment (assignee + watcher) (3-4 ngày)
  ✦ Audit change tracking (1-2 ngày)

Phase 3 (Tuần 6-9):
  ✦ Ticket Template (3 ngày)
  ✦ Subtask / Parent ticket (2 ngày)
  ✦ Workflow conditional (4-5 ngày)

Phase 4 (Tuần 10+):
  ✦ Rate limiting (2 ngày)
  ✦ File service / MinIO (4-5 ngày)
  ✦ Organization Tree UI (2 ngày)

Phase 5 (Future):
  - Search, Webhook, AI, Multi-tenant
```

---

## CÁC ĐỀ XUẤT BỎ QUA (hoặc để sau rất lâu)

| Đề xuất | Lý do bỏ qua |
|---|---|
| Multi-tenant | Redesign toàn bộ, không phù hợp giai đoạn này |
| AI features | Cần external API, scope quá rộng |
| Mobile/PWA | Nice-to-have, Angular đã responsive |
| Feature flags | Premature optimization |
| LDAP | Chỉ khi có enterprise requirement cụ thể |
