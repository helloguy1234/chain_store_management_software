async function loadProducts() {
  const data = await getProducts();
  const tbody = document.querySelector("#productTable tbody");
  tbody.innerHTML = "";

  data.forEach(product => {
    const row = `
      <tr>
        <td>${product.id}</td>
        <td>${product.name}</td>
        <td>${product.price}</td>
        <td>${product.category}</td>
      </tr>`;
    tbody.innerHTML += row;
  });
}

window.onload = loadProducts;
