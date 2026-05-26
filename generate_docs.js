const fs = require('fs');
const path = require('path');

const ROOT_DIR = 'd:\\DoAnTotNghiep';

// --- HÀM TRỢ GIÚP ĐỂ TẠO CẤU TRÚC HTML WORD COMPATIBLE ---
function wrapHtml(title, bodyContent) {
  return `
<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
<head>
<meta charset="utf-8">
<title>${title}</title>
<!--[if gte mso 9]>
<xml>
 <w:WordDocument>
  <w:View>Print</w:View>
  <w:Zoom>100</w:Zoom>
  <w:DoNotOptimizeForBrowser/>
 </w:WordDocument>
</xml>
<![endif]-->
<style>
  @page {
    size: 8.27in 11.69in; /* A4 size */
    margin: 1.0in 1.0in 1.0in 1.25in; /* Top, Bottom, Right 2.5cm, Left 3cm */
  }
  body {
    font-family: 'Times New Roman', Times, serif;
    font-size: 12pt;
    line-height: 1.3;
    color: #000000;
  }
  h1 {
    font-size: 16pt;
    color: #1e3a8a; /* Dark Blue */
    font-weight: bold;
    text-align: center;
    margin-top: 18pt;
    margin-bottom: 18pt;
    line-height: 1.4;
  }
  h2 {
    font-size: 14pt;
    color: #1e3a8a;
    font-weight: bold;
    margin-top: 14pt;
    margin-bottom: 8pt;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 4px;
  }
  h3 {
    font-size: 12.5pt;
    color: #334155; /* Slate */
    font-weight: bold;
    margin-top: 12pt;
    margin-bottom: 6pt;
  }
  p {
    margin-top: 0;
    margin-bottom: 8pt;
    text-align: justify;
    text-indent: 0.5in; /* Thụt lề đầu dòng */
  }
  p.no-indent {
    text-indent: 0;
  }
  ul, ol {
    margin-top: 0;
    margin-bottom: 8pt;
    padding-left: 20px;
  }
  li {
    margin-bottom: 4pt;
    text-align: justify;
  }
  .bold-prefix {
    font-weight: bold;
  }
  table {
    border-collapse: collapse;
    width: 100%;
    margin-top: 12pt;
    margin-bottom: 12pt;
  }
  th, td {
    border: 1px solid #334155;
    padding: 8px 10px;
    font-size: 11pt;
    vertical-align: top;
    text-align: left;
  }
  th {
    background-color: #1e3a8a;
    color: #ffffff;
    font-weight: bold;
    text-align: center;
  }
  .table-title {
    font-weight: bold;
    font-style: italic;
    text-align: center;
    margin-top: 6pt;
    margin-bottom: 6pt;
    font-size: 11pt;
  }
  .highlight-box {
    background-color: #f8fafc;
    border-left: 4px solid #1e3a8a;
    padding: 10px 15px;
    margin-bottom: 10pt;
  }
</style>
</head>
<body>
  ${bodyContent}
</body>
</html>
  `.trim();
}

