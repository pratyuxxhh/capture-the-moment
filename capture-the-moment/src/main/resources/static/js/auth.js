(function () {
  const tokenKey = "capture_auth_token";
  const originalFetch = window.fetch.bind(window);

  window.captureAuth = {
    tokenKey,
    getToken: () => window.localStorage.getItem(tokenKey),
    setToken: token => {
      if (token) {
        window.localStorage.setItem(tokenKey, token);
      }
    },
    clearToken: () => window.localStorage.removeItem(tokenKey)
  };

  window.authFetch = function (input, init = {}) {
    const headers = new Headers(init.headers || {});
    const token = window.captureAuth.getToken();
    const requestUrl = new URL(input instanceof Request ? input.url : input, window.location.href);
    if (token && requestUrl.origin === window.location.origin && !headers.has("Authorization")) {
      headers.set("Authorization", "Bearer " + token);
    }
    return originalFetch(input, { ...init, headers, credentials: "same-origin" })
      .then(response => {
        const isAuthRequest = requestUrl.pathname.startsWith("/auth/");
        if (response.status === 401 && !isAuthRequest && window.location.pathname !== "/signin") {
          window.captureAuth.clearToken();
          window.location.replace("/signin");
        }
        return response;
      });
  };

  window.fetch = window.authFetch;
})();
