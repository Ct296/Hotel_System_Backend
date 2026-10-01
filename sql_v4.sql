ROLLBACK;
BEGIN;

-- -----------------------------------------------------------
-- 1) HẠNG KHÁCH HÀNG
-- -----------------------------------------------------------
INSERT INTO TIER_CUSTOMER
    (TIER_CUS_ID, TIER_CUS_Name, TIER_CUS_Condition, TIER_CUS_Benefit, TIER_CUS_Discount)
VALUES
    ('TCR0000001', 'Đồng',       0,         'Hạng mặc định cho khách mới',                                           0.00),
    ('TCR0000002', 'Bạc',        5000000,   'Giảm giá cơ bản, ưu tiên hỗ trợ khi cần',                               5.00),
    ('TCR0000003', 'Vàng',       15000000,  'Giảm giá tốt hơn, ưu tiên xử lý nhanh và hỗ trợ linh hoạt',            10.00),
    ('TCR0000004', 'Kim Cương',  30000000,  'Ưu tiên hạng cao nhất, hỗ trợ nhanh, nhiều quyền lợi lưu trú hơn',     15.00);

-- -----------------------------------------------------------
-- 2) USERS
-- -----------------------------------------------------------
INSERT INTO USERS
    (USER_ID, USER_FirstName, USER_LastName, USER_Sex, USER_DateOfBirth, USER_PID,
     USER_Nationality, USER_Email, USER_PhoneNumber, USER_Avatar, USER_Role, USER_CreateDate, USER_UpdateDate)
