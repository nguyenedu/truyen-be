-- ============================================
-- FAKE DATA CHO DATABASE TRUYỆN
-- Tác giả: [Tên bạn]
-- Ngày tạo: 2025-01-26
-- ============================================

-- Tắt kiểm tra khóa ngoại tạm thời
SET FOREIGN_KEY_CHECKS = 0;

-- Xóa dữ liệu cũ (CẨN THẬN: Lệnh này sẽ xóa toàn bộ dữ liệu)
TRUNCATE TABLE comment_likes;
TRUNCATE TABLE reading_history;
TRUNCATE TABLE favorites;
TRUNCATE TABLE ratings;
TRUNCATE TABLE comments;
TRUNCATE TABLE chapters;
TRUNCATE TABLE story_categories;
TRUNCATE TABLE rankings;
TRUNCATE TABLE stories;
TRUNCATE TABLE authors;
TRUNCATE TABLE categories;
TRUNCATE TABLE users;
TRUNCATE TABLE activity_logs;

-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. THÊM DỮ LIỆU CHO BẢNG CATEGORIES
-- ============================================
INSERT INTO categories (name, description, created_at) VALUES
                                                           ('Tiên Hiệp', 'Thể loại tu tiên, tu chân, tìm đạo trường sinh', NOW()),
                                                           ('Huyền Huyễn', 'Thế giới huyền ảo, phép thuật, ma pháp', NOW()),
                                                           ('Đô Thị', 'Cuộc sống hiện đại trong thành phố', NOW()),
                                                           ('Kiếm Hiệp', 'Giang hồ, võ lâm, kiếm khách', NOW()),
                                                           ('Ngôn Tình', 'Tình cảm lãng mạn', NOW()),
                                                           ('Trinh Thám', 'Phá án, giải mã bí ẩn', NOW()),
                                                           ('Khoa Huyễn', 'Khoa học viễn tưởng, công nghệ tương lai', NOW()),
                                                           ('Đam Mỹ', 'Tình cảm nam nam', NOW()),
                                                           ('Xuyên Không', 'Du hành thời gian, xuyên qua quá khứ tương lai', NOW()),
                                                           ('Trọng Sinh', 'Tái sinh, sống lại, hồi quy', NOW()),
                                                           ('Hệ Thống', 'Truyện có hệ thống hỗ trợ nhân vật chính', NOW()),
                                                           ('Võ Hiệp', 'Võ thuật, nội công, kinh mạch', NOW());

-- ============================================
-- 2. THÊM DỮ LIỆU CHO BẢNG AUTHORS
-- ============================================
INSERT INTO authors (name, bio, avatar, created_at) VALUES
                                                        ('Ngã Ăn Tây Qua', 'Tác giả nổi tiếng với các tác phẩm tiên hiệp, huyền huyễn. Đại diện cho tác phẩm: Đấu Phá Thương Khung', 'author1.jpg', NOW()),
                                                        ('Thiên Tàm Thổ Đậu', 'Chuyên viết truyện huyền huyễn, võ hiệp. Phong cách viết cuốn hút, nhân vật sinh động', 'author2.jpg', NOW()),
                                                        ('Mộng Nhập Thần Cơ', 'Tác giả võ hiệp kiếm hiệp hàng đầu. Nổi tiếng với các tác phẩm như Thần Mộ', 'author3.jpg', NOW()),
                                                        ('Đường Gia Tam Thiếu', 'Nổi tiếng với thể loại huyền huyễn, võ hiệp. Tác phẩm tiêu biểu: Đấu La Đại Lục', 'author4.jpg', NOW()),
                                                        ('Cát Tường Dạ', 'Chuyên viết ngôn tình hiện đại, cổ đại. Phong cách ngọt ngào, lãng mạn', 'author5.jpg', NOW()),
                                                        ('Liễu Hạ Huy', 'Tác giả đô thị, hệ thống. Truyện mang tính giải trí cao', 'author6.jpg', NOW()),
                                                        ('Phong Hỏa Hí Chư Hầu', 'Tác giả nổi tiếng Trung Quốc. Chuyên viết tiên hiệp, huyền huyễn', 'author7.jpg', NOW()),
                                                        ('Cửu Lộ Phi Hương', 'Chuyên viết ngôn tình cổ đại, xuyên không. Tác phẩm nhiều twist bất ngờ', 'author8.jpg', NOW()),
                                                        ('Vĩnh Hằng Chi Hỏa', 'Tác giả tiên hiệp, huyền huyễn. Thế giới quan rộng lớn', 'author9.jpg', NOW()),
                                                        ('Quách Kính Minh', 'Tác giả võ hiệp Hong Kong. Phong cách kinh điển', 'author10.jpg', NOW()),
                                                        ('Nguyễn Nhật Ánh', 'Tác giả Việt Nam nổi tiếng với các tác phẩm thiếu nhi, tuổi học trò', 'author11.jpg', NOW()),
                                                        ('Tô Hoài', 'Nhà văn Việt Nam. Tác phẩm nổi tiếng: Dế Mèn Phiêu Lưu Ký', 'author12.jpg', NOW()),
                                                        ('Nguyễn Ngọc Tư', 'Nữ nhà văn Việt Nam. Chuyên viết truyện ngắn về miền Tây', 'author13.jpg', NOW()),
                                                        ('Anh Khang', 'Tác giả trẻ Việt Nam. Viết về đời sống đô thị', 'author14.jpg', NOW()),
                                                        ('Mai Lan Hương', 'Chuyên viết ngôn tình, lãng mạn. Phong cách nhẹ nhàng', 'author15.jpg', NOW());

