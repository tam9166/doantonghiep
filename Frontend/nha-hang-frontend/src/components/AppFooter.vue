<template>
  <footer class="app-footer">
    <!-- Wave separator -->
    <div class="footer-wave">
      <svg viewBox="0 0 1440 120" preserveAspectRatio="none">
        <path d="M0,64 C360,120 720,0 1080,64 C1260,96 1380,80 1440,64 L1440,120 L0,120 Z" fill="currentColor"/>
      </svg>
    </div>

    <div class="footer-content">
      <div class="footer-grid">
        <!-- Brand Column -->
        <div class="footer-col footer-brand-col">
          <div class="footer-brand">
            <span class="footer-brand-icon">🍽️</span>
            <div>
              <h3>NHÀ HÀNG <span>MỘC VỊ</span></h3>
              <p class="footer-brand-sub">Tinh hoa ẩm thực Việt</p>
            </div>
          </div>
          <p class="footer-desc">
            Nhà hàng Mộc Vị — nơi giao thoa giữa hương vị truyền thống và phong cách hiện đại, mang đến trải nghiệm ẩm thực hoàn hảo tại Đà Nẵng.
          </p>
          <div class="footer-socials">
            <a href="#" class="social-link" title="Facebook">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
            </a>
            <a href="#" class="social-link" title="Instagram">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/></svg>
            </a>
            <a href="#" class="social-link" title="TikTok">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.59 6.69a4.83 4.83 0 01-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 01-2.88 2.5 2.89 2.89 0 01-2.89-2.89 2.89 2.89 0 012.89-2.89c.28 0 .54.04.79.11V9.01a6.33 6.33 0 00-.79-.05 6.34 6.34 0 00-6.34 6.34 6.34 6.34 0 006.34 6.34A6.34 6.34 0 0015.83 15.3V8.73a8.19 8.19 0 003.76 1.04V6.69z"/></svg>
            </a>
          </div>
        </div>

        <!-- Quick Links -->
        <div class="footer-col">
          <h4>Khám Phá</h4>
          <ul>
            <li><router-link to="/menu">📖 Thực đơn</router-link></li>
            <li><router-link to="/reservation">📅 Đặt bàn</router-link></li>
            <li><router-link to="/dine-in">🍽️ Gọi món tại quán</router-link></li>
            <li><router-link to="/history">📜 Lịch sử đơn hàng</router-link></li>
          </ul>
        </div>

        <!-- Contact Info -->
        <div class="footer-col">
          <h4>Liên Hệ</h4>
          <ul class="contact-list">
            <li>
              <span class="contact-icon">📍</span>
              <span>137 Nguyễn Thị Thập, Hải Châu, Đà Nẵng</span>
            </li>
            <li>
              <span class="contact-icon">📞</span>
              <a href="tel:0123456789">0123.456.789</a>
            </li>
            <li>
              <span class="contact-icon">✉️</span>
              <a href="mailto:contact@mocvi.vn">contact@mocvi.vn</a>
            </li>
            <li>
              <span class="contact-icon">🕐</span>
              <span>T2-T6: 10:00 - 22:00 | T7-CN: 09:00 - 23:00</span>
            </li>
          </ul>
        </div>

        <!-- Newsletter -->
        <div class="footer-col">
          <h4>Nhận Ưu Đãi</h4>
          <p class="newsletter-desc">Đăng ký nhận thông tin khuyến mãi và ưu đãi đặc biệt từ Mộc Vị.</p>
          <div class="newsletter-form">
            <input type="email" placeholder="Email của bạn..." class="newsletter-input" />
            <button class="newsletter-btn">→</button>
          </div>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div class="footer-bottom">
        <p>© {{ currentYear }} NHÀ HÀNG MỘC VỊ — Đà Nẵng. Đồ án tốt nghiệp FPT Polytechnic.</p>
        <div class="footer-bottom-links">
          <a href="#">Chính sách bảo mật</a>
          <a href="#">Điều khoản sử dụng</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.app-footer {
  position: relative;
  background: linear-gradient(180deg, var(--bg-root) 0%, #050b18 100%);
  color: var(--text-secondary);
  overflow: hidden;
}

/* Wave */
.footer-wave {
  color: var(--bg-root);
  margin-bottom: -2px;
}
.footer-wave svg {
  display: block;
  width: 100%;
  height: 80px;
}

.footer-content {
  padding: 60px 40px 30px;
  max-width: 1300px;
  margin: 0 auto;
}

/* Grid */
.footer-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1.2fr;
  gap: 50px;
  margin-bottom: 50px;
}

