const TOKEN_KEY = "yolo_auth_token";
const USERNAME_KEY = "yolo_auth_username";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setAuth(token, username) {
  localStorage.setItem(TOKEN_KEY, String(token || ""));
  localStorage.setItem(USERNAME_KEY, String(username || ""));
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
}

export function isLoggedIn() {
  return getToken().trim().length > 0;
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY) || "";
}

export function setUsername(username) {
  localStorage.setItem(USERNAME_KEY, String(username || ""));
}

export function getDisplayInitial(username) {
  const text = String(username || "").trim();
  if (!text) return "?";
  return Array.from(text)[0].toUpperCase();
}
