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
    ('USR0000001', 'Hệ',        'Thống',    'MALE',        '1990-01-01', '001000000001', 'Việt Nam', 'admin@ithotel.vn',            '0901000001', '/image/default_avatar_customer.jpg', 'ADMIN',    '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000002', 'Ngọc',      'Lan',      'FEMALE',      '1991-02-14', '001000000002', 'Việt Nam', 'hr.manager@ithotel.vn',       '0901000002', '/image/default_avatar_customer.jpg', 'MANAGER',  '2026-01-01 08:10:00', '2026-01-01 08:10:00'),
    ('USR0000003', 'Minh',      'Khoa',     'MALE',        '1988-06-18', '001000000003', 'Việt Nam', 'operation.manager@ithotel.vn',  '0901000003', '/image/default_avatar_customer.jpg', 'MANAGER',  '2026-01-01 08:20:00', '2026-01-01 08:20:00'),
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
INSERT INTO ACCOUNT (USER_ID, USER_Password) VALUES
    ('USR0000001', 'admin123456'),
    ('USR0000002', 'manager123'),
    ('USR0000003', 'manager123'),
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
    ('AST0000001', 'ACTIVE', '2026-01-01 08:00:00', NULL, 'Tài khoản quản trị', 'USR0000001'),
    ('AST0000002', 'ACTIVE', '2026-01-01 08:10:00', NULL, 'Tài khoản quản lý',  'USR0000002'),
    ('AST0000003', 'ACTIVE', '2026-01-01 08:20:00', NULL, 'Tài khoản quản lý',  'USR0000003'),
    ('AST0000005', 'ACTIVE', '2026-01-01 08:40:00', NULL, 'Tài khoản quản lý',  'USR0000005'),
    ('AST0000006', 'ACTIVE', '2026-01-02 08:00:00', NULL, 'Tài khoản nhân viên', 'USR0000006'),
    ('AST0000007', 'ACTIVE', '2026-01-03 08:00:00', NULL, 'Tài khoản nhân viên', 'USR0000007'),
    ('AST0000008', 'ACTIVE', '2026-01-04 08:00:00', NULL, 'Tài khoản nhân viên', 'USR0000008'),
    ('AST0000009', 'ACTIVE', '2026-01-05 09:00:00', NULL, 'Tài khoản khách hàng', 'USR0000009'),
    ('AST0000010', 'ACTIVE', '2026-01-06 09:00:00', NULL, 'Tài khoản khách hàng', 'USR0000010'),
    ('AST0000011', 'ACTIVE', '2026-01-07 09:00:00', NULL, 'Tài khoản khách hàng', 'USR0000011'),
    ('AST0000012', 'ACTIVE', '2026-01-08 09:00:00', NULL, 'Tài khoản khách hàng', 'USR0000012'),
    ('AST0000013', 'ACTIVE', '2026-01-09 09:00:00', NULL, 'Tài khoản khách hàng', 'USR0000013');

-- -----------------------------------------------------------
-- 4) PHÂN QUYỀN NHÁNH ROLE
-- -----------------------------------------------------------
INSERT INTO ADMIN (USER_ID) VALUES ('USR0000001');

INSERT INTO MANAGER (USER_ID, MANAGER_JobTitle, MANAGER_WorkingStatus) VALUES
    ('USR0000002', 'HR_MANAGER', 'WORKING'),
    ('USR0000003', 'OPERATION_MANAGER', 'WORKING'),
    ('USR0000005', 'CUSTOMER_MANAGER', 'WORKING');

INSERT INTO STAFF (USER_ID, STAFF_EmploymentTime, STAFF_WorkingStatus) VALUES
    ('USR0000006', '2025-10-01 08:00:00', 'WORKING'),
    ('USR0000007', '2025-10-10 08:00:00', 'WORKING'),
    ('USR0000008', '2025-11-01 08:00:00', 'WORKING');

INSERT INTO CUSTOMER (USER_ID) VALUES
    ('USR0000009'),
    ('USR0000010'),
    ('USR0000011'),
    ('USR0000012'),
    ('USR0000013');