-- ============================================
-- 3. THÊM DỮ LIỆU CHO BẢNG USERS
-- ============================================
-- Lưu ý: Password đã mã hóa BCrypt = "password123"
INSERT INTO users (username, email, password, fullname, avatar, phone, role, is_active, created_at) VALUES
                                                                                                        ('admin', 'admin@truyen.vn', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Administrator', 'admin.jpg', '0901234567', 'ADMIN', true, NOW()),
                                                                                                        ('superadmin', 'superadmin@truyen.vn', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Super Administrator', 'superadmin.jpg', '0901234568', 'SUPER_ADMIN', true, NOW()),
                                                                                                        ('nguyenvana', 'vana@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Nguyễn Văn A', 'user1.jpg', '0912345678', 'USER', true, NOW()),
                                                                                                        ('tranthib', 'thib@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Trần Thị B', 'user2.jpg', '0923456789', 'USER', true, NOW()),
                                                                                                        ('levanc', 'vanc@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Lê Văn C', 'user3.jpg', '0934567890', 'USER', true, NOW()),
                                                                                                        ('phamthid', 'thid@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Phạm Thị D', 'user4.jpg', '0945678901', 'USER', true, NOW()),
                                                                                                        ('hoangvane', 'vane@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Hoàng Văn E', 'user5.jpg', '0956789012', 'USER', true, NOW()),
                                                                                                        ('vuthif', 'thif@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Vũ Thị F', NULL, '0967890123', 'USER', true, NOW()),
                                                                                                        ('dongvang', 'vang@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Đỗ Ngọc Văn G', NULL, '0978901234', 'USER', true, NOW()),
                                                                                                        ('buithih', 'thih@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Bùi Thị H', NULL, '0989012345', 'USER', false, NOW()),
                                                                                                        ('dangvani', 'vani@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Đặng Văn I', 'user6.jpg', '0990123456', 'USER', true, NOW()),
                                                                                                        ('lyvanK', 'vank@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Lý Văn K', 'user7.jpg', '0901234569', 'USER', true, NOW()),
                                                                                                        ('maithil', 'thil@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Mai Thị L', NULL, '0912345670', 'USER', true, NOW()),
                                                                                                        ('phantm', 'tm@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Phan Thanh M', 'user8.jpg', '0923456780', 'USER', true, NOW()),
                                                                                                        ('duongn', 'duongn@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Dương Ngọc N', NULL, '0934567891', 'USER', true, NOW()),
                                                                                                        ('havanp', 'vanp@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IKmU9CIE7t1F8v0lz/lxQZhFYPmHj6', 'Hà Văn P', 'user9.jpg', '0945678902', 'USER', true, NOW());

-- ============================================
-- 4. THÊM DỮ LIỆU CHO BẢNG STORIES
-- ============================================
INSERT INTO stories (title, author_id, description, image, status, total_chapters, total_views, is_hot, created_at, updated_at) VALUES
                                                                                                                                    ('Đấu Phá Thương Khung', 1, 'Đây là thế giới thuộc về Đấu Khí, không hề có ma pháp hoa tiếu diễm lệ! Truyện kể về hành trình từ thiên tài bị rơi xuống đến đỉnh cao của Tiêu Viêm.', 'dau-pha-thuong-khung.jpg', 'COMPLETED', 1648, 15234567, true, DATE_SUB(NOW(), INTERVAL 3 YEAR), NOW()),
                                                                                                                                    ('Vũ Động Càn Khôn', 2, 'Lâm Động, thiếu niên từ một tông phái nhỏ, bước lên con đường tu luyện để đối đầu với số phận.', 'vu-dong-can-khon.jpg', 'COMPLETED', 1343, 12456789, true, DATE_SUB(NOW(), INTERVAL 2 YEAR), NOW()),
                                                                                                                                    ('Thần Mộ', 3, 'Vạn cổ trường minh, ai nhân ai mộ? Một thần thoại cổ xưa về các vị thần và ác ma.', 'than-mo.jpg', 'COMPLETED', 900, 8765432, true, DATE_SUB(NOW(), INTERVAL 4 YEAR), NOW()),
                                                                                                                                    ('Bàn Long', 4, 'Lâm Lôi - một thiếu niên với khối huyết mạch đặc biệt, bước lên con đường tu luyện đầy gian nan.', 'ban-long.jpg', 'ONGOING', 2543, 20123456, true, DATE_SUB(NOW(), INTERVAL 1 YEAR), NOW()),
                                                                                                                                    ('Toàn Chức Pháp Sư', 5, 'Mạc Phàm - học sinh bình thường giác tỉnh ma pháp hiếm có. Trong thế giới mà yêu ma hoành hành.', 'toan-chuc-phap-su.jpg', 'ONGOING', 3215, 25678901, true, DATE_SUB(NOW(), INTERVAL 6 MONTH), NOW()),
                                                                                                                                    ('Đại Chúa Tể', 2, 'Trong đại thiên giới, vạn tộc hội tụ, chúng vương tranh bá. Một thiếu niên từ Bắc Linh Cảnh bước ra.', 'dai-chua-te.jpg', 'COMPLETED', 1600, 18234567, true, DATE_SUB(NOW(), INTERVAL 1 YEAR), NOW()),
                                                                                                                                    ('Nguyên Tôn', 1, 'Chu Nguyên - thiếu niên với vận mệnh đặc biệt, trên con đường tìm kiếm sức mạnh tuyệt đối.', 'nguyen-ton.jpg', 'ONGOING', 1800, 16543210, true, DATE_SUB(NOW(), INTERVAL 8 MONTH), NOW()),
                                                                                                                                    ('Tu La Vũ Thần', 3, 'Sở Phong - thiên tài võ đạo bị phế, nhưng với ý chí kiên cường đã vươn lên trở thành vũ thần.', 'tu-la-vu-than.jpg', 'ONGOING', 5000, 30123456, true, DATE_SUB(NOW(), INTERVAL 2 YEAR), NOW()),
                                                                                                                                    ('Tinh Thần Biến', 4, 'Tần Vũ - thiếu niên biến cố gia đình, bước vào tu luyện để trả thù và tìm lại công bằng.', 'tinh-than-bien.jpg', 'COMPLETED', 800, 7654321, false, DATE_SUB(NOW(), INTERVAL 5 YEAR), NOW()),
                                                                                                                                    ('Hoàng Mộc Lương Châu', 5, 'Truyện ngôn tình cổ đại đầy lãng mạn giữa nữ tướng quân và hoàng tử.', 'hoang-moc-luong-chau.jpg', 'COMPLETED', 120, 3456789, false, DATE_SUB(NOW(), INTERVAL 3 YEAR), NOW()),
                                                                                                                                    ('Tuyệt Thế Võ Hồn', 6, 'Phong Hạo - võ giả trẻ tuổi với vũ hồn kỳ lạ, chinh phục thiên hạ võ đạo.', 'tuyet-the-vo-hon.jpg', 'ONGOING', 2200, 14567890, true, DATE_SUB(NOW(), INTERVAL 10 MONTH), NOW()),
                                                                                                                                    ('Đế Bá', 7, 'Lý Thất Dạ - đế tử bị phế truất, vùng lên từ tro tàn để giành lại ngôi vị.', 'de-ba.jpg', 'ONGOING', 1950, 13234567, true, DATE_SUB(NOW(), INTERVAL 1 YEAR), NOW()),
                                                                                                                                    ('Thần Ấn Vương Tọa', 8, 'Long Hạo Thần - kỵ sĩ sở hữu thần ấn, chiến đấu chống lại ma tộc.', 'than-an-vuong-toa.jpg', 'COMPLETED', 876, 9876543, false, DATE_SUB(NOW(), INTERVAL 2 YEAR), NOW()),
                                                                                                                                    ('Độc Tôn Thiên Hạ', 9, 'Lâm Phong - thiên tài độc đạo, dùng độc thuật tung hoành giang hồ.', 'doc-ton-thien-ha.jpg', 'ONGOING', 1650, 11234567, false, DATE_SUB(NOW(), INTERVAL 7 MONTH), NOW()),
                                                                                                                                    ('Cuồng Thần', 10, 'Niếp Cuồng - thiếu niên cuồng nộ, một lần giác tỉnh trở thành cuồng thần bất bại.', 'cuong-than.jpg', 'PAUSED', 980, 6789012, false, DATE_SUB(NOW(), INTERVAL 1 YEAR), NOW()),
                                                                                                                                    ('Dị Giới Chi Quang Não Thuật Sư', 11, 'Lý Tưởng - người Trái Đất xuyên sang dị giới, trở thành thuật sư quang não.', 'di-gioi-quang-nao.jpg', 'ONGOING', 1234, 8901234, false, DATE_SUB(NOW(), INTERVAL 5 MONTH), NOW()),
                                                                                                                                    ('Trọng Sinh Chi Đô Thị Tu Tiên', 12, 'Trần Bắc Huyền - tu sĩ trọng sinh về quá khứ, tu tiên giữa đô thị hiện đại.', 'trong-sinh-do-thi.jpg', 'ONGOING', 2100, 19876543, true, DATE_SUB(NOW(), INTERVAL 4 MONTH), NOW()),
                                                                                                                                    ('Ma Pháp Học Viện', 13, 'Câu chuyện về cuộc sống tại học viện ma pháp danh tiếng nhất lục địa.', 'ma-phap-hoc-vien.jpg', 'ONGOING', 567, 4567890, false, DATE_SUB(NOW(), INTERVAL 9 MONTH), NOW()),
                                                                                                                                    ('Kiếm Đạo Độc Tôn', 14, 'Dương Diệp - kiếm tu với thiên phú tuyệt đỉnh, trên con đường kiếm đạo độc tôn.', 'kiem-dao-doc-ton.jpg', 'COMPLETED', 1456, 10234567, false, DATE_SUB(NOW(), INTERVAL 3 YEAR), NOW()),
                                                                                                                                    ('Hệ Thống Tu Tiên', 15, 'Trương Tam - người bình thường được hệ thống lựa chọn, bắt đầu hành trình tu tiên kỳ ảo.', 'he-thong-tu-tien.jpg', 'ONGOING', 890, 5678901, false, DATE_SUB(NOW(), INTERVAL 6 MONTH), NOW());

-- ============================================
-- 5. LIÊN KẾT TRUYỆN VỚI THỂ LOẠI
-- ============================================
INSERT INTO story_categories (story_id, category_id) VALUES
                                                         (1, 1), (1, 2), (2, 1), (2, 2), (3, 1), (3, 4), (4, 2), (4, 1), (5, 2), (5, 3),
                                                         (6, 1), (6, 2), (7, 1), (8, 1), (8, 4), (9, 2), (9, 7), (10, 5),
                                                         (11, 12), (11, 2), (12, 2), (12, 10), (13, 2), (13, 4), (14, 1),
                                                         (15, 2), (16, 2), (16, 9), (17, 3), (17, 10), (17, 1), (18, 2),
                                                         (19, 4), (19, 1), (20, 11), (20, 1);

-- ============================================
-- 6. THÊM CHAPTERS
-- ============================================
INSERT INTO chapters (story_id, chapter_number, title, content, views, created_at) VALUES
                                                                                       (1, 1, 'Chương 1: Thiên tài rơi xuống', 'Tiêu Viêm vốn là thiên tài của gia tộc Tiêu, được mệnh danh là kỳ tài nghìn năm có một. Nhưng từ sau sinh nhật lần thứ 11, đấu khí trong người hắn bắt đầu suy giảm một cách bí ẩn. Từ Đấu Chi Khí, hắn rơi xuống còn Tam Đoạn Đấu Chi Khí. Mọi người trong gia tộc bắt đầu khinh thị, chế giễu hắn. Thậm chí, hôn thê của hắn - Nạp Lan Yên Nhiên cũng đến hủy hôn ước...', 150000, DATE_SUB(NOW(), INTERVAL 3 YEAR)),
                                                                                       (1, 2, 'Chương 2: Lão sư bí ẩn', 'Trong chiếc nhẫn của Tiêu Viêm ẩn chứa một linh hồn cổ xưa - Dược Lão. Nguyên nhân đấu khí của Tiêu Viêm suy giảm chính là do nhẫn này hấp thụ. Dược Lão hứa sẽ giúp Tiêu Viêm lấy lại đấu khí và trở nên mạnh mẽ hơn bao giờ hết.', 148000, DATE_SUB(NOW(), INTERVAL 3 YEAR)),
                                                                                       (1, 3, 'Chương 3: Hủy hôn ước', 'Nạp Lan Yên Nhiên - thiên tài của gia tộc Nạp Lan, đến Tiêu gia để hủy hôn ước. Cô nhìn Tiêu Viêm với ánh mắt khinh thường. Tiêu Viêm cắn răng, thề rằng 30 năm Hà Đông, 30 năm Hà Tây, đừng khinh thường thiếu niên nghèo khó.', 145000, DATE_SUB(NOW(), INTERVAL 3 YEAR)),
                                                                                       (1, 4, 'Chương 4: Tu luyện Phần Quyết', 'Dưới sự hướng dẫn của Dược Lão, Tiêu Viêm bắt đầu tu luyện Phần Quyết - một công pháp đẳng cấp Địa Giai. Phần Quyết có thể hấp thụ dị hỏa thiên nhiên để tăng sức mạnh.', 142000, DATE_SUB(NOW(), INTERVAL 3 YEAR)),
                                                                                       (1, 5, 'Chương 5: Tăng tiến thần tốc', 'Chỉ trong vài tháng, Tiêu Viêm đã phục hồi tu vi từ Tam Đoạn Đấu Chi Khí lên Cửu Đoạn Đấu Chi Khí. Tốc độ tu luyện này làm cả gia tộc kinh ngạc.', 140000, DATE_SUB(NOW(), INTERVAL 3 YEAR)),
                                                                                       (2, 1, 'Chương 1: Niết Bàn Chi Trai', 'Lâm Động là một thiếu niên bình thường đến từ Thanh Dương Trấn. Trong một lần tình cờ, hắn nhặt được một viên Thạch Phù kỳ lạ...', 120000, DATE_SUB(NOW(), INTERVAL 2 YEAR)),
                                                                                       (2, 2, 'Chương 2: Biểu tượng tổ phù', 'Viên Thạch Phù hóa ra chứa đựng sức mạnh của Tổ Phù cổ xưa. Lâm Động bắt đầu tu luyện biểu tượng...', 118000, DATE_SUB(NOW(), INTERVAL 2 YEAR)),
                                                                                       (2, 3, 'Chương 3: Đại hội tuyển chọn', 'Thanh Dương Trấn tổ chức đại hội để tuyển chọn đệ tử vào Đạo Tông...', 115000, DATE_SUB(NOW(), INTERVAL 2 YEAR)),
                                                                                       (3, 1, 'Chương 1: Thần thoại sụp đổ', 'Nghĩa địa của các vị thần và ác ma xuất hiện giữa lục địa. Thần cũng có ngày tàn, ma cũng có lúc diệt vong...', 100000, DATE_SUB(NOW(), INTERVAL 4 YEAR)),
                                                                                       (3, 2, 'Chương 2: Phục sinh', 'Thần Thần - vị thần trẻ tuổi tỉnh dậy sau vạn năm trong nghĩa địa thần ma...', 98000, DATE_SUB(NOW(), INTERVAL 4 YEAR));

-- ============================================
-- 7. THÊM COMMENTS
-- ============================================
INSERT INTO comments (user_id, story_id, chapter_id, content, likes_count, created_at, updated_at) VALUES
                                                                                                       (3, 1, NULL, 'Truyện hay quá! Đọc mãi không chán!', 125, NOW(), NOW()),
                                                                                                       (4, 1, NULL, 'Tiêu Viêm quá ngầu, 30 năm Hà Đông Hà Tây =))', 89, NOW(), NOW()),
                                                                                                       (5, 1, 1, 'Chapter đầu rất hấp dẫn, cảm giác như bị lôi cuốn vào thế giới đấu khí', 45, NOW(), NOW()),
                                                                                                       (6, 2, NULL, 'Vũ Động Càn Khôn là một trong những truyện hay nhất tôi từng đọc!', 67, NOW(), NOW()),
                                                                                                       (7, 2, NULL, 'Lâm Động là nhân vật chính rất hay, không phải dạng chủ giác vô địch ngay từ đầu', 54, NOW(), NOW()),
                                                                                                       (8, 3, NULL, 'Thần Mộ có thế giới quan rất rộng lớn và sâu sắc', 78, NOW(), NOW()),
                                                                                                       (9, 4, NULL, 'Bàn Long viết rất cuốn, đọc xong muốn đọc tiếp ngay', 92, NOW(), NOW()),
                                                                                                       (10, 5, NULL, 'Toàn Chức Pháp Sư có hệ thống ma pháp rất hay!', 103, NOW(), NOW()),
                                                                                                       (11, 1, NULL, 'Mong tác giả ra thêm nhiều truyện hay như thế này', 34, NOW(), NOW()),
                                                                                                       (12, 1, 2, 'Dược Lão xuất hiện là plot twist hay nhất!', 56, NOW(), NOW());

-- ============================================
-- 8. THÊM RATINGS
-- ============================================
INSERT INTO ratings (user_id, story_id, rating, review, created_at) VALUES
                                                                        (3, 1, 5, 'Truyện xuất sắc! 10/10 điểm. Cốt truyện hấp dẫn, nhân vật sinh động, thế giới quan rộng lớn.', NOW()),
                                                                        (4, 1, 5, 'Một trong những truyện tiên hiệp hay nhất! Rấtđáng đọc!', NOW()),
                                                                        (5, 1, 4, 'Truyện hay nhưng hơi dài, nhiều chỗ lặp lại', NOW()),
                                                                        (6, 2, 5, 'Vũ Động Càn Khôn xứng đáng 5 sao! Cực kỳ hay!', NOW()),
                                                                        (7, 2, 4, 'Truyện hay, nhưng đầu hơi chậm', NOW()),
                                                                        (8, 3, 5, 'Thần Mộ là kiệt tác! Đọc đi đọc lại nhiều lần vẫn thấy hay', NOW()),
                                                                        (9, 4, 5, 'Bàn Long rất cuốn, đọc không thể dừng lại', NOW()),
                                                                        (10, 5, 4, 'Truyện hay nhưng có chỗ hơi phi lý', NOW()),
                                                                        (11, 1, 5, 'Đấu Phá là huyền thoại!', NOW()),
                                                                        (12, 6, 4, 'Đại Chúa Tể viết rất tốt', NOW());
-- ============================================
-- 9. THÊM FAVORITES
-- ============================================
INSERT INTO favorites (user_id, story_id, created_at) VALUES
                                                          (3, 1, NOW()), (3, 2, NOW()), (3, 5, NOW()),
                                                          (4, 1, NOW()), (4, 3, NOW()),
                                                          (5, 1, NOW()), (5, 2, NOW()), (5, 4, NOW()),
                                                          (6, 2, NOW()), (6, 5, NOW()),
                                                          (7, 1, NOW()), (7, 4, NOW()), (7, 8, NOW()),
                                                          (8, 3, NOW()), (8, 6, NOW()),
                                                          (9, 4, NOW()), (9, 7, NOW()),
                                                          (10, 5, NOW()), (10, 1, NOW()),
                                                          (11, 1, NOW()), (11, 2, NOW()), (11, 3, NOW()),
                                                          (12, 6, NOW()), (12, 8, NOW());
-- ============================================
-- 10. THÊM READING HISTORY
-- ============================================
INSERT INTO reading_history (user_id, story_id, chapter_id, read_at) VALUES
                                                                         (3, 1, 1, NOW()), (3, 1, 2, NOW()), (3, 1, 3, NOW()),
                                                                         (4, 1, 1, NOW()), (4, 1, 2, NOW()),
                                                                         (5, 2, 1, NOW()), (5, 2, 2, NOW()),
                                                                         (6, 1, 1, NOW()),
                                                                         (7, 3, 1, NOW()), (7, 3, 2, NOW()),
                                                                         (8, 4, 1, NOW()),
                                                                         (9, 5, 1, NOW()),
                                                                         (10, 2, 1, NOW());
-- ============================================
-- 11. THÊM RANKINGS
-- ============================================
INSERT INTO rankings (story_id, ranking_type, views, rank_position, ranking_date, created_at) VALUES
                                                                                                  (5, 'DAYLY', 125000, 1, CURDATE(), NOW()),
                                                                                                  (8, 'DAYLY', 98000, 2, CURDATE(), NOW()),
                                                                                                  (4, 'DAYLY', 87000, 3, CURDATE(), NOW()),
                                                                                                  (1, 'WEEKLY', 890000, 1, CURDATE(), NOW()),
                                                                                                  (5, 'WEEKLY', 765000, 2, CURDATE(), NOW()),
                                                                                                  (8, 'WEEKLY', 654000, 3, CURDATE(), NOW()),
                                                                                                  (8, 'MONTHLY', 3200000, 1, CURDATE(), NOW()),
                                                                                                  (5, 'MONTHLY', 2890000, 2, CURDATE(), NOW()),
                                                                                                  (1, 'MONTHLY', 2456000, 3, CURDATE(), NOW());
-- ============================================
-- 12. THÊM ACTIVITY LOGS
-- ============================================
INSERT INTO activity_logs (user_id, action, table_name, record_id, description, ip_address, created_at) VALUES
                                                                                                            (1, 'LOGIN', 'users', 1, 'Admin đăng nhập hệ thống', '192.168.1.1', NOW()),
                                                                                                            (3, 'CREATE', 'comments', 1, 'Người dùng tạo bình luận mới', '192.168.1.100', NOW()),
                                                                                                            (4, 'CREATE', 'ratings', 1, 'Người dùng đánh giá truyện', '192.168.1.101', NOW()),
                                                                                                            (5, 'READ', 'chapters', 1, 'Người dùng đọc chương 1', '192.168.1.102', NOW()),
                                                                                                            (1, 'UPDATE', 'stories', 1, 'Admin cập nhật thông tin truyện', '192.168.1.1', NOW());
-- ============================================
-- KẾT THÚC SCRIPT
-- ============================================
SELECT 'Fake data đã được tạo thành công!' AS message;