// ==============================================================================
// 1. FILE 1: MÔ TẢ HỆ THỐNG (1_Mo_Ta_He_Thong.doc)
// ==============================================================================
const motaContent = wrapHtml(
  'Báo cáo Mô tả Hệ thống Mộc Vị Restaurant',
  `
  <h1>BÁO CÁO MÔ TẢ HỆ THỐNG VÀ CHI TIẾT CÁC MÀN HÌNH<br>HỆ THỐNG QUẢN LÝ NHÀ HÀNG MỘC VỊ RESTAURANT</h1>
  
  <h2>1. Giới thiệu tổng quan dự án</h2>
  <p>Hệ thống Quản lý Nhà hàng Mộc Vị là một giải pháp công nghệ toàn diện phục vụ nhu cầu chuyển đổi số cho mô hình kinh doanh dịch vụ ăn uống (F&B). Dự án kết nối đồng bộ và tối ưu hóa các luồng công việc từ tiền sảnh đến hậu cần nhà bếp theo thời gian thực.</p>
  <p>Điểm đột phá của dự án là việc áp dụng các công nghệ tiên tiến nhất như kết nối WebSocket để truyền tải thông tin chế biến tức thời, thanh toán dynamic VietQR tự động hóa đối chiếu hóa đơn, và đặc biệt là tích hợp mô hình trí tuệ nhân tạo Gemini AI đóng vai trò là "chuyên gia tư vấn chéo món ăn" (cross-selling advice) nhằm tối đa hóa doanh thu trên mỗi bàn ăn của khách hàng.</p>
  
  <h2>2. Các vai trò người dùng trong hệ thống (User Roles)</h2>
  <p class="no-indent">Hệ thống phân cấp quyền truy cập bảo mật rõ ràng thành 4 vai trò thông qua cơ chế phân quyền bảo mật Spring Security ở Backend:</p>
  <ul>
    <li><span class="bold-prefix">Khách hàng (Guest / Customer):</span> Đối tượng trực tiếp sử dụng dịch vụ. Khách hàng có thể truy cập trang chủ công khai để xem thực đơn, đọc tin tức ẩm thực, gửi các phản hồi đánh giá món ăn (reviews) và thực hiện đặt bàn hẹn giờ trực tuyến trước khi đến. Khi có mặt tại nhà hàng, khách hàng quét mã QR đặt tại bàn để xem thực đơn số riêng biệt của bàn đó, trực tiếp gọi món và theo dõi quá trình chế biến của bếp.</li>
    <li><span class="bold-prefix">Nhân viên Phục vụ (Waiter):</span> Nhân viên vận hành tiền sảnh. Giao diện của Waiter hiển thị trực quan sơ đồ bàn ăn của nhà hàng, hỗ trợ khách hàng gọi thêm món, thực hiện chuyển bàn ăn tự động cho khách (chuyển đổi đơn đặt sang bàn trống mới), in hóa đơn tạm tính chứa VietQR động để khách thanh toán và trực tiếp nhận ý kiến tư vấn từ AI để mời khách dùng thêm các món ăn kèm phù hợp.</li>
    <li><span class="bold-prefix">Nhân viên Bếp (Kitchen):</span> Bộ phận chế biến món ăn. Bếp tiếp nhận các món ăn cần nấu theo thứ tự thời gian thực thông qua kết nối truyền tin WebSocket. Khi chế biến xong, bếp cập nhật trạng thái "Xong món" để gửi tín hiệu cho phục vụ bưng lên. Bếp cũng có quyền bật/tắt trạng thái hết món trực tiếp trên menu nếu nguyên liệu trong kho không còn đủ.</li>
    <li><span class="bold-prefix">Quản trị viên / Quản lý (Admin / Manager):</span> Người có quyền hạn cao nhất. Admin quản trị toàn bộ danh mục cốt lõi của nhà hàng bao gồm: tài khoản nhân viên và phân quyền, menu món ăn và giá cả, quản lý xuất-nhập kho nguyên liệu, thiết lập công thức hao phí chế biến (Recipes), tạo mã voucher khuyến mãi, quản lý tin bài sự kiện ẩm thực và theo dõi báo cáo doanh thu trực quan dưới dạng biểu đồ số liệu.</li>
  </ul>
  
  <h2>3. Các chức năng cốt lõi của hệ thống</h2>
  
  <h3>3.1. Đặt bàn hẹn giờ & Gọi món trực tuyến (Dine-in Order & Booking)</h3>
  <p>Khách hàng có thể chủ động đặt bàn trực tuyến qua Web công cộng bằng cách điền form đăng ký thông tin người đặt, số điện thoại, số lượng khách đi cùng và chọn tầng/vị trí bàn mong muốn. Khi đến ăn tại nhà hàng, hệ thống hỗ trợ gọi món tại chỗ (dine-in) thông qua mã QR được dán tại mỗi bàn, loại bỏ hoàn toàn các bước gọi món thủ công truyền thống.</p>
  
  <h3>3.2. Truyền tin và Đồng bộ thời gian thực (Real-time WebSockets)</h3>
  <p>Hệ thống tích hợp giao thức STOMP trên nền tảng WebSocket để đảm bảo sự phối hợp nhịp nhàng giữa Bếp và Phục vụ. Ngay khi Khách hàng hoặc Waiter gửi đơn gọi món, màn hình của Bếp sẽ hiển thị thông báo tức thời kèm tiếng chuông. Khi đầu bếp hoàn thành chế biến và nhấn "Xong món", màn hình Waiter phụ trách bàn đó sẽ nhấp nháy đỏ báo hiệu cần bưng món ra cho khách ngay lập tức, tối ưu hóa độ nóng sốt của đồ ăn.</p>
  
  <h3>3.3. Trợ lý AI bán hàng thông minh (Gemini AI Integration)</h3>
  <p>Hệ thống kết nối trực tiếp với API của Gemini AI để cung cấp tính năng tư vấn bán hàng vượt trội. Dựa trên danh sách các món ăn bàn khách hàng hiện đang gọi, AI sẽ phân tích khẩu vị và đề xuất kịch bản khéo léo để Waiter mời khách dùng thêm các món ăn kèm, đồ tráng miệng hoặc đồ uống phù hợp để gia tăng trải nghiệm ăn uống của khách và nâng cao doanh thu bán hàng.</p>
  
  <h3>3.4. Thanh toán dynamic VietQR thông minh</h3>
  <p>Mỗi hóa đơn tạm tính in ra đều tự động tích hợp mã VietQR động được tạo thông qua API của VietQR. Mã QR này chứa chính xác số tài khoản ngân hàng của nhà hàng, số tiền tổng cộng cần thanh toán và nội dung chuyển khoản tự động (Ví dụ: 'Thanh toan ban Ban 02'). Khách quét mã, hệ thống ngân hàng tự động điền các thông tin này giúp quá trình thanh toán diễn ra trong 5 giây, an toàn tuyệt đối và tránh nhầm lẫn số tiền.</p>
  
  <h2>4. Thiết kế các giao diện màn hình và biểu mẫu (Screens & Forms)</h2>
  <p class="no-indent">Hệ thống bao gồm các màn hình tương tác được tối ưu hóa hiển thị:</p>
  <ul>
    <li><span class="bold-prefix">Giao diện Trang chủ & Thực đơn Khách hàng:</span> Thiết kế sang trọng, hiện đại giới thiệu ẩm thực Mộc Vị, danh mục món ăn phân loại rõ ràng kèm hình ảnh trực quan sinh động, giỏ hàng nổi giúp khách hàng dễ dàng đặt món và ghi chú khẩu vị riêng.</li>
    <li><span class="bold-prefix">Màn hình Điều phối của Phục Vụ (Waiter Dashboard):</span> Hiển thị sơ đồ bàn ăn trực quan theo sơ đồ mặt bằng thực tế. Các bàn ăn được tô màu theo trạng thái vận hành: Xanh (Trống), Vàng (Đặt cọc trước), Đỏ (Bàn có khách đang ăn), Tím (Bàn cần dọn dẹp). Tích hợp khay hiển thị món ăn vừa chín cần bưng bưng ra bàn.</li>
    <li><span class="bold-prefix">Màn hình Chế biến của Bếp (Kitchen Dashboard):</span> Giao diện tối giản tối ưu hiển thị danh sách các món cần nấu xếp hàng theo thời gian gọi đơn của khách. Cho phép bếp tương tác chuyển trạng thái món ăn nhanh chóng qua các nút bấm "Bắt đầu nấu" và "Xong món".</li>
    <li><span class="bold-prefix">Bảng thống kê của Admin (Admin Dashboard):</span> Nơi hiển thị các con số tổng quan (Tổng doanh thu, số đơn hàng, mặt hàng bán chạy). Tích hợp hai biểu đồ trực quan động sử dụng thư viện Chart.js bao gồm Biểu đồ cột doanh thu 7 ngày gần nhất và Biểu đồ tròn thể hiện Top 5 món ăn bán chạy nhất hệ thống.</li>
    <li><span class="bold-prefix">Các Form quản lý danh mục (CRUD Forms):</span> Các biểu mẫu thêm mới/chỉnh sửa tài khoản nhân viên, thêm món ăn mới, nhập nguyên liệu tồn kho và khai báo định lượng chi tiết cho công thức nấu ăn.</li>
  </ul>
`
);

