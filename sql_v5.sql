ROLLBACK;
BEGIN;

-- ===========================================================
-- 1) HẠNG KHÁCH HÀNG & TIỆN ÍCH (MỞ RỘNG 18 TIỆN ÍCH)
-- ===========================================================
INSERT INTO TIER_CUSTOMER (TIER_CUS_ID, TIER_CUS_Name, TIER_CUS_Condition, TIER_CUS_Benefit, TIER_CUS_Discount) VALUES
    ('TCR0000001', 'Đồng',       0,         'Hạng mặc định cho khách mới, tích lũy chi tiêu tự động',                0.00),
    ('TCR0000002', 'Bạc',        5000000,   'Giảm giá cơ bản, ưu tiên hỗ trợ nhận phòng khi cần',                    5.00),
    ('TCR0000003', 'Vàng',       15000000,  'Giảm giá tốt hơn, ưu tiên xử lý nhanh và hỗ trợ linh hoạt',            10.00),
    ('TCR0000004', 'Bạch kim',   30000000,  'Ưu tiên nâng hạng phòng miễn phí tùy tình trạng, hỗ trợ VIP',          15.00),
    ('TCR0000005', 'Kim Cương',  60000000,  'Đặc quyền thượng lưu cao nhất, phục vụ riêng biệt 24/7',               20.00);

INSERT INTO AMENITY (AMENITY_ID, AMENITY_Name) VALUES
    ('AME0000001', 'Wifi 6 tốc độ cao'),
    ('AME0000002', 'Smart TV 4K 65 inch (Netflix)'),
    ('AME0000003', 'Tủ lạnh mini (Minibar)'),
    ('AME0000004', 'Máy sấy tóc Dyson'),
    ('AME0000005', 'Điều hòa Inverter 2 chiều'),
    ('AME0000006', 'Bồn tắm nằm ngâm hoa hồng'),
    ('AME0000007', 'Ban công Panorama toàn cảnh'),
    ('AME0000008', 'Két sắt vân tay an toàn'),
    ('AME0000009', 'Ghế tình yêu Tantra cao cấp'),
    ('AME0000010', 'Bàn làm việc Tổng tài & Ghế công thái học'),
    ('AME0000011', 'Bồn tắm sục Jacuzzi đôi'),
    ('AME0000012', 'Máy pha cà phê viên nén Espresso'),
    ('AME0000013', 'Hệ thống âm thanh vòm Bose Bluetooth'),
    ('AME0000014', 'Đèn LED Ambient đổi màu lãng mạn'),
    ('AME0000015', 'Rèm cửa tự động Smarthome'),
    ('AME0000016', 'Quầy Bar Mini & Tủ ướp rượu vang'),
    ('AME0000017', 'Phòng xông hơi đá muối Himalaya'),
    ('AME0000018', 'Ghế Massage toàn thân thương gia');