VALUES
    ('USR0000001', 'Hệ',        'Thông',    'MALE',        '1990-01-01', '001000000001', 'Việt Nam', 'admin@ithotel.vn',            '0901000001', '/image/default_avatar_customer.jpg', 'ADMIN',    '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000002', 'Ngọc',      'Lan',      'FEMALE',      '1991-02-14', '001000000002', 'Việt Nam', 'hr.manager@ithotel.vn',       '0901000002', '/image/default_avatar_customer.jpg', 'MANAGER',  '2026-01-01 08:10:00', '2026-01-01 08:10:00'),
    ('USR0000003', 'Minh',      'Khoa',     'MALE',        '1988-06-18', '001000000003', 'Việt Nam', 'room.manager@ithotel.vn',     '0901000003', '/image/default_avatar_customer.jpg', 'MANAGER',  '2026-01-01 08:20:00', '2026-01-01 08:20:00'),
    ('USR0000004', 'Thu',       'Hà',       'FEMALE',      '1992-09-20', '001000000004', 'Việt Nam', 'service.manager@ithotel.vn',  '0901000004', '/image/default_avatar_customer.jpg', 'MANAGER',  '2026-01-01 08:30:00', '2026-01-01 08:30:00'),
    ('USR0000005', 'Quốc',      'Bảo',      'MALE',        '1993-03-05', '001000000005', 'Việt Nam', 'customer.manager@ithotel.vn', '0901000005', '/image/default_avatar_customer.jpg', 'MANAGER',  '2026-01-01 08:40:00', '2026-01-01 08:40:00'),
    ('USR0000006', 'Mai',       'Anh',      'FEMALE',      '1998-04-11', '001000000006', 'Việt Nam', 'staff1@ithotel.vn',           '0901000006', '/image/default_avatar_customer.jpg', 'STAFF',    '2026-01-02 08:00:00', '2026-01-02 08:00:00'),
    ('USR0000007', 'Hoàng',     'Nam',      'MALE',        '1997-07-21', '001000000007', 'Việt Nam', 'staff2@ithotel.vn',           '0901000007', '/image/default_avatar_customer.jpg', 'STAFF',    '2026-01-03 08:00:00', '2026-01-03 08:00:00'),
    ('USR0000008', 'Phương',    'Linh',     'FEMALE',      '1999-10-09', '001000000008', 'Việt Nam', 'staff3@ithotel.vn',           '0901000008', '/image/default_avatar_customer.jpg', 'STAFF',    '2026-01-04 08:00:00', '2026-01-04 08:00:00'),
    ('USR0000009', 'Gia',       'Hân',      'FEMALE',      '2000-02-12', '001000000009', 'Việt Nam', 'khach1@ithotel.vn',           '0901000009', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-05 09:00:00', '2026-01-05 09:00:00'),
    ('USR0000010', 'Tuấn',      'Kiệt',     'MALE',        '1996-11-23', '001000000010', 'Việt Nam', 'khach2@ithotel.vn',           '0901000010', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-06 09:00:00', '2026-01-06 09:00:00'),
    ('USR0000011', 'Khánh',     'Vy',       'FEMALE',      '1995-08-15', '001000000011', 'Việt Nam', 'khach3@ithotel.vn',           '0901000011', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-07 09:00:00', '2026-01-07 09:00:00'),
    ('USR0000012', 'Đức',       'Phát',     'MALE',        '1994-12-01', '001000000012', 'Việt Nam', 'khach4@ithotel.vn',           '0901000012', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-08 09:00:00', '2026-01-08 09:00:00'),
    ('USR0000013', 'Thanh',     'Trúc',     'UNSPECIFIED', '2001-06-30', '001000000013', 'Việt Nam', 'khach5@ithotel.vn',           '0901000013', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-09 09:00:00', '2026-01-09 09:00:00');

-- -----------------------------------------------------------
-- 3) ACCOUNT + ACCOUNT_STATUS
-- -----------------------------------------------------------
INSERT INTO ACCOUNT (USER_ID, USER_Password)
VALUES
    ('USR0000001', 'admin123456'),
    ('USR0000002', 'manager123'),
    ('USR0000003', 'manager123'),
    ('USR0000004', 'manager123'),
    ('USR0000005', 'manager123'),
    ('USR0000006', 'staff12345'),
    ('USR0000007', 'staff12345'),
    ('USR0000008', 'staff12345'),
    ('USR0000009', 'customer123'),
    ('USR0000010', 'customer123'),
    ('USR0000011', 'customer123'),
    ('USR0000012', 'customer123'),
    ('USR0000013', 'customer123');

INSERT INTO ACCOUNT_STATUS
    (ACCOUNT_STATUS_ID, ACCOUNT_STATUS_Name, ACCOUNT_STATUS_StartTime, ACCOUNT_STATUS_EndTime, ACCOUNT_STATUS_Reason, USER_ID)
VALUES
    ('AST0000001', 'ACTIVE', '2026-01-01 08:00:00', NULL, 'Khởi tạo tài khoản quản trị',   'USR0000001'),
    ('AST0000002', 'ACTIVE', '2026-01-01 08:10:00', NULL, 'Khởi tạo tài khoản quản lý',    'USR0000002'),
    ('AST0000003', 'ACTIVE', '2026-01-01 08:20:00', NULL, 'Khởi tạo tài khoản quản lý',    'USR0000003'),
    ('AST0000004', 'ACTIVE', '2026-01-01 08:30:00', NULL, 'Khởi tạo tài khoản quản lý',    'USR0000004'),
    ('AST0000005', 'ACTIVE', '2026-01-01 08:40:00', NULL, 'Khởi tạo tài khoản quản lý',    'USR0000005'),
    ('AST0000006', 'ACTIVE', '2026-01-02 08:00:00', NULL, 'Khởi tạo tài khoản nhân viên',  'USR0000006'),
    ('AST0000007', 'ACTIVE', '2026-01-03 08:00:00', NULL, 'Khởi tạo tài khoản nhân viên',  'USR0000007'),
    ('AST0000008', 'ACTIVE', '2026-01-04 08:00:00', NULL, 'Khởi tạo tài khoản nhân viên',  'USR0000008'),
    ('AST0000009', 'ACTIVE', '2026-01-05 09:00:00', NULL, 'Khởi tạo tài khoản khách hàng', 'USR0000009'),
    ('AST0000010', 'ACTIVE', '2026-01-06 09:00:00', NULL, 'Khởi tạo tài khoản khách hàng', 'USR0000010'),
    ('AST0000011', 'ACTIVE', '2026-01-07 09:00:00', NULL, 'Khởi tạo tài khoản khách hàng', 'USR0000011'),
    ('AST0000012', 'ACTIVE', '2026-01-08 09:00:00', NULL, 'Khởi tạo tài khoản khách hàng', 'USR0000012'),
    ('AST0000013', 'ACTIVE', '2026-01-09 09:00:00', NULL, 'Khởi tạo tài khoản khách hàng', 'USR0000013');

-- -----------------------------------------------------------
-- 4) PHÂN NHÁNH ROLE
-- -----------------------------------------------------------
INSERT INTO ADMIN (USER_ID) VALUES ('USR0000001');

INSERT INTO MANAGER (USER_ID, MANAGER_JobTitle)
VALUES
    ('USR0000002', 'HR_MANAGER'),
    ('USR0000003', 'ROOM_PRICING_MANAGER'),
    ('USR0000004', 'SERVICE_MANAGER'),
    ('USR0000005', 'CUSTOMER_MANAGER');

INSERT INTO STAFF (USER_ID, STAFF_EmploymentTime)
VALUES
    ('USR0000006', '2025-10-01 08:00:00'),
    ('USR0000007', '2025-10-10 08:00:00'),
    ('USR0000008', '2025-11-01 08:00:00');

INSERT INTO CUSTOMER (USER_ID)
VALUES ('USR0000009'), ('USR0000010'), ('USR0000011'), ('USR0000012'), ('USR0000013');

-- -----------------------------------------------------------
-- 5) TIER_HISTORY
-- -----------------------------------------------------------
INSERT INTO TIER_HISTORY
    (TIER_HISTORY_ID, TIER_HISTORY_StartDate, TIER_HISTORY_EndDate, TIER_HISTORY_TotalSpending, TIER_HISTORY_Reason, USER_ID, TIER_CUS_ID)
VALUES
    ('THI0000001', '2026-02-01 08:00:00', NULL,  1700000,  'Khởi tạo', 'USR0000009', 'TCR0000001'),
    ('THI0000002', '2026-02-01 08:05:00', NULL,  6900000,  'Khởi tạo', 'USR0000010', 'TCR0000002'),
    ('THI0000003', '2026-02-01 08:10:00', NULL, 20550000,  'Khởi tạo', 'USR0000011', 'TCR0000003'),
    ('THI0000004', '2026-02-01 08:15:00', NULL, 52800000,  'Khởi tạo', 'USR0000012', 'TCR0000004'),
    ('THI0000005', '2026-02-01 08:20:00', NULL,   880000,  'Khởi tạo', 'USR0000013', 'TCR0000001');

-- -----------------------------------------------------------
-- 6) LOẠI PHÒNG (FIXED: Thay maxCustomer bằng bed, children, adult)
-- -----------------------------------------------------------
INSERT INTO ROOM_TYPE
    (ROOM_TYPE_ID, ROOM_TYPE_Name, ROOM_TYPE_Area, ROOM_TYPE_Bed, ROOM_TYPE_ChildrenMax, ROOM_TYPE_AdultMax, ROOM_TYPE_BasePrice,
     ROOM_TYPE_DepositPercent, ROOM_TYPE_Description, ROOM_TYPE_CreateDate, ROOM_TYPE_UpdateDate)
VALUES
    ('RTP0000001', 'Tiêu chuẩn', 22.50, 1, 1, 2, 180000, 30, 'Phòng gọn gàng, lưu trú ngắn giờ.',         '2026-01-10 08:00:00', '2026-01-10 08:00:00'),
    ('RTP0000002', 'Superior',   28.00, 1, 1, 2, 260000, 35, 'Nội thất nâng cấp, phù hợp cặp đôi.',     '2026-01-10 08:05:00', '2026-01-10 08:05:00'),
    ('RTP0000003', 'Deluxe',     36.50, 2, 2, 3, 380000, 40, 'Phòng cao cấp, có khu tiếp khách nhỏ.',      '2026-01-10 08:10:00', '2026-01-10 08:10:00'),
    ('RTP0000004', 'Suite',      55.00, 2, 2, 4, 650000, 50, 'Phòng hạng sang, diện tích lớn cho gia đình.', '2026-01-10 08:15:00', '2026-01-10 08:15:00');

-- -----------------------------------------------------------
-- 7) TIỆN NGHI (AMENITY) - NEW TABLE
-- -----------------------------------------------------------
INSERT INTO AMENITY (AMENITY_ID, AMENITY_Name) VALUES 
    ('AMN0000001', 'Wifi tốc độ cao'),
    ('AMN0000002', 'Smart TV'),
    ('AMN0000003', 'Máy sấy tóc'),
    ('AMN0000004', 'Ban công view biển'),
    ('AMN0000005', 'Bồn tắm massage');

