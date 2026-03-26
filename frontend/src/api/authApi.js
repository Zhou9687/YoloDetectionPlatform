import { httpRequest } from "./http";

export function registerUser(username, password) {
  return httpRequest("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
}

export function loginUser(username, password) {
  return httpRequest("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
}

async function postWithFallback(primaryPath, fallbackPath, body) {
  try {
    return await httpRequest(primaryPath, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
  } catch (error) {
    if (!(error instanceof Error) || !String(error.message).includes("404")) {
      throw error;
    }
    return httpRequest(fallbackPath, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
  }
}

export function updateUsername(currentUsername, newUsername) {
  const payload = {
    username: currentUsername,
    currentUsername,
    newUsername
  };
  return postWithFallback("/api/auth/profile/username", "/api/auth/update-username", payload);
}

export function updatePassword(username, oldPassword, newPassword) {
  const payload = {
    username,
    oldPassword,
    newPassword
  };
  return postWithFallback("/api/auth/profile/password", "/api/auth/update-password", payload);
}
