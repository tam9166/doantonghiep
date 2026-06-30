const fs = require('fs');

async function restore() {
  try {
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: '123' })
    });
    
    if (!loginRes.ok) {
        throw new Error('Login failed');
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
            console.error(`Failed to add ${ing.name}`);
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