-- -----------------------------------------------------------
-- 5) TIER_HISTORY
-- -----------------------------------------------------------
INSERT INTO TIER_HISTORY
    (TIER_HISTORY_ID, TIER_HISTORY_StartDate, TIER_HISTORY_EndDate, TIER_HISTORY_TotalSpending, TIER_HISTORY_Reason, USER_ID, TIER_CUS_ID)
VALUES
    ('THI0000001', '2026-02-01 08:00:00', NULL,  1700000,  'Hệ thống tự ra bill, tổng chi 1.700.000 -> Đồng',        'USR0000009', 'TCR0000001'),
    ('THI0000002', '2026-02-01 08:05:00', NULL,  6900000,  'Hệ thống tự ra bill, tổng chi 6.900.000 -> Bạc',         'USR0000010', 'TCR0000002'),
    ('THI0000003', '2026-02-01 08:10:00', NULL, 20550000,  'Hệ thống tự ra bill, tổng chi 20.550.000 -> Vàng',       'USR0000011', 'TCR0000003'),
    ('THI0000004', '2026-02-01 08:15:00', NULL, 52800000,  'Hệ thống tự ra bill, tổng chi 52.800.000 -> Kim Cương',  'USR0000012', 'TCR0000004'),
    ('THI0000005', '2026-02-01 08:20:00', NULL,   880000,  'Hệ thống tự ra bill, tổng chi 880.000 -> Đồng',          'USR0000013', 'TCR0000001');

-- -----------------------------------------------------------
-- 6) TIỆN ÍCH (AMENITY)
-- -----------------------------------------------------------
INSERT INTO AMENITY (AMENITY_ID, AMENITY_Name)
VALUES
    ('AME0000001', 'Wifi tốc độ cao'),
    ('AME0000002', 'Smart TV'),
    ('AME0000003', 'Tủ lạnh mini (Minibar)'),
    ('AME0000004', 'Máy sấy tóc'),
    ('AME0000005', 'Điều hòa nhiệt độ'),
    ('AME0000006', 'Bồn tắm nằm');

-- -----------------------------------------------------------
-- 7) LOẠI PHÒNG
-- -----------------------------------------------------------
INSERT INTO ROOM_TYPE
    (ROOM_TYPE_ID, ROOM_TYPE_Name, ROOM_TYPE_Bed, ROOM_TYPE_AdultMax, ROOM_TYPE_ChildrenMax, ROOM_TYPE_Area, ROOM_TYPE_BasePrice,
     ROOM_TYPE_DepositPercent, ROOM_TYPE_Description, ROOM_TYPE_CreateDate, ROOM_TYPE_UpdateDate)
VALUES
    ('RTP0000001', 'Tiêu chuẩn', '1 Giường đơn', 2, 1, 22.50, 180000, 30, 'Phòng gọn gàng, phù hợp khách đi công tác hoặc lưu trú ngắn giờ.',         '2026-01-10 08:00:00', '2026-01-10 08:00:00'),
    ('RTP0000002', 'Superior',   '1 Giường đôi', 2, 1, 28.00, 260000, 35, 'Phòng rộng hơn, nội thất nâng cấp, phù hợp cặp đôi hoặc khách công tác.', '2026-01-10 08:05:00', '2026-01-10 08:05:00'),
    ('RTP0000003', 'Deluxe',     '2 Giường đơn', 2, 2, 36.50, 380000, 40, 'Phòng cao cấp, có khu tiếp khách nhỏ và không gian thoáng hơn.',          '2026-01-10 08:10:00', '2026-01-10 08:10:00'),
    ('RTP0000004', 'Suite',      '1 Giường King', 4, 2, 55.00, 650000, 50, 'Phòng hạng sang, diện tích lớn, phù hợp gia đình hoặc khách VIP.',        '2026-01-10 08:15:00', '2026-01-10 08:15:00');

