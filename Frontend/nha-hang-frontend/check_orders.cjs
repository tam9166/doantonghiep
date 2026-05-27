const axios = require('axios');

async function test() {
    try {
        const res = await axios.get('http://localhost:8080/api/admin/orders');
        console.log("SUCCESS:");
        res.data.slice(-5).forEach(o => {
            console.log(`ID: ${o.id}, Status: ${o.status}, isPaid: ${o.isPaid}, Address: ${o.address}`);
        });
    } catch (e) {
        console.error("FAILED WITHOUT TOKEN:", e.message);
    }
}
test();