// ==============================================================================
// 2. FILE 2: USE CASE (2_Use_Case.doc)
// ==============================================================================
const usecaseContent = wrapHtml(
  'Báo cáo Phân tích Sơ đồ Use Case Mộc Vị Restaurant',
  `
  <h1>PHÂN TÍCH SƠ ĐỒ USE CASE CHI TIẾT HỆ THỐNG<br>QUẢN LÝ NHÀ HÀNG MỘC VỊ RESTAURANT</h1>
  
  <h2>1. Tổng quan các tác nhân tương tác (Actors)</h2>
  <p>Trong quá trình thiết kế hệ thống Quản lý Nhà hàng Mộc Vị, các chức năng được xây dựng xoay quanh 4 tác nhân chính nhằm phản ánh đúng quy trình hoạt động nghiệp vụ ngoài đời thực:</p>
  <ul>
    <li><span class="bold-prefix">Khách hàng (Customer):</span> Tác nhân ngoài hệ thống, truy cập thông qua thiết bị cá nhân (điện thoại, máy tính bảng) để tự phục vụ đặt bàn, đặt món.</li>
    <li><span class="bold-prefix">Phục vụ (Waiter):</span> Nhân viên trực tiếp vận hành tiền sảnh nhà hàng, tương tác liên tục với khách hàng tại bàn ăn và hệ thống điều phối.</li>
    <li><span class="bold-prefix">Bếp (Kitchen):</span> Nhân viên bộ phận chế biến hậu cần, tiếp nhận thông tin yêu cầu món ăn và phản hồi trạng thái hoàn thành.</li>
    <li><span class="bold-prefix">Quản lý / Admin (Admin/Manager):</span> Người quản trị kiểm soát dữ liệu, cấu hình danh mục thực đơn, kho hàng và theo dõi chỉ số tài chính.</li>
  </ul>
  
  <h2>2. Danh sách Use Case phân hệ người dùng</h2>
  
  <h3>2.1. Nhóm Use Case dành cho Khách hàng (Customer)</h3>
  <ul>
    <li><span class="bold-prefix">Xem Thực Đơn trực tuyến:</span> Xem danh mục các món ăn kèm giá cả, hình ảnh thực tế và mô tả.</li>
    <li><span class="bold-prefix">Đặt Bàn Hẹn Giờ:</span> Khai báo thông tin đặt bàn trước (ngày giờ, số người, vị trí tầng).</li>
    <li><span class="bold-prefix">Gọi Món Tại Bàn (Dine-in Order):</span> Quét mã QR tại bàn ăn thực tế, chọn món vào giỏ hàng và gửi yêu cầu nấu xuống bếp.</li>
    <li><span class="bold-prefix">Gửi Phản Hồi Đánh Giá (Review):</span> Đánh giá số sao (1-5 sao) kèm bình luận nhận xét về món ăn sau khi trải nghiệm.</li>
    <li><span class="bold-prefix">Đăng ký / Đăng nhập thành viên:</span> Tạo tài khoản để tích điểm loyalty khi thanh toán hóa đơn.</li>
  </ul>
  
  <h3>2.2. Nhóm Use Case dành cho Nhân viên Phục vụ (Waiter)</h3>
  <ul>
    <li><span class="bold-prefix">Xem Sơ Đồ Bàn Ăn:</span> Theo dõi trạng thái trống/có khách/chờ dọn của tất cả bàn ăn theo thời gian thực.</li>
    <li><span class="bold-prefix">Gọi Món Hộ Khách:</span> Hỗ trợ khách gọi thêm món ăn trực tiếp trên thiết bị phục vụ cầm tay.</li>
    <li><span class="bold-prefix">Chuyển Bàn Ăn (Move Table):</span> Chuyển toàn bộ danh sách món ăn đang gọi của khách từ bàn cũ sang bàn trống mới theo yêu cầu của khách.</li>
    <li><span class="bold-prefix">Xác Nhận Đã Bưng Món (Mark Served):</span> Click xác nhận đã đưa món ăn chín từ bếp ra đặt lên bàn cho khách.</li>
    <li><span class="bold-prefix">In Hóa Đơn Tạm Tính:</span> Xuất hóa đơn kèm VietQR động để khách tiến hành thanh toán tại bàn.</li>
    <li><span class="bold-prefix">Tham khảo Trợ Lý AI gợi ý:</span> Click xem kịch bản gợi ý mời món từ Gemini AI dựa trên thực đơn bàn đang gọi.</li>
  </ul>
  
  <h3>2.3. Nhóm Use Case dành cho Nhân viên Bếp (Kitchen)</h3>
  <ul>
    <li><span class="bold-prefix">Xem Danh Sách Chế Biến:</span> Tiếp nhận các món ăn cần nấu theo thời gian thực tự động xếp hàng.</li>
    <li><span class="bold-prefix">Cập nhật chế biến (Update Cooking):</span> Chuyển đổi trạng thái từ "Chờ nấu" -> "Đang nấu" -> "Đã xong" để hệ thống tự động thông báo phục vụ.</li>
    <li><span class="bold-prefix">Báo Hết Món Trực Tiếp:</span> Chuyển đổi trạng thái sẵn có của sản phẩm sang Hết hàng nếu nguyên liệu bếp cạn kiệt.</li>
  </ul>
  
  <h3>2.4. Nhóm Use Case dành cho Quản trị viên (Admin/Manager)</h3>
  <ul>
    <li><span class="bold-prefix">Quản lý Tài Khoản & Phân Quyền (CRUD Accounts):</span> Quản lý nhân viên nhà hàng và gán vai trò tương ứng (Admin, Waiter, Kitchen).</li>
    <li><span class="bold-prefix">Quản lý Thực Đơn (CRUD Products):</span> Thêm mới, sửa thông tin món ăn, thay đổi đơn giá bán, phân nhóm danh mục (Categories).</li>
    <li><span class="bold-prefix">Quản lý Kho & Định Lượng (CRUD Inventory & Recipes):</span> Theo dõi tồn kho nguyên liệu thô, định lượng hao hụt nguyên liệu thô khi hoàn thành món.</li>
    <li><span class="bold-prefix">Xem Báo Cáo Doanh Thu:</span> Phân tích trực quan doanh số bán hàng, xem biểu đồ doanh thu ngày và thống kê sản phẩm được ưa chuộng nhất.</li>
  </ul>
  
  <h2>3. Đặc tả chi tiết các Use Case nghiệp vụ chính</h2>
  <div class="table-title">Bảng 1: Đặc tả luồng xử lý các Use Case cốt lõi</div>
  <table>
    <thead>
      <tr>
        <th style="width: 25%">Tên Use Case</th>
        <th style="width: 20%">Tác nhân tương tác</th>
        <th style="width: 55%">Mô tả chi tiết luồng xử lý chính</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="bold-prefix">Gọi món qua mã QR tại bàn</td>
        <td>Khách hàng / Phục vụ</td>
        <td>Khách quét mã QR tại bàn để truy cập menu số -> Chọn món và nhập ghi chú -> Nhấn 'Gửi yêu cầu' -> Hệ thống tạo đơn hàng với trạng thái 'Đang nấu' -> Gửi tín hiệu WebSocket thông báo xuống Bếp tức thì.</td>
      </tr>
      <tr>
        <td class="bold-prefix">Tiếp nhận và xử lý tại Bếp</td>
        <td>Đầu bếp (Kitchen)</td>
        <td>Bếp nhận âm thanh chuông báo món mới -> Click nút 'Bắt đầu nấu' để khách/phục vụ biết món đang xử lý -> Click 'Xong món' -> Hệ thống tự động dựa vào công thức định lượng (Recipes) để trừ lượng nguyên liệu tương ứng trong kho (Ingredients), đồng thời bắn tín hiệu WebSocket báo hiệu xong món lên màn hình Waiter.</td>
      </tr>
      <tr>
        <td class="bold-prefix">Mời món thông minh bằng AI</td>
        <td>Nhân viên Phục vụ</td>
        <td>Waiter mở chi tiết bàn ăn đang phục vụ -> Nhấn nút 'AI Gợi ý mời món' -> Hệ thống gom danh sách tên món bàn đó đang ăn gửi lên Gemini AI kèm prompt tối ưu -> AI phân tích khẩu vị và phản hồi kịch bản đề xuất (Ví dụ gợi ý thêm nước ép dưa hấu giải nhiệt) -> Waiter ra tư vấn giới thiệu trực tiếp cho khách.</td>
      </tr>
      <tr>
        <td class="bold-prefix">Thanh toán hóa đơn nhanh VietQR</td>
        <td>Phục vụ / Khách hàng</td>
        <td>Waiter nhấn in hóa đơn tạm tính -> Hệ thống gọi API ngân hàng tạo VietQR động chứa đúng số tiền tổng cộng và số bàn -> Xuất hóa đơn kèm mã QR -> Khách quét mã chuyển tiền -> Sau khi xác nhận nhận tiền, bàn ăn được Waiter bấm chuyển sang trạng thái 'Cần dọn dẹp'.</td>
      </tr>
    </tbody>
  </table>
`
);