-- -----------------------------------------------------------
-- 8) TIỆN ÍCH PHÒNG (ROOM_AMENITY) - LIÊN KẾT NHIỀU NHIỀU
-- -----------------------------------------------------------
INSERT INTO ROOM_AMENITY (ROOM_TYPE_ID, AMENITY_ID)
VALUES
    -- Tiêu chuẩn (Wifi, Điều hòa, Máy sấy tóc)
    ('RTP0000001', 'AME0000001'),
    ('RTP0000001', 'AME0000004'),
    ('RTP0000001', 'AME0000005'),
    
    -- Superior (+ Smart TV)
    ('RTP0000002', 'AME0000001'),
    ('RTP0000002', 'AME0000002'),
    ('RTP0000002', 'AME0000004'),
    ('RTP0000002', 'AME0000005'),
    
    -- Deluxe (+ Minibar)
    ('RTP0000003', 'AME0000001'),
    ('RTP0000003', 'AME0000002'),
    ('RTP0000003', 'AME0000003'),
    ('RTP0000003', 'AME0000004'),
    ('RTP0000003', 'AME0000005'),
    
    -- Suite (+ Bồn tắm nằm)
    ('RTP0000004', 'AME0000001'),
    ('RTP0000004', 'AME0000002'),
    ('RTP0000004', 'AME0000003'),
    ('RTP0000004', 'AME0000004'),
    ('RTP0000004', 'AME0000005'),
    ('RTP0000004', 'AME0000006');

-- -----------------------------------------------------------
-- 9) PHÒNG
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

-- -----------------------------------------------------------
-- 10) ẢNH PHÒNG
-- -----------------------------------------------------------
INSERT INTO ROOM_IMAGE
    (ROOM_IMAGE_ID, ROOM_IMAGE_Path, ROOM_IMAGE_IsPrimary, ROOM_ID)
VALUES
    ('RIM0000001',  '/image/default_room.jpg', true,  'ROM0000001'),
    ('RIM0000002',  '/image/default_room.jpg', true,  'ROM0000002'),
    ('RIM0000003',  '/image/default_room.jpg', true,  'ROM0000003'),
    ('RIM0000004',  '/image/default_room.jpg', true,  'ROM0000004'),
    ('RIM0000005',  '/image/default_room.jpg', true,  'ROM0000005'),
    ('RIM0000006',  '/image/default_room.jpg', true,  'ROM0000006'),
    ('RIM0000007',  'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80', true,  'ROM0000007'),
    ('RIM0000008',  '/image/default_room.jpg', true,  'ROM0000008'),
    ('RIM0000009',  '/image/default_room.jpg', true,  'ROM0000009'),
    ('RIM0000010',  'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', true,  'ROM0000010'),
    ('RIM0000011',  '/image/default_room.jpg', true,  'ROM0000011'),
    ('RIM0000012',  '/image/default_room.jpg', true,  'ROM0000012'),
    ('RIM0000013',  'https://images.unsplash.com/photo-1505692952047-1a78307da8f2?auto=format&fit=crop&w=1200&q=80', false, 'ROM0000007'),
    ('RIM0000014',  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', false, 'ROM0000007'),
    ('RIM0000015',  'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', false, 'ROM0000007'),
    ('RIM0000016',  'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80', false, 'ROM0000010'),
    ('RIM0000017',  'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80', false, 'ROM0000010'),
    ('RIM0000018',  'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1200&q=80', false, 'ROM0000010');

-- -----------------------------------------------------------
-- 11) GIÁ PHỤ THU / KỲ ÁP DỤNG
-- -----------------------------------------------------------
INSERT INTO PRICE_RATE
    (PRICE_RATE_ID, PRICE_RATE_EventName, PRICE_RATE_SurchargeAmount, PRICE_RATE_CreateDate, PRICE_RATE_UpdateDate)
VALUES
    ('PRC0000001', 'Phụ thu loại Tiêu chuẩn 2026', 20000, '2026-01-11 08:00:00', '2026-01-11 08:00:00'),
    ('PRC0000002', 'Phụ thu loại Superior 2026',   20000, '2026-01-11 08:05:00', '2026-01-11 08:05:00'),
    ('PRC0000003', 'Phụ thu loại Deluxe 2026',     40000, '2026-01-11 08:10:00', '2026-01-11 08:10:00'),
    ('PRC0000004', 'Phụ thu loại Suite 2026',      70000, '2026-01-11 08:15:00', '2026-01-11 08:15:00');

INSERT INTO APPLIED_PERIOD
    (APPLIED_PERIOD_ID, APPLIED_PERIOD_StartDate, APPLIED_PERIOD_EndDate, PRICE_RATE_ID, ROOM_TYPE_ID)
