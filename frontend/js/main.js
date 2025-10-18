function login() {
  const user = document.getElementById('username').value;
  const pass = document.getElementById('password').value;

  if (user === "admin" && pass === "123") {
    alert("Đăng nhập thành công!");
    window.location.href = "dashboard.html";
  } else {
    alert("Sai thông tin đăng nhập!");
  }
}
