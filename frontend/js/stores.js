async function loadStores() {
  const data = await getStores();
  const tbody = document.querySelector("#storeTable tbody");
  tbody.innerHTML = "";

  data.forEach(store => {
    const row = `
      <tr>
        <td>${store.id}</td>
        <td>${store.name}</td>
        <td>${store.address}</td>
        <td>${store.managerName}</td>
      </tr>`;
    tbody.innerHTML += row;
  });
}

window.onload = loadStores;
