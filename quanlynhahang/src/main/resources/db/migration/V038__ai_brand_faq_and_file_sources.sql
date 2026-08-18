IF COL_LENGTH('ai_knowledge_sources', 'original_filename') IS NULL ALTER TABLE ai_knowledge_sources ADD original_filename NVARCHAR(255) NULL;
IF COL_LENGTH('ai_knowledge_sources', 'mime_type') IS NULL ALTER TABLE ai_knowledge_sources ADD mime_type VARCHAR(100) NULL;
IF COL_LENGTH('ai_knowledge_sources', 'processing_status') IS NULL ALTER TABLE ai_knowledge_sources ADD processing_status VARCHAR(20) NOT NULL CONSTRAINT df_ai_source_processing DEFAULT 'READY';
IF COL_LENGTH('ai_knowledge_sources', 'processing_error') IS NULL ALTER TABLE ai_knowledge_sources ADD processing_error NVARCHAR(500) NULL;

IF OBJECT_ID('ai_brand_profile', 'U') IS NULL
BEGIN
 CREATE TABLE ai_brand_profile(id INT PRIMARY KEY, brand_name NVARCHAR(150), addressing NVARCHAR(300), tone_of_voice NVARCHAR(500), preferred_words NVARCHAR(MAX), forbidden_words NVARCHAR(MAX), advice_style NVARCHAR(MAX), unknown_answer_rule NVARCHAR(MAX), no_fabrication_rule NVARCHAR(MAX), handoff_rule NVARCHAR(MAX));
 INSERT INTO ai_brand_profile(id,brand_name,addressing,tone_of_voice,unknown_answer_rule,no_fabrication_rule,handoff_rule) VALUES(1,N'Mộc Vị Restaurant',N'Xưng Nhà hàng và gọi khách là Quý khách',N'Thân thiện, lịch sự, ngắn gọn',N'Nói rõ chưa có thông tin và mời khách liên hệ nhân viên',N'Không bịa giá, món ăn, bàn trống hoặc chính sách',N'Chuyển nhân viên khi cần xác nhận dữ liệu động');
END;
IF OBJECT_ID('ai_faq_examples', 'U') IS NULL
BEGIN
 CREATE TABLE ai_faq_examples(id BIGINT IDENTITY(1,1) PRIMARY KEY, question NVARCHAR(1000) NOT NULL, ideal_answer NVARCHAR(MAX) NOT NULL, enabled BIT NOT NULL CONSTRAINT df_ai_faq_enabled DEFAULT 1);
END;