VALUES
    ('APD0000001', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000001', 'RTP0000001'),
    ('APD0000002', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000002', 'RTP0000002'),
    ('APD0000003', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000003', 'RTP0000003'),
    ('APD0000004', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000004', 'RTP0000004');

-- -----------------------------------------------------------
-- 12) DỊCH VỤ
-- -----------------------------------------------------------
INSERT INTO SERVICE
    (SERVICE_ID, SERVICE_Name, SERVICE_Description, SERVICE_Unit, SERVICE_BasePrice, SERVICE_ImagePath, SERVICE_Status, SERVICE_CreateDate, SERVICE_UpdateDate)
VALUES
    ('SER0000001', 'Nước suối',           'Nước suối chai 500ml đặt trong minibar.',               'chai',   10000,  '/image/default_service.jpg', 'ACTIVE',    '2026-01-12 08:00:00', '2026-01-12 08:00:00'),
    ('SER0000002', 'Bữa sáng buffet',     'Suất buffet sáng tại nhà hàng tầng trệt.',              'suất',   90000,  '/image/default_service.jpg', 'ACTIVE',    '2026-01-12 08:05:00', '2026-01-12 08:05:00'),
    ('SER0000003', 'Giặt ủi',             'Giặt và ủi quần áo trong ngày.',                        'lần',    60000,  '/image/default_service.jpg', 'ACTIVE',    '2026-01-12 08:10:00', '2026-01-12 08:10:00'),
    ('SER0000004', 'Đưa đón sân bay',     'Xe đưa đón sân bay một chiều trong nội thành.',         'chuyến', 250000, '/image/default_service.jpg', 'ACTIVE',    '2026-01-12 08:15:00', '2026-01-12 08:15:00'),
    ('SER0000005', 'Phụ thu nệm phụ',     'Bổ sung nệm phụ cho khách ở ghép.',                     'cái',    180000, '/image/default_service.jpg', 'ACTIVE',    '2026-01-12 08:20:00', '2026-01-12 08:20:00'),
    ('SER0000006', 'Dọn phòng ngoài giờ', 'Dịch vụ vệ sinh phòng ngoài khung giờ tiêu chuẩn.',     'lần',    150000, '/image/default_service.jpg', 'SUSPENDED', '2026-01-12 08:25:00', '2026-01-12 08:25:00');

-- -----------------------------------------------------------
-- 13) LỊCH NHÂN SỰ
-- -----------------------------------------------------------
INSERT INTO WORK_SCHEDULE
    (WORK_SCHEDULE_ID, WORK_SCHEDULE_Date, WORK_SCHEDULE_Shift)
VALUES
    ('WKS0000001', '2026-02-10', 'MORNING'),
    ('WKS0000002', '2026-02-10', 'AFTERNOON'),
    ('WKS0000003', '2026-02-11', 'MORNING'),
    ('WKS0000004', '2026-02-11', 'EVENING'),
    ('WKS0000005', '2026-02-12', 'MORNING'),
    ('WKS0000006', '2026-02-12', 'AFTERNOON'),
    ('WKS0000007', '2026-02-13', 'MORNING'),
    ('WKS0000008', '2026-02-13', 'EVENING');

INSERT INTO WORK_ASSIGNMENT
    (WORK_ASSIGNMENT_ID, WORK_ASSIGNMENT_AssignedAt, WORK_ASSIGNMENT_EndAt, WORK_ASSIGNMENT_Note, USER_ID, WORK_SCHEDULE_ID)
VALUES
    ('WAT0000001', '2026-02-10 05:45:00', '2026-02-10 12:05:00', 'Ca đã hoàn tất', 'USR0000006', 'WKS0000001'),
    ('WAT0000002', '2026-02-10 11:45:00', '2026-02-10 18:05:00', 'Ca đã hoàn tất', 'USR0000007', 'WKS0000002'),
    ('WAT0000003', '2026-02-11 05:45:00', '2026-02-11 12:05:00', 'Ca đã hoàn tất', 'USR0000008', 'WKS0000003'),
    ('WAT0000004', '2026-02-11 17:45:00', '2026-02-12 00:05:00', 'Ca đã hoàn tất', 'USR0000006', 'WKS0000004'),
    ('WAT0000005', '2026-02-12 05:45:00', '2026-02-12 12:05:00', 'Ca đã hoàn tất', 'USR0000007', 'WKS0000005'),
    ('WAT0000006', '2026-02-12 11:45:00', '2026-02-12 18:05:00', 'Ca đã hoàn tất', 'USR0000008', 'WKS0000006'),
    ('WAT0000007', '2026-02-13 05:45:00', '2026-02-13 12:05:00', 'Ca đã hoàn tất', 'USR0000006', 'WKS0000007'),
    ('WAT0000008', '2026-02-13 17:45:00', '2026-02-14 00:05:00', 'Ca đã hoàn tất', 'USR0000007', 'WKS0000008');

INSERT INTO HISTORY_WORK
    (HISTORY_WORK_ID, HISTORY_WORK_CheckinTime, HISTORY_WORK_CheckoutTime, HISTORY_WORK_Status, USER_ID, WORK_SCHEDULE_ID)
VALUES
    ('HWK0000001', '2026-02-10 06:01:00', '2026-02-10 11:58:00', 'COMPLETED', 'USR0000006', 'WKS0000001'),
    ('HWK0000002', '2026-02-10 12:02:00', '2026-02-10 17:57:00', 'COMPLETED', 'USR0000007', 'WKS0000002'),
    ('HWK0000003', '2026-02-11 06:00:00', '2026-02-11 11:54:00', 'COMPLETED', 'USR0000008', 'WKS0000003'),
    ('HWK0000004', '2026-02-11 18:03:00', '2026-02-11 23:56:00', 'COMPLETED', 'USR0000006', 'WKS0000004'),
    ('HWK0000005', '2026-02-12 06:05:00', '2026-02-12 11:59:00', 'COMPLETED', 'USR0000007', 'WKS0000005'),
    ('HWK0000006', '2026-02-12 12:01:00', '2026-02-12 17:55:00', 'COMPLETED', 'USR0000008', 'WKS0000006'),
    ('HWK0000007', '2026-02-13 06:00:00', '2026-02-13 11:57:00', 'COMPLETED', 'USR0000006', 'WKS0000007'),
    ('HWK0000008', '2026-02-13 18:00:00', '2026-02-13 23:58:00', 'COMPLETED', 'USR0000007', 'WKS0000008');

-- -----------------------------------------------------------
-- 14) RENTAL 
-- -----------------------------------------------------------
INSERT INTO RENTAL
    (RENTAL_ID, RENTAL_RentDate, RENTAL_Note, RENTAL_IsBooking, RENTAL_Status, CUSTOMER_ID)
VALUES
    ('REN0000001', '2026-02-01 09:15:00', 'Khách đi công tác ngắn ngày.',        TRUE,  'COMPLETED', 'USR0000009'),
    ('REN0000002', '2026-02-02 10:20:00', 'Khách ở cặp đôi, yêu cầu yên tĩnh.',  TRUE,  'COMPLETED', 'USR0000010'),
    ('REN0000003', '2026-02-05 11:10:00', 'Khách gia đình nhỏ.',                 TRUE,  'COMPLETED', 'USR0000011'),
    ('REN0000004', '2026-02-08 15:30:00', 'Khách doanh nhân, cần phòng rộng.',   TRUE,  'COMPLETED', 'USR0000012'),
    ('REN0000005', '2026-02-16 09:45:00', 'Thuê ngắn giờ, không dùng dịch vụ.',  FALSE, 'COMPLETED', 'USR0000013');
-- -----------------------------------------------------------
-- 14.1) RENTAL_DETAIL 
-- -----------------------------------------------------------
INSERT INTO RENTAL_DETAIL
    (RENTAL_DETAIL_ID, RENTAL_DETAIL_CheckinDate, RENTAL_DETAIL_LengthOfStay, RENTAL_DETAIL_ChildrenCount,
     RENTAL_DETAIL_AdultCount, RENTAL_DETAIL_UnitPrice, RENTAL_ID, ROOM_ID)