// ==============================================================================
// 3. FILE 3: THIẾT KẾ DATABASE (3_Database_Design.doc)
// ==============================================================================
const dbContent = wrapHtml(
  'Báo cáo Thiết kế Cơ sở Dữ liệu Mộc Vị Restaurant',
  `
  <h1>TÀI LIỆU THIẾT KẾ CƠ SỞ DỮ LIỆU CHI TIẾT SYSTEM<br>QUẢN LÝ NHÀ HÀNG MỘC VỊ RESTAURANT</h1>
  
  <h2>1. Tổng quan cấu trúc Cơ sở dữ liệu</h2>
  <p>Cơ sở dữ liệu của dự án Mộc Vị Restaurant được vận hành trên hệ quản trị cơ sở dữ liệu Microsoft SQL Server. Mô hình thực thể liên kết được thiết kế chuẩn hóa cao đạt chuẩn 3NF, bảo đảm tính toàn vẹn của dữ liệu trong quá trình ghi nhận đơn hàng, tính tiền, và theo dõi lượng nguyên liệu xuất nhập kho bếp.</p>
  <p>Hệ thống bao gồm tổng cộng 13 bảng dữ liệu chính, được chia thành 4 phân hệ chính liên kết chặt chẽ:</p>
  <ul>
    <li><span class="bold-prefix">Phân hệ Tài khoản và Phân quyền:</span> Gồm các bảng Accounts, Roles, Authorities thiết lập kiểm soát đăng nhập an toàn.</li>
    <li><span class="bold-prefix">Phân hệ Thực đơn và Kho chế biến:</span> Gồm các bảng Categories, Products, Ingredients, Recipes phục vụ việc trừ kho tự động khi bếp nấu xong món ăn theo công thức hao phí được khai báo sẵn.</li>
    <li><span class="bold-prefix">Phân hệ Tiền sảnh và Vận hành:</span> Gồm các bảng restaurant_table (Sơ đồ bàn), Orders, OrderDetails lưu trữ giao dịch hóa đơn bán hàng.</li>
    <li><span class="bold-prefix">Phân hệ Tương tác cộng thêm:</span> Gồm các bảng Reviews (Đánh giá), Vouchers (Khuyến mãi), Posts & Applications (Tin tức & Tuyển dụng nhân sự).</li>
  </ul>
  
  <h2>2. Đặc tả chi tiết cấu trúc các bảng dữ liệu chính</h2>
  
  <h3>2.1. Bảng Accounts (Quản lý thông tin tài khoản người dùng)</h3>
  <div class="table-title">Cấu trúc chi tiết bảng Accounts</div>
  <table>
    <thead>
      <tr>
        <th>Tên Cột (Cột)</th>
        <th>Kiểu Dữ Liệu</th>
        <th>Khóa</th>
        <th>Null</th>
        <th>Mô tả / Ràng buộc</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="bold-prefix">username</td>
        <td>VARCHAR(50)</td>
        <td>PK</td>
        <td>No</td>
        <td>Tên đăng nhập duy nhất dùng đăng nhập hệ thống</td>
      </tr>
      <tr>
        <td class="bold-prefix">password</td>
        <td>VARCHAR(100)</td>
        <td>-</td>
        <td>No</td>
        <td>Mật khẩu đã mã hóa của người dùng</td>
      </tr>
      <tr>
        <td class="bold-prefix">fullname</td>
        <td>NVARCHAR(200)</td>
        <td>-</td>
        <td>No</td>
        <td>Họ và tên đầy đủ người dùng</td>
      </tr>
      <tr>
        <td class="bold-prefix">email</td>
        <td>VARCHAR(100)</td>
        <td>-</td>
        <td>No</td>
        <td>Địa chỉ email liên lạc</td>
      </tr>
      <tr>
        <td class="bold-prefix">photo</td>
        <td>VARCHAR(255)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Đường dẫn ảnh đại diện avatar</td>
      </tr>
      <tr>
        <td class="bold-prefix">total_spent</td>
        <td>FLOAT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Tổng số tiền tích lũy tiêu dùng (Mặc định 0.0)</td>
      </tr>
      <tr>
        <td class="bold-prefix">loyalty_points</td>
        <td>INT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Điểm tích lũy thành viên (Mặc định 0)</td>
      </tr>
      <tr>
        <td class="bold-prefix">tier</td>
        <td>VARCHAR(20)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Hạng thành viên (Mặc định 'BRONZE')</td>
      </tr>
    </tbody>
  </table>

  <h3>2.2. Bảng Products (Thông tin món ăn trong thực đơn)</h3>
  <div class="table-title">Cấu trúc chi tiết bảng Products</div>
  <table>
    <thead>
      <tr>
        <th>Tên Cột</th>
        <th>Kiểu Dữ Liệu</th>
        <th>Khóa</th>
        <th>Null</th>
        <th>Mô tả / Ràng buộc</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="bold-prefix">id</td>
        <td>INT (IDENTITY)</td>
        <td>PK</td>
        <td>No</td>
        <td>Mã món ăn tự tăng</td>
      </tr>
      <tr>
        <td class="bold-prefix">name</td>
        <td>NVARCHAR(200)</td>
        <td>-</td>
        <td>No</td>
        <td>Tên món ăn trong thực đơn</td>
      </tr>
      <tr>
        <td class="bold-prefix">price</td>
        <td>FLOAT</td>
        <td>-</td>
        <td>No</td>
        <td>Giá niêm yết bán sản phẩm món ăn</td>
      </tr>
      <tr>
        <td class="bold-prefix">image</td>
        <td>VARCHAR(255)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Đường dẫn hình ảnh chụp món ăn thực tế</td>
      </tr>
      <tr>
        <td class="bold-prefix">description</td>
        <td>NVARCHAR(MAX)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Mô tả thành phần, khẩu vị món ăn</td>
      </tr>
      <tr>
        <td class="bold-prefix">create_date</td>
        <td>DATE</td>
        <td>-</td>
        <td>Yes</td>
        <td>Ngày tạo món (Mặc định GETDATE())</td>
      </tr>
      <tr>
        <td class="bold-prefix">available</td>
        <td>BIT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Trạng thái bán (1: Đang bán, 0: Ngừng bán)</td>
      </tr>
      <tr>
        <td class="bold-prefix">category_id</td>
        <td>INT</td>
        <td>FK</td>
        <td>Yes</td>
        <td>Mã liên kết tới danh mục món ăn (Categories)</td>
      </tr>
    </tbody>
  </table>

  <h3>2.3. Bảng Orders (Thông tin đơn đặt bàn / đặt món)</h3>
  <div class="table-title">Cấu trúc chi tiết bảng Orders</div>
  <table>
    <thead>
      <tr>
        <th>Tên Cột</th>
        <th>Kiểu Dữ Liệu</th>
        <th>Khóa</th>
        <th>Null</th>
        <th>Mô tả / Ràng buộc</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="bold-prefix">id</td>
        <td>INT (IDENTITY)</td>
        <td>PK</td>
        <td>No</td>
        <td>Mã hóa đơn tự tăng</td>
      </tr>
      <tr>
        <td class="bold-prefix">create_date</td>
        <td>DATETIME</td>
        <td>-</td>
        <td>Yes</td>
        <td>Ngày giờ tạo đơn (Mặc định GETDATE())</td>
      </tr>
      <tr>
        <td class="bold-prefix">address</td>
        <td>NVARCHAR(500)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Ghi nhận thông tin số bàn (Ví dụ: 'Bàn: Bàn 05') hoặc địa chỉ giao</td>
      </tr>
      <tr>
        <td class="bold-prefix">status</td>
        <td>INT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Trạng thái đơn hàng: 1: Đang nấu, 2: Xong món, 3: Đang ăn, 4: Đã thanh toán, 5: Hẹn giờ</td>
      </tr>
      <tr>
        <td class="bold-prefix">note</td>
        <td>NVARCHAR(MAX)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Ghi chú chế biến riêng biệt của thực khách</td>
      </tr>
      <tr>
        <td class="bold-prefix">voucher_code</td>
        <td>VARCHAR(50)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Mã voucher ưu đãi đã áp dụng cho hóa đơn</td>
      </tr>
      <tr>
        <td class="bold-prefix">username</td>
        <td>VARCHAR(50)</td>
        <td>FK</td>
        <td>Yes</td>
        <td>Tài khoản đặt đơn, liên kết tới bảng Accounts</td>
      </tr>
    </tbody>
  </table>

  <h3>2.4. Bảng restaurant_table (Sơ đồ các bàn ăn tại tiền sảnh)</h3>
  <div class="table-title">Cấu trúc chi tiết bảng restaurant_table</div>
  <table>
    <thead>
      <tr>
        <th>Tên Cột</th>
        <th>Kiểu Dữ Liệu</th>
        <th>Khóa</th>
        <th>Null</th>
        <th>Mô tả / Ràng buộc</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="bold-prefix">id</td>
        <td>INT (IDENTITY)</td>
        <td>PK</td>
        <td>No</td>
        <td>Mã định danh bàn ăn</td>
      </tr>
      <tr>
        <td class="bold-prefix">name</td>
        <td>NVARCHAR(255)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Tên số bàn (Ví dụ: Bàn 01, Bàn 02)</td>
      </tr>
      <tr>
        <td class="bold-prefix">floor</td>
        <td>NVARCHAR(255)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Khu vực tầng của bàn ăn (Tầng 1, Tầng 2, Sân vườn)</td>
      </tr>
      <tr>
        <td class="bold-prefix">is_occupied</td>
        <td>INT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Trạng thái hoạt động: 0: Trống, 1: Đặt cọc trước, 2: Có khách ăn, 3: Chờ dọn dẹp</td>
      </tr>
      <tr>
        <td class="bold-prefix">has_view</td>
        <td>BIT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Có cảnh quan view đẹp hay không (Mặc định 0)</td>
      </tr>
      <tr>
        <td class="bold-prefix">reserved_time</td>
        <td>VARCHAR(255)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Thời điểm khách hẹn trước qua web</td>
      </tr>
      <tr>
        <td class="bold-prefix">capacity</td>
        <td>INT</td>
        <td>-</td>
        <td>Yes</td>
        <td>Số ghế tối đa phục vụ tại bàn (Mặc định 4)</td>
      </tr>
      <tr>
        <td class="bold-prefix">view_type</td>
        <td>NVARCHAR(50)</td>
        <td>-</td>
        <td>Yes</td>
        <td>Loại view cảnh quan chi tiết (Cửa sổ, Ban công, Bể cá)</td>
      </tr>
    </tbody>
  </table>

  <h3>2.5. Bảng Ingredients & Recipes (Kho nguyên liệu và Định lượng chế biến)</h3>
  <p class="no-indent"><span class="bold-prefix">Ingredients:</span> Quản lý lượng tồn kho nguyên liệu trong bếp nhà hàng. Bảng chứa mã ID nguyên liệu thô (PK), name nguyên liệu, quantity còn trong kho, unit (đơn vị: kg, lít, gram) và min_stock (mức tồn tối thiểu để báo động nhập hàng).</p>
  <p class="no-indent"><span class="bold-prefix">Recipes:</span> Thiết lập định lượng hao phí để sản xuất ra món ăn. Bảng chứa mã ID công thức (PK), product_id (FK liên kết Products), ingredient_id (FK liên kết Ingredients) và amount_required (khối lượng nguyên liệu tiêu thụ chính xác cho 1 phần ăn). Khi Bếp báo nấu xong món ăn, hệ thống tự động trừ kho nguyên liệu thô tương ứng dựa theo bảng Recipes này.</p>
`
);