/* Brand */
.footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.footer-brand-icon {
  font-size: 2rem;
  filter: drop-shadow(0 0 12px rgba(0, 212, 170, 0.5));
}
.footer-brand h3 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 900;
  color: var(--text-heading);
  letter-spacing: 1px;
}
.footer-brand h3 span { color: var(--primary); }
.footer-brand-sub {
  margin: 0;
  font-size: 0.7rem;
  color: var(--text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
}
.footer-desc {
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--text-muted);
  margin-bottom: 20px;
}

/* Socials */
.footer-socials { display: flex; gap: 10px; }
.social-link {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  transition: var(--transition);
}
.social-link:hover {
  background: rgba(0, 212, 170, 0.1);
  border-color: rgba(0, 212, 170, 0.3);
  color: var(--primary);
  transform: translateY(-3px);
}

/* Columns */
.footer-col h4 {
  color: var(--text-heading);
  font-size: 0.95rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin: 0 0 20px 0;
  position: relative;
  padding-bottom: 12px;
}
.footer-col h4::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 30px;
  height: 3px;
  background: linear-gradient(90deg, var(--primary), transparent);
  border-radius: 3px;
}

.footer-col ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.footer-col ul li {
  margin-bottom: 10px;
}
.footer-col ul a {
  color: var(--text-muted);
  text-decoration: none;
  font-size: 0.88rem;
  transition: var(--transition);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.footer-col ul a:hover {
  color: var(--primary);
  transform: translateX(4px);
}

/* Contact */
.contact-list li {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  font-size: 0.85rem;
  line-height: 1.5;
}
.contact-icon { flex-shrink: 0; }
.contact-list a {
  color: var(--text-muted);
}
.contact-list a:hover { color: var(--primary); transform: none; }

/* Newsletter */
.newsletter-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 16px;
}
.newsletter-form {
  display: flex;
  gap: 0;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.newsletter-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.04);
  border: none;
  padding: 12px 16px;
  color: var(--text-primary);
  font-family: inherit;
  font-size: 0.88rem;
  outline: none;
}
.newsletter-input::placeholder { color: var(--text-muted); }
.newsletter-btn {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  border: none;
  color: var(--bg-dark);
  padding: 12px 20px;
  cursor: pointer;
  font-size: 1.2rem;
  font-weight: 800;
  transition: var(--transition);
}
.newsletter-btn:hover {
  background: var(--primary);
  box-shadow: 0 0 20px rgba(0, 212, 170, 0.4);
}

/* Bottom */
.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.footer-bottom p {
  margin: 0;
  font-size: 0.82rem;
  color: var(--text-muted);
}
.footer-bottom-links {
  display: flex;
  gap: 20px;
}
.footer-bottom-links a {
  color: var(--text-muted);
  text-decoration: none;
  font-size: 0.82rem;
  transition: var(--transition);
}
.footer-bottom-links a:hover { color: var(--primary); }

/* Responsive */
@media (max-width: 1024px) {
  .footer-grid { grid-template-columns: 1fr 1fr; gap: 40px; }
}
@media (max-width: 640px) {
  .footer-grid { grid-template-columns: 1fr; gap: 36px; }
  .footer-content { padding: 40px 20px 24px; }
  .footer-bottom { flex-direction: column; text-align: center; }
}
</style>