VALUES
    ('RDT0000001', '2026-02-05 08:00:00',  8, 0, 1, 200000, 'REN0000001', 'ROM0000001'),
    ('RDT0000002', '2026-02-07 12:00:00', 24, 0, 2, 280000, 'REN0000002', 'ROM0000004'),
    ('RDT0000003', '2026-02-10 14:00:00', 48, 1, 2, 420000, 'REN0000003', 'ROM0000007'),
    ('RDT0000004', '2026-02-14 09:00:00', 72, 2, 2, 720000, 'REN0000004', 'ROM0000010'),
    ('RDT0000005', '2026-02-16 10:00:00',  4, 0, 1, 200000, 'REN0000005', 'ROM0000002');

-- -----------------------------------------------------------
-- 15) BILL
-- -----------------------------------------------------------
INSERT INTO BILL
    (BILL_ID, BILL_CreateDate, BILL_TotalAmount, BILL_Type, BILL_ActualStayHours,
     BILL_ActualRoomAmount, BILL_EarlyCheckoutPenaltyPercent, RENTAL_ID)
VALUES
    ('BIL0000001', '2026-02-01 09:20:00',   480000, 'DEPOSIT', 0, 0, 0.00, 'REN0000001'),
    ('BIL0000002', '2026-02-05 16:15:00',  1220000, 'FINAL',   8,  1600000, 0.00, 'REN0000001'),
    ('BIL0000003', '2026-02-02 10:25:00',  2352000, 'DEPOSIT', 0, 0, 0.00, 'REN0000002'),
    ('BIL0000004', '2026-02-08 12:10:00',  4548000, 'FINAL',  24,  6720000, 0.00, 'REN0000002'),
    ('BIL0000005', '2026-02-05 11:20:00',  8064000, 'DEPOSIT', 0, 0, 0.00, 'REN0000003'),
    ('BIL0000006', '2026-02-12 14:10:00', 12486000, 'FINAL',  48, 20160000, 0.00, 'REN0000003'),
    ('BIL0000007', '2026-02-08 15:35:00', 25920000, 'DEPOSIT', 0, 0, 0.00, 'REN0000004'),
    ('BIL0000008', '2026-02-17 09:15:00', 26880000, 'FINAL',  72, 51840000, 0.00, 'REN0000004'),
    ('BIL0000009', '2026-02-16 09:50:00',   264000, 'DEPOSIT', 0, 0, 0.00, 'REN0000005'),
    ('BIL0000010', '2026-02-16 14:15:00',   616000, 'FINAL',   4,   880000, 0.00, 'REN0000005');

