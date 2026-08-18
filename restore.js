// restore.js - Security-hardened version
// P0-06: Removed hardcoded admin/123 credentials
// Usage: Set ADMIN_USERNAME and ADMIN_PASSWORD environment variables, or edit below
const fs = require('fs');

// P0-06: Read credentials from environment or prompt user
const adminUsername = process.env.ADMIN_USERNAME || 'admin';
const adminPassword = process.env.ADMIN_PASSWORD;

if (!adminPassword) {
  console.error('ERROR: ADMIN_PASSWORD environment variable is required.');
  console.error('Usage: ADMIN_PASSWORD=your_secure_password node restore.js');
  process.exit(1);
}

async function restore() {
  try {
    // P0-06: Use secure credentials from environment
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: adminUsername, password: adminPassword })
    });
    
    if (!loginRes.ok) {
      throw new Error(`Login failed for ${adminUsername}: ${loginRes.status}`);
    }
    const loginData = await loginRes.json();
    const token = loginData.token;
    
    const ingredients = JSON.parse(fs.readFileSync('temp_ingredients.json', 'utf8'));
    for (const ing of ingredients) {
      try {
        const res = await fetch('http://localhost:8080/api/admin/ingredients', {
          method: 'POST',
          headers: { 
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}` 
          },
          body: JSON.stringify(ing)
        });
        if (res.ok) {
          console.log(`Added ${ing.name}`);
        } else {
          console.error(`Failed to add ${ing.name}: ${res.status}`);
        }
      } catch(e) {
        console.error(`Error adding ${ing.name}: ${e.message}`);
      }
    }
    console.log("Done restoring ingredients!");
  } catch (err) {
    console.error('Error:', err.message);
  }
}

restore();