// ==============================================================================
// 4. FILE 4: CÔNG NGHỆ DỰ ÁN (4_Cong_Nghe_Du_An.doc)
// ==============================================================================
const techContent = wrapHtml(
  'Báo cáo Công nghệ và Kiến trúc Hệ thống Mộc Vị Restaurant',
  `
  <h1>BÁO CÁO CÔNG NGHỆ VÀ THIẾT KẾ KIẾN TRÚC HỆ THỐNG<br>HỆ THỐNG QUẢN LÝ NHÀ HÀNG MỘC VỊ RESTAURANT</h1>
  
  <h2>1. Thiết kế kiến trúc tổng thể dự án</h2>
  <p>Dự án Quản lý Nhà hàng Mộc Vị được xây dựng theo mô hình kiến trúc phân lớp hiện đại Single Page Application (SPA) giúp tách rời hoàn toàn giao diện người dùng (Frontend) và logic xử lý cốt lõi phía máy chủ (Backend). Mô hình này mang lại khả năng mở rộng hệ thống tốt, tối ưu hóa lưu lượng băng thông truyền tải và cải thiện trải nghiệm mượt mà giống như phần mềm Desktop cài đặt sẵn.</p>
  <p>Các phương thức trao đổi dữ liệu chính trong kiến trúc:</p>
  <ul>
    <li><span class="bold-prefix">RESTful API (JSON):</span> Frontend giao tiếp bất đồng bộ gửi yêu cầu lên Backend và nhận lại phản hồi dữ liệu cấu trúc JSON, giúp cập nhật trang web nhanh chóng mà không cần reload trang.</li>
    <li><span class="bold-prefix">WebSocket TCP Connection:</span> Duy trì một đường truyền song công liên tục giữa máy chủ và các trình duyệt (Waiter, Kitchen) để truyền tải nhanh các sự kiện real-time như bếp xong món hoặc có đơn hàng mới từ bàn ăn của khách.</li>
    <li><span class="bold-prefix">Security Filters (State-less):</span> Sử dụng cơ chế xác thực phi trạng thái. Backend không duy trì Session của user trên RAM mà sử dụng chuỗi Token mã hóa JWT đính kèm ở mỗi Request từ client để chứng thực phân quyền API.</li>
  </ul>
  
  <h2>2. Công nghệ phát triển phía Backend (Spring Boot Stack)</h2>
  <p>Bộ phận Backend xử lý toàn bộ logic tính toán, phân quyền bảo mật, quản trị dữ liệu và tích hợp các dịch vụ bên thứ ba. Công nghệ sử dụng bao gồm:</p>
  <ul>
    <li><span class="bold-prefix">Spring Boot (Java 21):</span> Sử dụng phiên bản Java LTS mới nhất mang lại hiệu năng xử lý đa luồng vượt trội, kết hợp tính năng nhúng sẵn Web Server Tomcat giúp việc khởi chạy máy chủ vô cùng đơn giản.</li>
    <li><span class="bold-prefix">Spring Security & JWT:</span> Quản lý xác thực và phân quyền cực kỳ chặt chẽ cấp độ Method. Sử dụng NoOpPasswordEncoder mã hóa mật khẩu và cơ chế JSON Web Token sinh Token chứa vai trò tài khoản gửi về client.</li>
    <li><span class="bold-prefix">Spring Data JPA & Hibernate:</span> Công cụ Object-Relational Mapping (ORM) hàng đầu, tự động ánh xạ cơ sở dữ liệu SQL Server thành các đối tượng Java (Entity), bảo vệ mã nguồn tuyệt đối khỏi lỗ hổng SQL Injection.</li>
    <li><span class="bold-prefix">Spring WebSocket (STOMP):</span> Thiết lập Message Broker nội bộ giúp đăng ký các kênh nhận tin nhắn (/topic/kitchen, /topic/waiter) và đẩy thông tin real-time tới client tức thì.</li>
    <li><span class="bold-prefix">Google Gemini AI Integration:</span> Sử dụng SDK thế hệ mới của Google để đưa mô hình ngôn ngữ lớn (LLM) vào hệ thống thông qua controller gọi API, hỗ trợ chức năng gợi ý mời món thông minh tại bàn.</li>
  </ul>
  
  <h2>3. Công nghệ phát triển phía Frontend (Vue 3 Stack)</h2>
  <p>Giao diện người dùng được tối ưu hóa cho cả thiết bị máy tính để bàn (Admin) và thiết bị di động cầm tay (Khách hàng gọi món, Nhân viên phục vụ đi lại tiền sảnh):</p>
  <ul>
    <li><span class="bold-prefix">Vue 3 (Composition API):</span> JavaScript Framework hiện đại nhất sử dụng cơ chế Virtual DOM giúp render giao diện siêu tốc, kết hợp cú pháp Composition API giúp cấu trúc code sạch sẽ, dễ bảo trì.</li>
    <li><span class="bold-prefix">Vite Build Tool:</span> Công cụ build thay thế Webpack mang lại tốc độ biên dịch mã nguồn cực nhanh, tối ưu hóa kích thước file JS đầu ra khi triển khai.</li>
    <li><span class="bold-prefix">Pinia State Management:</span> State store quản lý tập trung trạng thái đăng nhập tài khoản, quyền hạn user, và thông tin giỏ hàng món ăn xuyên suốt các màn hình.</li>
    <li><span class="bold-prefix">Axios:</span> Thư viện gửi yêu cầu HTTP Client bất đồng bộ mạnh mẽ, hỗ trợ viết Interceptors để tự động đính kèm Token JWT lấy từ localStorage vào header của mỗi API Request.</li>
    <li><span class="bold-prefix">SockJS & StompJS:</span> Thư viện JavaScript hỗ trợ kết nối WebSocket ổn định tới máy chủ Spring Boot, tự động reconnect nếu gặp sự cố đứt mạng.</li>
    <li><span class="bold-prefix">Chart.js:</span> Thư viện vẽ biểu đồ phân tích thống kê doanh số và món ăn bán chạy trực quan, sống động.</li>
    <li><span class="bold-prefix">VietQR API Integration:</span> API tạo hình ảnh mã QR động tích hợp thông tin hóa đơn giúp thanh toán tự động qua App ngân hàng.</li>
  </ul>
  
  <h2>4. Hệ Quản trị Cơ sở dữ liệu và Hạ tầng vận hành</h2>
  <ul>
    <li><span class="bold-prefix">Microsoft SQL Server:</span> Hệ quản trị CSDL quan hệ ổn định của Microsoft, đáp ứng đầy đủ các tiêu chuẩn khắt khe về toàn vẹn ACID của giao dịch thanh toán hóa đơn.</li>
    <li><span class="bold-prefix">Hạ tầng triển khai:</span> Dự án chạy thử nghiệm hoàn hảo trên môi trường cục bộ Localhost tích hợp máy chủ Tomcat ảo của Spring Boot và máy chủ dev của Vite phục vụ việcPair-Programming và kiểm thử.</li>
  </ul>
`
);

// --- HÀM GHI FILE DOCX (DẠNG HTML WORD COMPATIBLE) ---
function saveDoc(filename, content) {
  const filepath = path.join(ROOT_DIR, filename);
  fs.writeFileSync(filepath, content, 'utf-8');
  console.log(`[Node.js] Đã tạo thành công file Word: ${filepath}`);
}

console.log("--- BẮT ĐẦU QUÁ TRÌNH TẠO 4 FILE WORD CHO ĐỒ ÁN TỐT NGHỆP ---");
saveDoc('1_Mo_Ta_He_Thong.doc', motaContent);
saveDoc('2_Use_Case.doc', usecaseContent);
saveDoc('3_Database_Design.doc', dbContent);
saveDoc('4_Cong_Nghe_Du_An.doc', techContent);
console.log("--- HOÀN THÀNH TẤT CẢ 4 FILE WORD (.doc) CỰC KỲ CHUYÊN NGHIỆP! ---");