-- -----------------------------------------------------------
-- 16) PAYMENT
-- -----------------------------------------------------------
INSERT INTO PAYMENT
    (PAYMENT_ID, PAYMENT_Method, PAYMENT_Date, PAYMENT_Transaction, BILL_ID)
VALUES
    ('PAY0000001', 'BANK', '2026-02-01 09:21:00', 'GD-DEP-REN0000001', 'BIL0000001'),
    ('PAY0000002', 'CASH', '2026-02-05 16:16:00', 'GD-FIN-REN0000001', 'BIL0000002'),
    ('PAY0000003', 'BANK', '2026-02-02 10:26:00', 'GD-DEP-REN0000002', 'BIL0000003'),
    ('PAY0000004', 'BANK', '2026-02-08 12:11:00', 'GD-FIN-REN0000002', 'BIL0000004'),
    ('PAY0000005', 'BANK', '2026-02-05 11:21:00', 'GD-DEP-REN0000003', 'BIL0000005'),
    ('PAY0000006', 'CASH', '2026-02-12 14:11:00', 'GD-FIN-REN0000003', 'BIL0000006'),
    ('PAY0000007', 'BANK', '2026-02-08 15:36:00', 'GD-DEP-REN0000004', 'BIL0000007'),
    ('PAY0000008', 'BANK', '2026-02-17 09:16:00', 'GD-FIN-REN0000004', 'BIL0000008'),
    ('PAY0000009', 'CASH', '2026-02-16 09:51:00', 'GD-DEP-REN0000005', 'BIL0000009'),
    ('PAY0000010', 'CASH', '2026-02-16 14:16:00', 'GD-FIN-REN0000005', 'BIL0000010');

-- -----------------------------------------------------------
-- 17) SERVICE_USAGE
-- -----------------------------------------------------------
INSERT INTO SERVICE_USAGE
    (SERVICE_USAGE_ID, SERVICE_USAGE_Count, SERVICE_USAGE_Time, SERVICE_USAGE_UnitPrice, RENTAL_DETAIL_ID, SERVICE_ID)
