const fs = require('fs');
const path = require('path');

const viewsDir = path.join(__dirname, 'src', 'views');

const customerFiles = ['CustomerProfile.vue', 'Reservation.vue', 'DineInOrder.vue'];
const adminFiles = [
  'AdminVoucher.vue', 'AdminTable.vue', 'AdminProduct.vue', 
  'AdminStaff.vue', 'AdminPost.vue', 'AdminOrder.vue', 
  'AdminCategory.vue', 'AdminAnalytics.vue', 'AdminIngredient.vue'
];

function processFile(filename, isCustomer) {
  const filePath = path.join(viewsDir, filename);
  if (!fs.existsSync(filePath)) return;
  
  let content = fs.readFileSync(filePath, 'utf-8');
  
  // 1. Remove header
  // Note: Some files have multiple headers (like AdminIngredient has v-if and v-else)
  // We'll use regex to remove everything from <header to </header>
  content = content.replace(/<header[\s\S]*?<\/header>/g, '');
  
  // 2. Wrap template with Layout
  const layoutName = isCustomer ? 'CustomerLayout' : 'AdminLayout';
  content = content.replace(/<template>/, `<template>\n  <${layoutName}>`);
  
  // Find </template> but only the first one which is the closing tag for root (actually the last one in file)
  const lastTemplateIdx = content.lastIndexOf('</template>');
  if (lastTemplateIdx !== -1) {
    content = content.substring(0, lastTemplateIdx) + `  </${layoutName}>\n` + content.substring(lastTemplateIdx);
  }
  
  // 3. Add import to <script setup>
  const importStatement = `import ${layoutName} from '@/components/${layoutName}.vue';\n`;
  content = content.replace(/<script setup>/, `<script setup>\n${importStatement}`);
  
  fs.writeFileSync(filePath, content);
  console.log(`Processed ${filename}`);
}

customerFiles.forEach(f => processFile(f, true));
adminFiles.forEach(f => processFile(f, false));

console.log('Done');