INSERT INTO ROOM_AMENITY (ROOM_TYPE_ID, AMENITY_ID) VALUES 
    ('RTP0000001', 'AMN0000001'), ('RTP0000001', 'AMN0000002'),
    ('RTP0000002', 'AMN0000001'), ('RTP0000002', 'AMN0000002'), ('RTP0000002', 'AMN0000003'),
    ('RTP0000003', 'AMN0000001'), ('RTP0000003', 'AMN0000002'), ('RTP0000003', 'AMN0000003'), ('RTP0000003', 'AMN0000004'),
    ('RTP0000004', 'AMN0000001'), ('RTP0000004', 'AMN0000002'), ('RTP0000004', 'AMN0000003'), ('RTP0000004', 'AMN0000004'), ('RTP0000004', 'AMN0000005');

-- -----------------------------------------------------------
-- 8) PHÒNG & ẢNH PHÒNG
-- -----------------------------------------------------------
INSERT INTO ROOM (ROOM_ID, ROOM_Name, ROOM_Location, ROOM_Status, ROOM_TYPE_ID)
VALUES
    ('ROM0000001', '101', 'Tầng 1', 'AVAILABLE',   'RTP0000001'),
    ('ROM0000002', '102', 'Tầng 1', 'AVAILABLE',   'RTP0000001'),
    ('ROM0000003', '103', 'Tầng 1', 'AVAILABLE',   'RTP0000001'),
    ('ROM0000004', '201', 'Tầng 2', 'AVAILABLE',   'RTP0000002'),
    ('ROM0000005', '202', 'Tầng 2', 'AVAILABLE',   'RTP0000002'),
    ('ROM0000006', '203', 'Tầng 2', 'AVAILABLE',   'RTP0000002'),
    ('ROM0000007', '301', 'Tầng 3', 'AVAILABLE',   'RTP0000003'),
    ('ROM0000008', '302', 'Tầng 3', 'AVAILABLE',   'RTP0000003'),
    ('ROM0000009', '303', 'Tầng 3', 'AVAILABLE',   'RTP0000003'),
    ('ROM0000010', '401', 'Tầng 4', 'AVAILABLE',   'RTP0000004'),
    ('ROM0000011', '402', 'Tầng 4', 'AVAILABLE',   'RTP0000004'),
    ('ROM0000012', '403', 'Tầng 4', 'MAINTENANCE', 'RTP0000004');