VALUES
    ('SVG0000001',  1, '2026-02-05 09:00:00',  10000, 'RDT0000001', 'SER0000001'),
    ('SVG0000002',  1, '2026-02-05 09:30:00',  90000, 'RDT0000001', 'SER0000002'),
    ('SVG0000003',  1, '2026-02-08 07:00:00',  90000, 'RDT0000002', 'SER0000002'),
    ('SVG0000004',  1, '2026-02-08 08:00:00',  90000, 'RDT0000002', 'SER0000002'),
    ('SVG0000005',  3, '2026-02-11 08:00:00',  10000, 'RDT0000003', 'SER0000001'),
    ('SVG0000006',  1, '2026-02-11 12:30:00',  60000, 'RDT0000003', 'SER0000003'),
    ('SVG0000007',  1, '2026-02-12 07:30:00',  90000, 'RDT0000003', 'SER0000002'),
    ('SVG0000008',  1, '2026-02-12 10:00:00', 250000, 'RDT0000003', 'SER0000004'),
    ('SVG0000009',  2, '2026-02-15 07:00:00',  90000, 'RDT0000004', 'SER0000002'),
    ('SVG000010',  1, '2026-02-15 11:00:00',  60000, 'RDT0000004', 'SER0000003'),
    ('SVG000011',  1, '2026-02-15 13:00:00', 250000, 'RDT0000004', 'SER0000004'),
    ('SVG000012',  3, '2026-02-16 09:00:00', 180000, 'RDT0000004', 'SER0000005');
	
-- -----------------------------------------------------------
-- 18) REVIEW
-- -----------------------------------------------------------
INSERT INTO REVIEW
    (REVIEW_ID, REVIEW_Rate, REVIEW_Description, REVIEW_UpdateDate, USER_ID)
VALUES
    ('REV0000001', 5, 'Phòng sạch sẽ, thủ tục nhanh, nhân viên hỗ trợ rất nhiệt tình.',                '2026-02-06 10:00:00', 'USR0000009'),
    ('REV0000002', 4, 'Trải nghiệm tốt, phòng yên tĩnh, bữa sáng ổn. Sẽ cân nhắc quay lại.',           '2026-02-08 13:00:00', 'USR0000010'),
    ('REV0000003', 5, 'Không gian đẹp, dịch vụ đưa đón sân bay rất tiện, gia đình tôi khá hài lòng.',  '2026-02-12 16:00:00', 'USR0000011'),
    ('REV0000004', 5, 'Phòng rộng, tiện nghi tốt, quy trình thanh toán rõ ràng và chuyên nghiệp.',     '2026-02-17 11:00:00', 'USR0000012'),
    ('REV0000005', 4, 'Thuê ngắn giờ nhưng vẫn được hỗ trợ chu đáo, phòng gọn gàng và sạch sẽ.',       '2026-02-16 18:00:00', 'USR0000013');



INSERT INTO INCIDENT 
    (INCIDENT_ID, INCIDENT_Name, INCIDENT_OccurTime, INCIDENT_Description, 
     INCIDENT_Responsible, INCIDENT_Money, INCIDENT_Handle, INCIDENT_Status, RENTAL_DETAIL_ID, CREATED_BY)
VALUES 
    ('ICD0000001', 'Hỏng máy sấy tóc', '2026-02-06 14:30:00', 'Khách làm rơi vỡ máy sấy tóc trong phòng tắm', 'CUSTOMER', 350000, 'Đền bù tiền mặt', 'RESOLVED', 'RDT0000001', 'USR0000006'),
    ('ICD0000002', 'Rò rỉ nước', '2026-02-15 08:00:00', 'Ống nước bồn rửa mặt bị rỉ', 'HOTEL', 0, 'Sửa chữa bảo trì', 'PROCESSING', 'RDT0000004', 'USR0000007'),
    ('ICD0000003', 'Hỏng điều hòa', '2026-02-16 09:30:00', 'Khách báo điều hòa không mát, có tiếng ồn lớn', NULL, 0, NULL, 'PROCESSING', 'RDT0000005', 'USR0000006');

COMMIT;