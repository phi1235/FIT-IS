-- Ensure schema exists
CREATE SCHEMA IF NOT EXISTS ticket;

-- Insert sample tickets
INSERT INTO ticket.ticket (id, code, title, description, status, amount, maker_user_id, checker_user_id, created_at, updated_at) 
VALUES 
(gen_random_uuid(), 'TCK-001', 'Bàn giao thiết bị IT', 'Yêu cầu bàn giao Laptop Dell XPS cho nhân viên mới bộ phận Dev', 'APPROVED', 25000000.00, 'f5dc0d8a-6be5-40c3-a65c-dc50b5c1dbcf', 'b55a5ec3-a6ec-4143-8e4c-95a50037e400', NOW(), NOW()),

(gen_random_uuid(), 'TCK-002', 'Thanh toán tiền điện tháng 12', 'Chi phí điện năng cho văn phòng tầng 15 tòa nhà FIS', 'PENDING', 12450000.00, 'f5dc0d8a-6be5-40c3-a65c-dc50b5c1dbcf', NULL, NOW(), NOW()),

(gen_random_uuid(), 'TCK-003', 'Mua bản quyền phần mềm JetBrains', 'Gia hạn 10 license IntelliJ IDEA Ultimate cho team Core', 'DRAFT', 50000000.00, '8a8e8e38-f5cb-4aa6-a61b-741ecb8e4551', NULL, NOW(), NOW()),

(gen_random_uuid(), 'TCK-004', 'Sửa chữa điều hòa phòng Server', 'Điều hòa số 2 bị chảy nước, cần gọi bảo trì gấp', 'PENDING', 3500000.00, 'ce42e543-2d75-4210-b6ca-b6eb1c0533fe', NULL, NOW(), NOW()),

(gen_random_uuid(), 'TCK-005', 'Nâng cấp RAM server DB', 'Nâng cấp từ 64GB lên 128GB cho Production Server', 'REJECTED', 15000000.00, '8a8e8e38-f5cb-4aa6-a61b-741ecb8e4551', 'b55a5ec3-a6ec-4143-8e4c-95a50037e400', NOW(), NOW());