-- ===========================================================
-- 2) USERS, ACCOUNT, ROLE (GIỮ NGUYÊN CÁC TÀI KHOẢN)
-- ===========================================================
INSERT INTO USERS (USER_ID, USER_FirstName, USER_LastName, USER_Sex, USER_DateOfBirth, USER_PID, USER_Nationality, USER_Email, USER_PhoneNumber, USER_Avatar, USER_Role, USER_CreateDate, USER_UpdateDate) VALUES
    ('USR0000001', 'Hệ', 'Thống', 'MALE', '1990-01-01', '001000000001', 'Việt Nam', 'admin@ithotel.vn', '0901000001', '/image/default_avatar_customer.jpg', 'ADMIN', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000002', 'Ngọc', 'Lan', 'FEMALE', '1991-02-14', '001000000002', 'Việt Nam', 'hr@ithotel.vn', '0901000002', '/image/default_avatar_customer.jpg', 'MANAGER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000003', 'Minh', 'Khoa', 'MALE', '1988-06-18', '001000000003', 'Việt Nam', 'op@ithotel.vn', '0901000003', '/image/default_avatar_customer.jpg', 'MANAGER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000005', 'Quốc', 'Bảo', 'MALE', '1993-03-05', '001000000005', 'Việt Nam', 'cus@ithotel.vn', '0901000005', '/image/default_avatar_customer.jpg', 'MANAGER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000006', 'Mai', 'Anh', 'FEMALE', '1998-04-11', '001000000006', 'Việt Nam', 'staff1@ithotel.vn', '0901000006', '/image/default_avatar_customer.jpg', 'STAFF', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000007', 'Hoàng', 'Nam', 'MALE', '1997-07-21', '001000000007', 'Việt Nam', 'staff2@ithotel.vn', '0901000007', '/image/default_avatar_customer.jpg', 'STAFF', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000008', 'Phương', 'Linh', 'FEMALE', '1999-10-09', '001000000008', 'Việt Nam', 'staff3@ithotel.vn', '0901000008', '/image/default_avatar_customer.jpg', 'STAFF', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    -- 10 Khách hàng
    ('USR0000009', 'Gia', 'Hân', 'FEMALE', '2000-02-12', '001000000009', 'Việt Nam', 'khach1@gmail.com', '0901000009', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000010', 'Tuấn', 'Kiệt', 'MALE', '1996-11-23', '001000000010', 'Việt Nam', 'khach2@gmail.com', '0901000010', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000011', 'Khánh', 'Vy', 'FEMALE', '1995-08-15', '001000000011', 'Việt Nam', 'khach3@gmail.com', '0901000011', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000012', 'Đức', 'Phát', 'MALE', '1994-12-01', '001000000012', 'Việt Nam', 'khach4@gmail.com', '0901000012', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000013', 'Thanh', 'Trúc', 'FEMALE', '2001-06-30', '001000000013', 'Việt Nam', 'khach5@gmail.com', '0901000013', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000014', 'Bảo', 'Ngọc', 'FEMALE', '1992-05-15', '001000000014', 'Việt Nam', 'khach6@gmail.com', '0901000014', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000015', 'Quang', 'Hải', 'MALE', '1997-04-12', '001000000015', 'Việt Nam', 'khach7@gmail.com', '0901000015', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000016', 'Lan', 'Hương', 'FEMALE', '1985-09-09', '001000000016', 'Việt Nam', 'khach8@gmail.com', '0901000016', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000017', 'Quốc', 'Trường', 'MALE', '1990-12-25', '001000000017', 'Việt Nam', 'khach9@gmail.com', '0901000017', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('USR0000018', 'Diễm', 'My', 'FEMALE', '1999-07-07', '001000000018', 'Việt Nam', 'khach10@gmail.com', '0901000018', '/image/default_avatar_customer.jpg', 'CUSTOMER', '2026-01-01 08:00:00', '2026-01-01 08:00:00');

INSERT INTO ACCOUNT (USER_ID, USER_Password) VALUES
    ('USR0000001', 'admin123456'), ('USR0000002', 'manager123'), ('USR0000003', 'manager123'), ('USR0000005', 'manager123'), 
    ('USR0000006', 'staff123'), ('USR0000007', 'staff123'), ('USR0000008', 'staff123'),
    ('USR0000009', 'customer123'), ('USR0000010', 'customer123'), ('USR0000011', 'customer123'), ('USR0000012', 'customer123'), ('USR0000013', 'customer123'),
    ('USR0000014', 'customer123'), ('USR0000015', 'customer123'), ('USR0000016', 'customer123'), ('USR0000017', 'customer123'), ('USR0000018', 'customer123');

INSERT INTO ACCOUNT_STATUS (ACCOUNT_STATUS_ID, ACCOUNT_STATUS_Name, ACCOUNT_STATUS_StartTime, ACCOUNT_STATUS_Reason, USER_ID) VALUES
    ('AST0000001', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000001'),
    ('AST0000002', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000002'),
    ('AST0000003', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000003'),
    ('AST0000005', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000005'),
    ('AST0000006', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000006'),
    ('AST0000007', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000007'),
    ('AST0000008', 'ACTIVE', '2026-01-01 08:00:00', 'Khởi tạo hệ thống', 'USR0000008'),
    ('AST0000009', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000009'),
    ('AST0000010', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000010'),
    ('AST0000011', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000011'),
    ('AST0000012', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000012'),
    ('AST0000013', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000013'),
    ('AST0000014', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000014'),
    ('AST0000015', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000015'),
    ('AST0000016', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000016'),
    ('AST0000017', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000017'),
    ('AST0000018', 'ACTIVE', '2026-01-01 08:00:00', 'Kích hoạt tài khoản', 'USR0000018');

INSERT INTO ADMIN (USER_ID) VALUES ('USR0000001');
INSERT INTO MANAGER (USER_ID, MANAGER_JobTitle, MANAGER_WorkingStatus) VALUES
    ('USR0000002', 'HR_MANAGER', 'WORKING'),
    ('USR0000003', 'OPERATION_MANAGER', 'WORKING'),
    ('USR0000005', 'CUSTOMER_MANAGER', 'WORKING');
INSERT INTO STAFF (USER_ID, STAFF_EmploymentTime, STAFF_WorkingStatus) VALUES
    ('USR0000006', '2025-10-01 08:00:00', 'WORKING'),
    ('USR0000007', '2025-10-01 08:00:00', 'WORKING'),
    ('USR0000008', '2025-10-01 08:00:00', 'WORKING');
INSERT INTO CUSTOMER (USER_ID) VALUES
    ('USR0000009'), ('USR0000010'), ('USR0000011'), ('USR0000012'), ('USR0000013'),
    ('USR0000014'), ('USR0000015'), ('USR0000016'), ('USR0000017'), ('USR0000018');

-- ===========================================================
-- 3) ROOM TYPE, ROOM & AMENITY (ĐA DẠNG LOẠI PHÒNG SANG TRỌNG)
-- ===========================================================
INSERT INTO ROOM_TYPE (ROOM_TYPE_ID, ROOM_TYPE_Name, ROOM_TYPE_Bed, ROOM_TYPE_AdultMax, ROOM_TYPE_ChildrenMax, ROOM_TYPE_Area, ROOM_TYPE_BasePrice, ROOM_TYPE_DailyPrice, ROOM_TYPE_DepositPercent, ROOM_TYPE_Description, ROOM_TYPE_CreateDate, ROOM_TYPE_UpdateDate) VALUES
    ('RTP0000001', 'Standard Elegance (Tiêu chuẩn)', '1 Giường Queen', 2, 1, 24.00, 150000, 650000, 30,
     'Không gian tối giản hiện đại với tông màu ấm áp, ánh sáng tự nhiên cùng đầy đủ tiện nghi thiết yếu cho kỳ nghỉ gọn gàng, thư thái.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000002', 'Superior City View', '1 Giường Đôi Lớn', 2, 1, 30.00, 220000, 950000, 30,
     'Tầm nhìn thoáng đãng hướng phố thị sôi động, nội thất gỗ óc chó sang trọng kết hợp cùng hệ thống cách âm tiêu chuẩn 5 sao.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000003', 'Deluxe Twin Retreat', '2 Giường Đơn Lớn', 2, 2, 38.00, 320000, 1350000, 35,
     'Thiết kế rộng rãi theo phong cách Indochine đương đại, tích hợp bồn tắm nằm thư giãn và ban công đón gió mát lành.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000004', 'Sweet Couple - Cặp đôi', '1 Giường Tròn King Size', 2, 0, 42.00, 420000, 1750000, 40,
     'Không gian lãng mạn tuyệt đối thiết kế riêng cho các cặp đôi với đèn LED Ambient đổi màu, bồn tắm sục Jacuzzi đôi, rèm tự động và ghế tình yêu Tantra.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000005', 'CEO & Secretary Luxury', '1 Giường King + 1 Sofa Bed VIP', 3, 0, 62.00, 680000, 2850000, 45,
     'Đẳng cấp thương gia thượng lưu: Phân khu bàn làm việc Tổng tài sang trọng, quầy Bar rượu vang, máy pha Espresso, loa Bose và phòng tắm kính tràn viền.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000006', 'Executive Ocean Suite', '1 Giường Super King', 4, 2, 68.00, 750000, 3200000, 50,
     'Phòng Suite hạng sang với phòng khách tách biệt, tầm nhìn Panorama đắt giá và dịch vụ trà chiều thượng hạng dành cho giới tinh hoa.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000007', 'Royal Family Residence', '2 Giường Super King', 4, 4, 85.00, 950000, 4200000, 50,
     'Tổ ấm nghỉ dưỡng hoàng gia dành cho gia đình hoặc nhóm bạn với 2 phòng ngủ thông nhau, khu sinh hoạt chung ấm cúng và tiện nghi giải trí đỉnh cao.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00'),

    ('RTP0000008', 'Presidential Sky Penthouse', '3 Giường Hoàng Gia', 6, 2, 150.00, 2200000, 9500000, 50,
     'Tuyệt tác kiến trúc tầng thượng xa hoa bậc nhất ITHotel: Tích hợp phòng xông hơi đá muối Himalaya, Jacuzzi ngoài trời, ghế massage và quầy Bar riêng biệt.',
     '2026-01-01 08:00:00', '2026-01-01 08:00:00');

-- Nối đầy đủ từng loại phòng với các tiện ích tương xứng
INSERT INTO ROOM_AMENITY (ROOM_TYPE_ID, AMENITY_ID) VALUES
    -- RTP0000001: Standard Elegance
    ('RTP0000001', 'AME0000001'), ('RTP0000001', 'AME0000002'), ('RTP0000001', 'AME0000004'), ('RTP0000001', 'AME0000005'),
    -- RTP0000002: Superior City View
    ('RTP0000002', 'AME0000001'), ('RTP0000002', 'AME0000002'), ('RTP0000002', 'AME0000003'), ('RTP0000002', 'AME0000004'), ('RTP0000002', 'AME0000005'), ('RTP0000002', 'AME0000008'),
    -- RTP0000003: Deluxe Twin Retreat
    ('RTP0000003', 'AME0000001'), ('RTP0000003', 'AME0000002'), ('RTP0000003', 'AME0000003'), ('RTP0000003', 'AME0000004'), ('RTP0000003', 'AME0000005'), ('RTP0000003', 'AME0000006'), ('RTP0000003', 'AME0000007'), ('RTP0000003', 'AME0000008'),
    -- RTP0000004: Sweet Couple - Cặp đôi Mường Thanh
    ('RTP0000004', 'AME0000001'), ('RTP0000004', 'AME0000002'), ('RTP0000004', 'AME0000003'), ('RTP0000004', 'AME0000005'), ('RTP0000004', 'AME0000006'), ('RTP0000004', 'AME0000009'), ('RTP0000004', 'AME0000011'), ('RTP0000004', 'AME0000013'), ('RTP0000004', 'AME0000014'), ('RTP0000004', 'AME0000015'),
    -- RTP0000005: CEO & Secretary Luxury (Tổng Tài & Thư Ký)
    ('RTP0000005', 'AME0000001'), ('RTP0000005', 'AME0000002'), ('RTP0000005', 'AME0000005'), ('RTP0000005', 'AME0000008'), ('RTP0000005', 'AME0000009'), ('RTP0000005', 'AME0000010'), ('RTP0000005', 'AME0000011'), ('RTP0000005', 'AME0000012'), ('RTP0000005', 'AME0000013'), ('RTP0000005', 'AME0000015'), ('RTP0000005', 'AME0000016'), ('RTP0000005', 'AME0000018'),
    -- RTP0000006: Executive Ocean Suite
    ('RTP0000006', 'AME0000001'), ('RTP0000006', 'AME0000002'), ('RTP0000006', 'AME0000003'), ('RTP0000006', 'AME0000005'), ('RTP0000006', 'AME0000006'), ('RTP0000006', 'AME0000007'), ('RTP0000006', 'AME0000008'), ('RTP0000006', 'AME0000012'), ('RTP0000006', 'AME0000015'), ('RTP0000006', 'AME0000016'),
    -- RTP0000007: Royal Family Residence
    ('RTP0000007', 'AME0000001'), ('RTP0000007', 'AME0000002'), ('RTP0000007', 'AME0000003'), ('RTP0000007', 'AME0000004'), ('RTP0000007', 'AME0000005'), ('RTP0000007', 'AME0000006'), ('RTP0000007', 'AME0000007'), ('RTP0000007', 'AME0000008'), ('RTP0000007', 'AME0000012'), ('RTP0000007', 'AME0000018'),
    -- RTP0000008: Presidential Sky Penthouse
    ('RTP0000008', 'AME0000001'), ('RTP0000008', 'AME0000002'), ('RTP0000008', 'AME0000003'), ('RTP0000008', 'AME0000004'), ('RTP0000008', 'AME0000005'), ('RTP0000008', 'AME0000007'), ('RTP0000008', 'AME0000008'), ('RTP0000008', 'AME0000009'), ('RTP0000008', 'AME0000010'), ('RTP0000008', 'AME0000011'), ('RTP0000008', 'AME0000012'), ('RTP0000008', 'AME0000013'), ('RTP0000008', 'AME0000015'), ('RTP0000008', 'AME0000016'), ('RTP0000008', 'AME0000017'), ('RTP0000008', 'AME0000018');

INSERT INTO ROOM (ROOM_ID, ROOM_Name, ROOM_Location, ROOM_Status, ROOM_TYPE_ID) VALUES
    ('ROM0000001', '101', 'Tầng 1', 'AVAILABLE', 'RTP0000001'),
    ('ROM0000002', '102', 'Tầng 1', 'AVAILABLE', 'RTP0000001'),
    ('ROM0000003', '103', 'Tầng 1', 'AVAILABLE', 'RTP0000001'),
    ('ROM0000004', '201', 'Tầng 2', 'AVAILABLE', 'RTP0000002'),
    ('ROM0000005', '202', 'Tầng 2', 'AVAILABLE', 'RTP0000002'),
    ('ROM0000006', '203', 'Tầng 2', 'AVAILABLE', 'RTP0000002'),
    ('ROM0000007', '301', 'Tầng 3', 'AVAILABLE', 'RTP0000003'),
    ('ROM0000008', '302', 'Tầng 3', 'AVAILABLE', 'RTP0000003'),
    ('ROM0000009', '303', 'Tầng 3', 'AVAILABLE', 'RTP0000003'),
    ('ROM0000010', '401', 'Tầng 4', 'AVAILABLE', 'RTP0000004'),
    ('ROM0000011', '402', 'Tầng 4', 'AVAILABLE', 'RTP0000004'),
    ('ROM0000012', '403', 'Tầng 4', 'MAINTENANCE', 'RTP0000004'),
    ('ROM0000013', '501', 'Tầng 5', 'AVAILABLE', 'RTP0000005'),
    ('ROM0000014', '502', 'Tầng 5', 'AVAILABLE', 'RTP0000005'),
    ('ROM0000015', '601', 'Tầng 6', 'AVAILABLE', 'RTP0000006'),
    ('ROM0000016', '602', 'Tầng 6', 'AVAILABLE', 'RTP0000006'),
    ('ROM0000017', '701', 'Tầng 7', 'AVAILABLE', 'RTP0000007'),
    ('ROM0000018', '702', 'Tầng 7', 'AVAILABLE', 'RTP0000007'),
    ('ROM0000019', '801', 'Tầng 8', 'AVAILABLE', 'RTP0000008'),
    ('ROM0000020', '802', 'Tầng 8', 'AVAILABLE', 'RTP0000008');

-- ===========================================================
-- 4) DỊCH VỤ (12 DỊCH VỤ ĐA DẠNG) & CHIẾN DỊCH GIÁ
-- ===========================================================
INSERT INTO SERVICE (SERVICE_ID, SERVICE_Name, SERVICE_Description, SERVICE_Unit, SERVICE_BasePrice, SERVICE_ImagePath, SERVICE_Status, SERVICE_CreateDate, SERVICE_UpdateDate) VALUES
    ('SER0000001', 'Nước khoáng Evian', 'Nước khoáng thiên nhiên nhập khẩu 500ml', 'chai', 25000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000002', 'Buffet sáng Á - Âu 5 sao', 'Thực đơn hơn 60 món tại nhà hàng tầng 2', 'suất', 150000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000003', 'Giặt ủi lấy ngay cao cấp', 'Giặt hấp, ủi hơi nước và giao tận phòng sau 3 giờ', 'lần', 80000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000004', 'Đưa đón sân bay Limousine', 'Xe Limousine VIP đưa đón riêng tận sảnh', 'chuyến', 350000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000005', 'Phụ thu giường phụ (Extra Bed)', 'Nệm lò xo túi độc lập kèm bộ chăn ga tiêu chuẩn', 'cái', 250000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000006', 'Set Rượu vang Pháp & Phô mai', 'Rượu vang đỏ Bordeaux kèm đĩa phô mai & trái cây nhập khẩu', 'set', 850000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000007', 'Combo Trang trí phòng Honeymoon', 'Hoa hồng tươi, nến thơm, bóng bay và bánh kem lãng mạn', 'gói', 600000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000008', 'Liệu trình Spa & Massage đá nóng', 'Chăm sóc toàn thân 90 phút bởi kỹ thuật viên chuyên nghiệp', 'vé', 450000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000009', 'Bữa tối Bít tết Wagyu tại phòng', 'Thăn bò Wagyu áp chảo kèm súp nấm Truffle phục vụ tận phòng', 'suất', 650000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000010', 'Set Trà chiều Hoàng gia Anh', 'Tháp bánh ngọt Macaron, Scone và trà Bá Tước thượng hạng', 'set', 320000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000011', 'Thuê xe máy tay ga khám phá phố', 'Xe AirBlade/Vision đời mới kèm 2 mũ bảo hiểm', 'ngày', 180000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('SER0000012', 'Combo Snack & Nước ngọt Minibar', 'Khoai tây chiên, hạt điều rang muối và 2 lon nước ngọt', 'combo', 95000, '/image/default_service.jpg', 'ACTIVE', '2026-01-01 08:00:00', '2026-01-01 08:00:00');

INSERT INTO PRICE_RATE (PRICE_RATE_ID, PRICE_RATE_EventName, PRICE_RATE_SurchargeAmount, PRICE_RATE_CreateDate, PRICE_RATE_UpdateDate) VALUES
    ('PRC0000001', 'Giá tiêu chuẩn 2026', 0.0, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('PRC0000002', 'Ưu đãi Mùa Thu - Đông (-10%)', -10.0, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
    ('PRC0000003', 'Cao điểm Lễ Tết (+15%)', 15.0, '2026-01-01 08:00:00', '2026-01-01 08:00:00');

INSERT INTO APPLIED_PERIOD (APPLIED_PERIOD_ID, APPLIED_PERIOD_StartDate, APPLIED_PERIOD_EndDate, PRICE_RATE_ID, ROOM_TYPE_ID) VALUES
    ('APD0000001', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'PRC0000001', 'RTP0000001'),
    ('APD0000002', '2026-10-01 00:00:00', '2026-12-15 23:59:59', 'PRC0000002', 'RTP0000004'),
    ('APD0000003', '2026-10-01 00:00:00', '2026-12-15 23:59:59', 'PRC0000002', 'RTP0000005'),
    ('APD0000004', '2026-12-20 00:00:00', '2026-12-31 23:59:59', 'PRC0000003', 'RTP0000008');

-- ===========================================================
-- 5) LỊCH SỬ HẠNG KHÁCH HÀNG & LỊCH LÀM VIỆC
-- ===========================================================
INSERT INTO TIER_HISTORY (TIER_HISTORY_ID, TIER_HISTORY_StartDate, TIER_HISTORY_TotalSpending, TIER_HISTORY_Reason, USER_ID, TIER_CUS_ID) VALUES
    ('THI0000001', '2026-01-01 08:00:00', 2280000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000009', 'TCR0000001'),
    ('THI0000002', '2026-01-01 08:00:00', 5530000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000010', 'TCR0000002'),
    ('THI0000003', '2026-01-01 08:00:00', 3100000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000011', 'TCR0000001'),
    ('THI0000004', '2026-01-01 08:00:00', 5850000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000012', 'TCR0000002'),
    ('THI0000005', '2026-01-01 08:00:00', 6400000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000013', 'TCR0000002'),
    ('THI0000006', '2026-01-01 08:00:00', 7950000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000014', 'TCR0000002'),
    ('THI0000007', '2026-01-01 08:00:00', 9800000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000015', 'TCR0000002'),
    ('THI0000008', '2026-01-01 08:00:00', 2150000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000016', 'TCR0000001'),
    ('THI0000009', '2026-01-01 08:00:00', 4650000,  'Đồng bộ theo hóa đơn lưu trú', 'USR0000017', 'TCR0000001'),
    ('THI0000010', '2026-01-01 08:00:00', 21100000, 'Đồng bộ theo hóa đơn lưu trú', 'USR0000018', 'TCR0000003');

INSERT INTO WORK_SCHEDULE (WORK_SCHEDULE_ID, WORK_SCHEDULE_Date, WORK_SCHEDULE_Shift) VALUES
    ('WKS0000001', '2026-11-17', 'MORNING'),
    ('WKS0000002', '2026-11-17', 'AFTERNOON'),
    ('WKS0000003', '2026-11-18', 'MORNING');
INSERT INTO WORK_ASSIGNMENT (WORK_ASSIGNMENT_ID, WORK_ASSIGNMENT_AssignedAt, WORK_ASSIGNMENT_Note, USER_ID, WORK_SCHEDULE_ID) VALUES
    ('WAT0000001', '2026-11-16 08:00:00', 'Trực quầy lễ tân chính', 'USR0000006', 'WKS0000001'),
    ('WAT0000002', '2026-11-16 08:00:00', 'Hỗ trợ khách Check-out', 'USR0000007', 'WKS0000002'),
    ('WAT0000003', '2026-11-17 08:00:00', 'Trực ca sáng', 'USR0000008', 'WKS0000003');
INSERT INTO HISTORY_WORK (HISTORY_WORK_ID, HISTORY_WORK_CheckinTime, HISTORY_WORK_CheckoutTime, HISTORY_WORK_OvertimeHours, HISTORY_WORK_Note, HISTORY_WORK_Status, USER_ID, WORK_SCHEDULE_ID) VALUES
    ('HWK0000001', '2026-11-17 05:55:00', '2026-11-17 12:05:00', 0.0, 'Hoàn thành tốt', 'COMPLETED', 'USR0000006', 'WKS0000001'),
    ('HWK0000002', '2026-11-17 11:58:00', '2026-11-17 18:40:00', 0.7, 'Ở lại bàn giao ca', 'COMPLETED', 'USR0000007', 'WKS0000002');

-- ===========================================================
-- 6) GIAO DỊCH VÀ LƯU TRỮ HÓA ĐƠN
-- ===========================================================

-- Danh sách đơn RENTAL
INSERT INTO RENTAL (RENTAL_ID, RENTAL_RentDate, RENTAL_Note, RENTAL_IsBooking, RENTAL_Status, CUSTOMER_ID, STAFF_ID) VALUES
    -- Tháng 1
    ('REN260101', '2026-01-05 10:00:00', 'Khách nghỉ ngắn giờ', TRUE, 'COMPLETED', 'USR0000009', 'USR0000006'),
    ('REN260102', '2026-01-14 09:30:00', 'Gia đình nghỉ cuối tuần', TRUE, 'COMPLETED', 'USR0000014', 'USR0000006'),
    -- Tháng 2 (Dịp Tết & Valentine)
    ('REN260201', '2026-02-10 11:00:00', 'Kỷ niệm ngày Valentine', TRUE, 'COMPLETED', 'USR0000010', 'USR0000007'),
    ('REN260202', '2026-02-14 08:00:00', 'Công tác kết hợp nghỉ dưỡng', TRUE, 'COMPLETED', 'USR0000015', 'USR0000007'),
    ('REN260203', '2026-02-19 15:00:00', 'Tiệc Penthouse đầu năm', TRUE, 'COMPLETED', 'USR0000018', 'USR0000006'),
    -- Tháng 3
    ('REN260301', '2026-03-08 09:00:00', 'Quốc tế Phụ nữ 8/3', TRUE, 'COMPLETED', 'USR0000011', 'USR0000008'),
    -- Tháng 4
    ('REN260401', '2026-04-28 10:00:00', 'Nghỉ lễ 30/4 - Đặt 2 phòng cho gia đình', TRUE, 'COMPLETED', 'USR0000012', 'USR0000006'),
    -- Tháng 5
    ('REN260501', '2026-05-15 13:00:00', 'Thuê phòng làm việc yên tĩnh', TRUE, 'COMPLETED', 'USR0000016', 'USR0000007'),
    -- Tháng 6
    ('REN260601', '2026-06-19 10:00:00', 'Du lịch hè miền Tây', TRUE, 'COMPLETED', 'USR0000017', 'USR0000008'),
    -- Tháng 7
    ('REN260701', '2026-07-04 16:00:00', 'Trải nghiệm phòng Tổng tài & Thư ký', TRUE, 'COMPLETED', 'USR0000013', 'USR0000006'),
    -- Tháng 8
    ('REN260801', '2026-08-10 12:00:00', 'Nghỉ dưỡng Penthouse mùa hè', TRUE, 'COMPLETED', 'USR0000018', 'USR0000007'),
    -- Tháng 9
    ('REN260901', '2026-09-02 09:00:00', 'Nghỉ lễ Quốc khánh 2/9', TRUE, 'COMPLETED', 'USR0000009', 'USR0000006'),
    -- Tháng 10
    ('REN261001', '2026-10-18 11:00:00', 'Kỷ niệm 20/10 phòng Cặp đôi', TRUE, 'COMPLETED', 'USR0000010', 'USR0000008'),
    -- Tháng 11
    ('REN261101', '2026-11-10 10:00:00', 'Chuyến công tác tháng 11', TRUE, 'COMPLETED', 'USR0000014', 'USR0000006'),
    -- Tháng 12
    ('REN261201', '2026-12-23 09:00:00', 'Đón Giáng sinh tại khách sạn', TRUE, 'COMPLETED', 'USR0000015', 'USR0000007');

-- Chi tiết từng phòng thuê
INSERT INTO RENTAL_DETAIL (RENTAL_DETAIL_ID, RENTAL_DETAIL_CheckinDate, RENTAL_DETAIL_Type, RENTAL_DETAIL_LengthOfStay, RENTAL_DETAIL_UnitPrice, RENTAL_DETAIL_AdultCount, RENTAL_DETAIL_ChildrenCount, RENTAL_DETAIL_Status, RENTAL_ID, ROOM_ID) VALUES
    ('RDT260101', '2026-01-05 14:00:00', 'HOURLY', 4,  150000,  2, 0, 'CHECKED_OUT', 'REN260101', 'ROM0000001'),
    ('RDT260102', '2026-01-15 14:00:00', 'DAILY',  22, 4200000, 4, 2, 'CHECKED_OUT', 'REN260102', 'ROM0000017'),
    ('RDT260201', '2026-02-12 14:00:00', 'DAILY',  22, 1750000, 2, 0, 'CHECKED_OUT', 'REN260201', 'ROM0000010'),
    ('RDT260202', '2026-02-14 14:00:00', 'DAILY',  46, 2850000, 2, 0, 'CHECKED_OUT', 'REN260202', 'ROM0000013'),
    ('RDT260203', '2026-02-20 14:00:00', 'DAILY',  22, 9500000, 4, 0, 'CHECKED_OUT', 'REN260203', 'ROM0000019'),
    ('RDT260301', '2026-03-10 14:00:00', 'DAILY',  22, 1750000, 2, 0, 'CHECKED_OUT', 'REN260301', 'ROM0000011'),
    -- Đơn REN260401 đặt 2 phòng cùng lúc (1 phòng bản thân ở, 1 phòng đặt cho người đi cùng)
    ('RDT260401', '2026-04-28 14:00:00', 'DAILY',  22, 3200000, 2, 1, 'CHECKED_OUT', 'REN260401', 'ROM0000015'),
    ('RDT260402', '2026-04-28 14:00:00', 'DAILY',  22, 1350000, 2, 0, 'CHECKED_OUT', 'REN260401', 'ROM0000007'),
    ('RDT260501', '2026-05-15 14:00:00', 'DAILY',  22, 950000,  2, 0, 'CHECKED_OUT', 'REN260501', 'ROM0000004'),
    ('RDT260601', '2026-06-20 14:00:00', 'DAILY',  46, 1350000, 2, 1, 'CHECKED_OUT', 'REN260601', 'ROM0000008'),
    ('RDT260701', '2026-07-05 14:00:00', 'DAILY',  46, 2850000, 2, 0, 'CHECKED_OUT', 'REN260701', 'ROM0000014'),
    ('RDT260801', '2026-08-10 14:00:00', 'DAILY',  22, 9500000, 4, 1, 'CHECKED_OUT', 'REN260801', 'ROM0000020'),
    ('RDT260901', '2026-09-12 14:00:00', 'DAILY',  22, 950000,  2, 0, 'CHECKED_OUT', 'REN260901', 'ROM0000005'),
    ('RDT261001', '2026-10-18 14:00:00', 'DAILY',  22, 1750000, 2, 0, 'CHECKED_OUT', 'REN261001', 'ROM0000010'),
    ('RDT261101', '2026-11-11 14:00:00', 'DAILY',  22, 2850000, 2, 0, 'CHECKED_OUT', 'REN261101', 'ROM0000013'),
    ('RDT261201', '2026-12-24 14:00:00', 'DAILY',  22, 3200000, 3, 1, 'CHECKED_OUT', 'REN261201', 'ROM0000016');

-- Danh sách khách lưu trú thực tế tại từng phòng
INSERT INTO RENTAL_GUEST (RENTAL_GUEST_ID, RENTAL_GUEST_FullName, RENTAL_GUEST_PID, RENTAL_GUEST_Phone, RENTAL_GUEST_IsAdult, RENTAL_GUEST_IsRepresentative, RENTAL_DETAIL_ID) VALUES
    -- RDT260101 (2 NL)
    ('RGS260101A', 'Hân Gia', '001000000009', '0901000009', TRUE,  TRUE,  'RDT260101'),
    ('RGS260101B', 'Lê Minh Tuấn', '079099001122', '0912345111', TRUE,  FALSE, 'RDT260101'),
    -- RDT260102 (4 NL, 2 TE)
    ('RGS260102A', 'Ngọc Bảo', '001000000014', '0901000014', TRUE,  TRUE,  'RDT260102'),
    ('RGS260102B', 'Trần Hoàng Long', '079090002233', '0912345222', TRUE,  FALSE, 'RDT260102'),
    ('RGS260102C', 'Nguyễn Thị Mai', '079065003344', NULL, TRUE,  FALSE, 'RDT260102'),
    ('RGS260102D', 'Trần Văn Bách', '079062004455', NULL, TRUE,  FALSE, 'RDT260102'),
    ('RGS260102E', 'Bé Trần Gia Huy', NULL, NULL, FALSE, FALSE, 'RDT260102'),
    ('RGS260102F', 'Bé Trần Bảo An', NULL, NULL, FALSE, FALSE, 'RDT260102'),
    -- RDT260201 (2 NL - Phòng cặp đôi Mường Thanh)
    ('RGS260201A', 'Kiệt Tuấn', '001000000010', '0901000010', TRUE,  TRUE,  'RDT260201'),
    ('RGS260201B', 'Phạm Thu Thủy', '079098005566', '0988777666', TRUE,  FALSE, 'RDT260201'),
    -- RDT260202 (2 NL - Phòng Tổng tài & Thư ký)
    ('RGS260202A', 'Hải Quang', '001000000015', '0901000015', TRUE,  TRUE,  'RDT260202'),
    ('RGS260202B', 'Đỗ Nhật Hạ', '079099006677', '0977666555', TRUE,  FALSE, 'RDT260202'),
    -- RDT260203 (4 NL - Penthouse)
    ('RGS260203A', 'My Diễm', '001000000018', '0901000018', TRUE,  TRUE,  'RDT260203'),
    ('RGS260203B', 'Vũ Hoàng Việt', '079095007788', '0966555444', TRUE,  FALSE, 'RDT260203'),
    ('RGS260203C', 'Lý Nhã Kỳ', '079096008899', NULL, TRUE,  FALSE, 'RDT260203'),
    ('RGS260203D', 'Đặng Lê Nguyên', '079094009900', NULL, TRUE,  FALSE, 'RDT260203'),
    -- RDT260301 (2 NL)
    ('RGS260301A', 'Vy Khánh', '001000000011', '0901000011', TRUE,  TRUE,  'RDT260301'),
    ('RGS260301B', 'Phan Mạnh Quỳnh', '079093001234', '0933222111', TRUE,  FALSE, 'RDT260301'),
    -- RDT260401 & RDT260402 (Đơn 2 phòng: Phòng 1 chủ đơn ở, Phòng 2 đặt hộ cho bạn đi cùng)
    ('RGS260401A', 'Phát Đức', '001000000012', '0901000012', TRUE,  TRUE,  'RDT260401'),
    ('RGS260401B', 'Bùi Bích Phương', '079095002345', '0944333222', TRUE,  FALSE, 'RDT260401'),
    ('RGS260401C', 'Bé Đức Minh', NULL, NULL, FALSE, FALSE, 'RDT260401'),
    ('RGS260402A', 'Ngô Kiến Huy', '079091003456', '0911222333', TRUE,  TRUE,  'RDT260402'),
    ('RGS260402B', 'Khổng Tú Quỳnh', '079092004567', '0911222444', TRUE,  FALSE, 'RDT260402'),
    -- RDT260501 (2 NL)
    ('RGS260501A', 'Hương Lan', '001000000016', '0901000016', TRUE,  TRUE,  'RDT260501'),
    ('RGS260501B', 'Trương Minh Cường', '079085005678', NULL, TRUE,  FALSE, 'RDT260501'),
    -- RDT260601 (2 NL, 1 TE)
    ('RGS260601A', 'Trường Quốc', '001000000017', '0901000017', TRUE,  TRUE,  'RDT260601'),
    ('RGS260601B', 'Bảo Thanh', '079090006789', NULL, TRUE,  FALSE, 'RDT260601'),
    ('RGS260601C', 'Bé Quốc Thiên', NULL, NULL, FALSE, FALSE, 'RDT260601'),
    -- RDT260701 (2 NL)
    ('RGS260701A', 'Trúc Thanh', '001000000013', '0901000013', TRUE,  TRUE,  'RDT260701'),
    ('RGS260701B', 'Lương Thế Thành', '079088007890', '0909888777', TRUE,  FALSE, 'RDT260701'),
    -- RDT260801 (4 NL, 1 TE)
    ('RGS260801A', 'My Diễm', '001000000018', '0901000018', TRUE,  TRUE,  'RDT260801'),
    ('RGS260801B', 'Hứa Vĩ Văn', '079084008901', NULL, TRUE,  FALSE, 'RDT260801'),
    ('RGS260801C', 'Ninh Dương Lan Ngọc', '079090009012', NULL, TRUE,  FALSE, 'RDT260801'),
    ('RGS260801D', 'Kaity Nguyễn', '079099009123', NULL, TRUE,  FALSE, 'RDT260801'),
    ('RGS260801E', 'Bé Sunny', NULL, NULL, FALSE, FALSE, 'RDT260801'),
    -- RDT260901 (2 NL)
    ('RGS260901A', 'Hân Gia', '001000000009', '0901000009', TRUE,  TRUE,  'RDT260901'),
    ('RGS260901B', 'Lê Minh Tuấn', '079099001122', NULL, TRUE,  FALSE, 'RDT260901'),
    -- RDT261001 (2 NL)
    ('RGS261001A', 'Kiệt Tuấn', '001000000010', '0901000010', TRUE,  TRUE,  'RDT261001'),
    ('RGS261001B', 'Phạm Thu Thủy', '079098005566', NULL, TRUE,  FALSE, 'RDT261001'),
    -- RDT261101 (2 NL)
    ('RGS261101A', 'Ngọc Bảo', '001000000014', '0901000014', TRUE,  TRUE,  'RDT261101'),
    ('RGS261101B', 'Trần Hoàng Long', '079090002233', NULL, TRUE,  FALSE, 'RDT261101'),
    -- RDT261201 (3 NL, 1 TE)
    ('RGS261201A', 'Hải Quang', '001000000015', '0901000015', TRUE,  TRUE,  'RDT261201'),
    ('RGS261201B', 'Chu Thanh Huyền', '079099004321', NULL, TRUE,  FALSE, 'RDT261201'),
    ('RGS261201C', 'Đoàn Văn Hậu', '079099005432', NULL, TRUE,  FALSE, 'RDT261201'),
    ('RGS261201D', 'Bé Lido', NULL, NULL, FALSE, FALSE, 'RDT261201');

-- Hóa đơn (BILL)
INSERT INTO BILL (BILL_ID, BILL_CreateDate, BILL_TotalAmount, BILL_ActualStayHours, BILL_ActualRoomAmount, BILL_EarlyCheckinSurcharge, BILL_LateCheckoutSurcharge, BILL_Type, RENTAL_ID) VALUES
    ('BIL260101D', '2026-01-05 10:05:00', 180000,   NULL, NULL,    0,      0,      'DEPOSIT', 'REN260101'),
    ('BIL260101F', '2026-01-05 18:00:00', 720000,   4,    600000,  0,      0,      'FINAL',   'REN260101'),
    ('BIL260102D', '2026-01-14 09:35:00', 2100000,  NULL, NULL,    0,      0,      'DEPOSIT', 'REN260102'),
    ('BIL260102F', '2026-01-16 12:00:00', 2700000,  22,   4200000, 0,      0,      'FINAL',   'REN260102'),
    ('BIL260201D', '2026-02-10 11:05:00', 700000,   NULL, NULL,    0,      0,      'DEPOSIT', 'REN260201'),
    ('BIL260201F', '2026-02-13 12:00:00', 1650000,  22,   1750000, 0,      0,      'FINAL',   'REN260201'),
    ('BIL260202F', '2026-02-16 12:00:00', 6050000,  46,   5700000, 0,      0,      'FINAL',   'REN260202'),
    ('BIL260203F', '2026-02-21 12:00:00', 10350000, 22,   9500000, 0,      0,      'FINAL',   'REN260203'),
    ('BIL260301F', '2026-03-11 12:00:00', 3100000,  22,   2500000, 0,      0,      'FINAL',   'REN260301'),
    ('BIL260401F', '2026-04-29 14:30:00', 5850000,  24,   5250000, 0,      700000, 'FINAL',   'REN260401'),
    ('BIL260501F', '2026-05-16 12:00:00', 2150000,  22,   1900000, 0,      0,      'FINAL',   'REN260501'),
    ('BIL260601F', '2026-06-22 12:00:00', 4650000,  46,   4200000, 0,      0,      'FINAL',   'REN260601'),
    ('BIL260701F', '2026-07-07 12:00:00', 6400000,  46,   5700000, 0,      0,      'FINAL',   'REN260701'),
    ('BIL260801F', '2026-08-11 12:00:00', 10750000, 22,   9500000, 0,      0,      'FINAL',   'REN260801'),
    ('BIL260901F', '2026-09-13 12:00:00', 1380000,  22,   950000,  0,      0,      'FINAL',   'REN260901'),
    ('BIL261001F', '2026-10-19 12:00:00', 3180000,  22,   2330000, 580000, 0,      'FINAL',   'REN261001'),
    ('BIL261101F', '2026-11-12 12:00:00', 3150000,  22,   2850000, 0,      0,      'FINAL',   'REN261101'),
    ('BIL261201F', '2026-12-25 12:00:00', 3750000,  22,   3200000, 0,      0,      'FINAL',   'REN261201');

-- Giao dịch thanh toán (PAYMENT)
INSERT INTO PAYMENT (PAYMENT_ID, PAYMENT_Method, PAYMENT_Date, PAYMENT_Transaction, BILL_ID) VALUES
    ('PAY260101D', 'BANK', '2026-01-05 10:05:00', 'VNPAY-260101D', 'BIL260101D'),
    ('PAY260101F', 'CASH', '2026-01-05 18:00:00', 'CASH-260101F',  'BIL260101F'),
    ('PAY260102D', 'BANK', '2026-01-14 09:35:00', 'VNPAY-260102D', 'BIL260102D'),
    ('PAY260102F', 'BANK', '2026-01-16 12:00:00', 'BANK-260102F',  'BIL260102F'),
    ('PAY260201D', 'BANK', '2026-02-10 11:05:00', 'VNPAY-260201D', 'BIL260201D'),
    ('PAY260201F', 'BANK', '2026-02-13 12:00:00', 'BANK-260201F',  'BIL260201F'),
    ('PAY260202F', 'BANK', '2026-02-16 12:00:00', 'BANK-260202F',  'BIL260202F'),
    ('PAY260203F', 'BANK', '2026-02-21 12:00:00', 'BANK-260203F',  'BIL260203F'),
    ('PAY260301F', 'BANK', '2026-03-11 12:00:00', 'BANK-260301F',  'BIL260301F'),
    ('PAY260401F', 'BANK', '2026-04-29 14:30:00', 'BANK-260401F',  'BIL260401F'),
    ('PAY260501F', 'CASH', '2026-05-16 12:00:00', 'CASH-260501F',  'BIL260501F'),
    ('PAY260601F', 'BANK', '2026-06-22 12:00:00', 'BANK-260601F',  'BIL260601F'),
    ('PAY260701F', 'BANK', '2026-07-07 12:00:00', 'BANK-260701F',  'BIL260701F'),
    ('PAY260801F', 'BANK', '2026-08-11 12:00:00', 'BANK-260801F',  'BIL260801F'),
    ('PAY260901F', 'CASH', '2026-09-13 12:00:00', 'CASH-260901F',  'BIL260901F'),
    ('PAY261001F', 'BANK', '2026-10-19 12:00:00', 'BANK-261001F',  'BIL261001F'),
    ('PAY261101F', 'BANK', '2026-11-12 12:00:00', 'BANK-261101F',  'BIL261101F'),
    ('PAY261201F', 'BANK', '2026-12-25 12:00:00', 'BANK-261201F',  'BIL261201F');

-- Sử dụng dịch vụ đa dạng qua 12 tháng (SERVICE_USAGE)
INSERT INTO SERVICE_USAGE (SERVICE_USAGE_ID, SERVICE_USAGE_Count, SERVICE_USAGE_Time, SERVICE_USAGE_UnitPrice, RENTAL_DETAIL_ID, SERVICE_ID) VALUES
    ('SVG260101', 2, '2026-01-05 15:00:00', 150000, 'RDT260101', 'SER0000002'),
    ('SVG260102', 4, '2026-01-16 08:00:00', 150000, 'RDT260102', 'SER0000002'),
    ('SVG260201', 1, '2026-02-12 19:00:00', 600000, 'RDT260201', 'SER0000007'),
    ('SVG260202', 1, '2026-02-14 10:00:00', 350000, 'RDT260202', 'SER0000004'),
    ('SVG260203', 1, '2026-02-20 20:00:00', 850000, 'RDT260203', 'SER0000006'),
    ('SVG260301', 1, '2026-03-10 19:30:00', 600000, 'RDT260301', 'SER0000007'),
    ('SVG260401', 4, '2026-04-29 08:00:00', 150000, 'RDT260401', 'SER0000002'),
    ('SVG260501', 1, '2026-05-15 16:00:00', 250000, 'RDT260501', 'SER0000005'),
    ('SVG260601', 1, '2026-06-21 15:00:00', 450000, 'RDT260601', 'SER0000008'),
    ('SVG260701', 1, '2026-07-06 19:00:00', 650000, 'RDT260701', 'SER0000009'),
    ('SVG260801', 2, '2026-08-10 20:30:00', 650000, 'RDT260801', 'SER0000009'),
    ('SVG260901', 1, '2026-09-12 16:00:00', 320000, 'RDT260901', 'SER0000010'),
    ('SVG260902', 1, '2026-09-12 21:00:00', 95000,  'RDT260901', 'SER0000012'),
    ('SVG261001', 1, '2026-10-18 20:00:00', 850000, 'RDT261001', 'SER0000006'),
    ('SVG261101', 2, '2026-11-11 08:30:00', 150000, 'RDT261101', 'SER0000002'),
    ('SVG261201', 1, '2026-12-24 18:00:00', 350000, 'RDT261201', 'SER0000004'),
    ('SVG261202', 8, '2026-12-24 21:00:00', 25000,  'RDT261201', 'SER0000001');

-- ===========================================================
-- 7) ĐÁNH GIÁ ĐA DẠNG KHEN / CHÊ 
-- ===========================================================
INSERT INTO REVIEW (REVIEW_ID, REVIEW_Rate, REVIEW_Description, REVIEW_UpdateDate, USER_ID) VALUES
    ('REV0000001', 5, 'Phòng cực kỳ sạch sẽ và thơm, nội thất sang trọng đúng chuẩn 5 sao. Nhân viên lễ tân hỗ trợ thủ tục check-in rất nhanh gọn, nhiệt tình.', '2026-09-15 10:00:00', 'USR0000009'),
    ('REV0000002', 5, 'Phòng Cặp đôi Mường Thanh thiết kế quá đỉnh! Bồn tắm Jacuzzi và đèn LED đổi màu siêu lãng mạn, rèm tự động hiện đại, người yêu mình cực kỳ thích.', '2026-10-20 13:00:00', 'USR0000010'),
    ('REV0000003', 4, 'Không gian phòng đẹp, view thành phố thoáng đãng, giường nằm êm. Tuy nhiên wifi vào buổi tối hơi chập chờn và đồ ăn sáng lên món hơi chậm một chút.', '2026-03-12 09:30:00', 'USR0000011'),
    ('REV0000004', 3, 'Vị trí khách sạn thuận tiện đi lại, phòng rộng rãi. Nhưng cách âm chưa thực sự tốt, ban đêm vẫn nghe tiếng ồn ngoài hành lang và giá dịch vụ minibar hơi đắt.', '2026-05-01 14:20:00', 'USR0000012'),
    ('REV0000005', 5, 'Trải nghiệm phòng Tổng tài và Thư ký thực sự đẳng cấp! Bàn làm việc rộng, ghế ngồi cực êm, có sẵn máy pha cà phê Espresso và loa Bose nghe nhạc rất chill.', '2026-07-08 11:15:00', 'USR0000013'),
    ('REV0000006', 4, 'Gia đình mình ở phòng Royal Family rất thoải mái, buffet sáng ngon và đa dạng món. Điểm trừ nhỏ là lúc nhận phòng phải đợi dọn phòng mất gần 15 phút.', '2026-11-13 16:40:00', 'USR0000014'),
    ('REV0000007', 2, 'Phòng ốc nhìn chung ổn nhưng điều hòa làm lạnh khá chậm, gọi bộ phận kỹ thuật lên kiểm tra hơi lâu. Mong khách sạn cải thiện tốc độ phục vụ buồng phòng.', '2026-11-15 18:00:00', 'USR0000015'),
    ('REV0000008', 4, 'Giá cả hợp lý so với chất lượng, khăn tắm và chăn ga rất sạch sẽ. Nhân viên bảo vệ và lễ tân đều thân thiện, có dịp công tác Cần Thơ sẽ quay lại.', '2026-05-18 08:45:00', 'USR0000016'),
    ('REV0000009', 3, 'Phòng Deluxe có bồn tắm nằm khá thích nhưng nước nóng buổi khuya hơi yếu. Bù lại dịch vụ massage đá nóng của khách sạn làm rất tốt và thư giãn.', '2026-06-24 20:10:00', 'USR0000017'),
    ('REV0000010', 5, 'Căn Penthouse tầng thượng thực sự xa hoa tuyệt đối! Phòng xông hơi đá muối và Jacuzzi hoạt động hoàn hảo, bữa tối Wagyu phục vụ tận phòng nóng hổi, đáng từng đồng.', '2026-08-14 12:00:00', 'USR0000018');

-- Sự cố phát sinh trong quá trình lưu trú (INCIDENT)
INSERT INTO INCIDENT (INCIDENT_ID, INCIDENT_Name, INCIDENT_OccurTime, INCIDENT_Description, INCIDENT_Responsible, INCIDENT_Money, INCIDENT_Handle, INCIDENT_Status, RENTAL_DETAIL_ID, CREATED_BY) VALUES 
    ('ICD0000001', 'Làm vỡ ly rượu vang Pha lê', '2026-10-18 22:30:00', 'Khách sơ ý làm rơi vỡ 1 ly rượu vang trong phòng 401', 'CUSTOMER', 150000, 'Khách đồng ý đền bù khi trả phòng', 'RESOLVED', 'RDT261001', 'USR0000006');

COMMIT;