INSERT INTO ROOM_IMAGE
    (ROOM_IMAGE_ID, ROOM_IMAGE_Path, ROOM_IMAGE_IsPrimary, ROOM_IMAGE_CreateDate, ROOM_ID)
VALUES
    ('RIM0000001',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:00:00', 'ROM0000001'),
    ('RIM0000002',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:01:00', 'ROM0000002'),
    ('RIM0000003',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:02:00', 'ROM0000003'),
    ('RIM0000004',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:03:00', 'ROM0000004'),
    ('RIM0000005',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:04:00', 'ROM0000005'),
    ('RIM0000006',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:05:00', 'ROM0000006'),
    ('RIM0000007',  'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80', TRUE,  '2026-01-10 09:06:00', 'ROM0000007'),
    ('RIM0000008',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:07:00', 'ROM0000008'),
    ('RIM0000009',  '/image/default_room.jpg', TRUE,  '2026-01-10 09:08:00', 'ROM0000009'),
    ('RIM0000010',  'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', TRUE,  '2026-01-10 09:09:00', 'ROM0000010');

-- -----------------------------------------------------------
-- 9) KỲ ÁP DỤNG & DỊCH VỤ
-- -----------------------------------------------------------
INSERT INTO PRICE_RATE (PRICE_RATE_ID, PRICE_RATE_EventName, PRICE_RATE_SurchargeAmount, PRICE_RATE_CreateDate, PRICE_RATE_UpdateDate) VALUES
    ('PRC0000001', 'Phụ thu loại Tiêu chuẩn 2026', 20000, '2026-01-11 08:00:00', '2026-01-11 08:00:00'),
    ('PRC0000002', 'Phụ thu loại Superior 2026',   20000, '2026-01-11 08:05:00', '2026-01-11 08:05:00');

INSERT INTO APPLIED_PERIOD (APPLIED_PERIOD_ID, APPLIED_PERIOD_StartDate, APPLIED_PERIOD_EndDate, PRICE_RATE_ID, ROOM_TYPE_ID) VALUES
    ('APD0000001', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000001', 'RTP0000001'),
    ('APD0000002', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000002', 'RTP0000002');

INSERT INTO SERVICE
    (SERVICE_ID, SERVICE_Name, SERVICE_Description, SERVICE_Unit, SERVICE_BasePrice, SERVICE_ImagePath, SERVICE_Status, SERVICE_CreateDate, SERVICE_UpdateDate)
VALUES
    ('SER0000001', 'Nước suối', 'Chai 500ml trong minibar.', 'chai', 10000, '/image/default_service.jpg', 'ACTIVE', '2026-01-12 08:00:00', '2026-01-12 08:00:00'),
    ('SER0000002', 'Bữa sáng buffet', 'Tại nhà hàng tầng trệt.', 'suất', 90000, '/image/default_service.jpg', 'ACTIVE', '2026-01-12 08:05:00', '2026-01-12 08:05:00');

-- -----------------------------------------------------------
-- 10) LỊCH NHÂN SỰ & CHẤM CÔNG (FIXED: Có IsOverTime)
-- -----------------------------------------------------------
INSERT INTO WORK_SCHEDULE (WORK_SCHEDULE_ID, WORK_SCHEDULE_Date, WORK_SCHEDULE_Shift) VALUES
    ('WKS0000001', '2026-02-10', 'MORNING'),
    ('WKS0000002', '2026-02-10', 'AFTERNOON');

INSERT INTO WORK_ASSIGNMENT (WORK_ASSIGNMENT_ID, WORK_ASSIGNMENT_AssignedAt, WORK_ASSIGNMENT_EndAt, WORK_ASSIGNMENT_Note, USER_ID, WORK_SCHEDULE_ID) VALUES
    ('WAT0000001', '2026-02-10 05:45:00', '2026-02-10 12:05:00', 'Hoàn tất', 'USR0000006', 'WKS0000001'),
    ('WAT0000002', '2026-02-10 11:45:00', '2026-02-10 18:05:00', 'Hoàn tất', 'USR0000007', 'WKS0000002');

INSERT INTO HISTORY_WORK (HISTORY_WORK_ID, HISTORY_WORK_CheckinTime, HISTORY_WORK_CheckoutTime, HISTORY_WORK_IsOverTime, HISTORY_WORK_Status, USER_ID, WORK_SCHEDULE_ID) VALUES
    ('HWK0000001', '2026-02-10 06:01:00', '2026-02-10 11:58:00', FALSE, 'COMPLETED', 'USR0000006', 'WKS0000001'),
    ('HWK0000002', '2026-02-10 12:02:00', '2026-02-10 17:57:00', FALSE, 'COMPLETED', 'USR0000007', 'WKS0000002');

-- -----------------------------------------------------------
-- 11) CHÍNH SÁCH
-- -----------------------------------------------------------
INSERT INTO POLICY
    (POLICY_Number, POLICY_Name, POLICY_Content, POLICY_Subject, POLICY_CreateDate, POLICY_UpdateDate, admin_id)
VALUES
    ('POL0000001', 'Điều khoản đặt phòng', 'Quy định về khách sạn', 'CUSTOMER', '2026-01-13 08:00:00', '2026-01-13 08:00:00', 'USR0000001');

-- -----------------------------------------------------------
-- 12) RENTAL & RENTAL_DETAIL (FIXED: Giỏ hàng đa phòng)
-- -----------------------------------------------------------
-- Đơn đặt tổng
INSERT INTO RENTAL (RENTAL_ID, RENTAL_RentDate, RENTAL_Note, RENTAL_IsBooking, RENTAL_Status, CUSTOMER_ID, STAFF_ID) VALUES
    ('REN0000001', '2026-02-01 09:15:00', 'Khách công tác',        TRUE,  'COMPLETED', 'USR0000009', 'USR0000006'),
    ('REN0000002', '2026-02-02 10:20:00', 'Khách gia đình',        TRUE,  'COMPLETED', 'USR0000010', 'USR0000006'),
    ('REN0000003', '2026-02-16 09:45:00', 'Khách vãng lai walkin', FALSE, 'COMPLETED', 'USR0000013', 'USR0000007');

-- Chi tiết phòng trong Đơn
INSERT INTO RENTAL_DETAIL
    (RENTAL_DETAIL_ID, RENTAL_DETAIL_CheckinDate, RENTAL_DETAIL_LengthOfStay, RENTAL_DETAIL_ChildrenCount, RENTAL_DETAIL_AdultCount,
     RENTAL_DETAIL_UnitPrice, RENTAL_DETAIL_Status, RENTAL_ID, ROOM_ID)
VALUES
    -- Đơn 1 có 1 phòng Standard
    ('RDT0000001', '2026-02-05 08:00:00',  8, 0, 1, 200000, 'CHECKED_OUT', 'REN0000001', 'ROM0000001'),
    -- Đơn 2 có 2 phòng Superior
    ('RDT0000002', '2026-02-07 12:00:00', 24, 1, 2, 280000, 'CHECKED_OUT', 'REN0000002', 'ROM0000004'),
    ('RDT0000003', '2026-02-07 12:00:00', 24, 0, 2, 280000, 'CHECKED_OUT', 'REN0000002', 'ROM0000005'),
    -- Đơn 3 walkin 1 phòng
    ('RDT0000004', '2026-02-16 10:00:00',  4, 0, 2, 200000, 'CHECKED_OUT', 'REN0000003', 'ROM0000002');

-- -----------------------------------------------------------
-- 13) SỰ CỐ (INCIDENT) - NEW TABLE
-- -----------------------------------------------------------
INSERT INTO INCIDENT 
    (INCIDENT_ID, INCIDENT_Name, INCIDENT_OccurTime, INCIDENT_Description, INCIDENT_Responsible, INCIDENT_Money, INCIDENT_Handle, INCIDENT_Status, RENTAL_DETAIL_ID)
VALUES 
    ('ICD0000001', 'Làm vỡ cốc', '2026-02-05 10:00:00', 'Khách làm vỡ cốc thủy tinh', 'CUSTOMER', 50000, 'Khách đã đền bù', 'RESOLVED', 'RDT0000001');

-- -----------------------------------------------------------
-- 14) BILL & PAYMENT
-- -----------------------------------------------------------
INSERT INTO BILL
    (BILL_ID, BILL_CreateDate, BILL_TotalAmount, BILL_Type, BILL_ActualStayHours,
     BILL_ActualRoomAmount, BILL_EarlyCheckoutPenaltyPercent, RENTAL_ID)
VALUES
    ('BIL0000001', '2026-02-01 09:20:00',   480000, 'DEPOSIT', NULL, NULL, NULL, 'REN0000001'),
    ('BIL0000002', '2026-02-05 16:15:00',  1220000, 'FINAL',   8,  1600000, 0.00, 'REN0000001'),
    ('BIL0000003', '2026-02-02 10:25:00',  2352000, 'DEPOSIT', NULL, NULL, NULL, 'REN0000002'),
    ('BIL0000004', '2026-02-08 12:10:00',  4548000, 'FINAL',  24,  6720000, 0.00, 'REN0000002');

INSERT INTO PAYMENT (PAYMENT_ID, PAYMENT_Method, PAYMENT_Date, PAYMENT_Transaction, BILL_ID) VALUES
    ('PAY0000001', 'BANK', '2026-02-01 09:21:00', 'GD-DEP-REN0000001', 'BIL0000001'),
    ('PAY0000002', 'CASH', '2026-02-05 16:16:00', 'GD-FIN-REN0000001', 'BIL0000002');

-- -----------------------------------------------------------
-- 15) SERVICE_USAGE (FIXED: Liên kết qua RENTAL_DETAIL)
-- -----------------------------------------------------------
INSERT INTO SERVICE_USAGE
    (SERVICE_USAGE_ID, SERVICE_USAGE_Count, SERVICE_USAGE_Time, SERVICE_USAGE_UnitPrice, RENTAL_DETAIL_ID, SERVICE_ID)
VALUES
    ('SVG0000001',  1, '2026-02-05 09:00:00',  10000, 'RDT0000001', 'SER0000001'),
    ('SVG0000002',  1, '2026-02-05 09:30:00',  90000, 'RDT0000001', 'SER0000002'),
    ('SVG0000003',  2, '2026-02-08 07:00:00',  90000, 'RDT0000002', 'SER0000002');

-- -----------------------------------------------------------
-- 16) REVIEW
-- -----------------------------------------------------------
INSERT INTO REVIEW (REVIEW_ID, REVIEW_Rate, REVIEW_Description, REVIEW_UpdateDate, USER_ID) VALUES
    ('REV0000001', 5, 'Phòng sạch sẽ, thủ tục nhanh', '2026-02-06 10:00:00', 'USR0000009'),
    ('REV0000002', 4, 'Trải nghiệm tốt, ăn sáng ngon', '2026-02-08 13:00:00', 'USR0000010');

COMMIT;