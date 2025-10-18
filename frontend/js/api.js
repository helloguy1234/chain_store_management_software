const API_BASE = "http://localhost:3000/api";

async function getStores() {
  const res = await fetch(`${API_BASE}/stores`);
  return await res.json();
}
async function getProducts() {
  const res = await fetch(`${API_BASE}/products`);
  return await res.json();
}
async function getEmployees() {
  const res = await fetch(`${API_BASE}/employees`);
  return await res.json();
}

