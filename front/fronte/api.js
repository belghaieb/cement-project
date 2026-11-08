(function () {
  const API_BASE = 'http://localhost:8080';

  function getStoredRole() {
    return sessionStorage.getItem('role') || '';
  }

  async function request(path, options = {}) {
    const headers = options.headers || {};
    const isFormData = options.body instanceof FormData;
    const hasBody = options.body !== undefined && options.body !== null;
    const config = {
      ...options,
      credentials: 'include',
      headers: isFormData || !hasBody ? headers : { ...(headers || {}), 'Content-Type': 'application/json' },
    };

    const response = await fetch(API_BASE + path, config);
    const contentType = response.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');
    const payload = isJson ? await response.json().catch(() => null) : await response.text().catch(() => '');

    if (!response.ok) {
      const message = payload && payload.message ? payload.message : 'Une erreur est survenue.';
      throw new Error(message);
    }

    return payload;
  }

  async function login(username, password) {
    return request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
  }

  async function logout() {
    return request('/auth/logout', { method: 'POST' });
  }

  async function me() {
    return request('/auth/me');
  }

  async function getFactures(filters = {}) {
    const params = new URLSearchParams();
    // attach any non-empty filter keys as query params (backend can choose which to support)
    Object.entries(filters || {}).forEach(([k, v]) => {
      if (v !== undefined && v !== null && String(v).trim() !== '') {
        params.set(k, String(v).trim());
      }
    });
    const query = params.toString();
    return request(`/factures${query ? `?${query}` : ''}`);
  }

  async function getFacture(id) {
    return request(`/factures/${id}`);
  }

  async function uploadScan(file) {
    const formData = new FormData();
    formData.append('image', file);
    return request('/factures/scan', { method: 'POST', body: formData, headers: {} });
  }

  async function updateFacture(id, data) {
    return request(`/factures/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  function isOcrIncomplete(facture) {
    const requiredFields = ['numeroFacture', 'fournisseur', 'montantHT'];
    return requiredFields.some(field => {
      const value = facture[field];
      return value === null || value === undefined || value === '';
    });
  }

  function getImageScanUrl(id) {
    return `${API_BASE}/factures/${id}/image-scan`;
  }

  async function uploadReceptionPhoto(id, file) {
    const formData = new FormData();
    formData.append('photo', file);
    return request(`/factures/${id}/photos`, { method: 'POST', body: formData, headers: {} });
  }

  async function confirmerReceptionMarchandise(id) {
    return request(`/factures/${id}/confirmer-reception`, { method: 'POST' });
  }

  async function adminListUtilisateurs() {
    return request('/admin/utilisateurs');
  }

  async function adminCreateUtilisateur(payload) {
    return request('/admin/utilisateurs', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  async function adminDesactiverUtilisateur(id) {
    return request(`/admin/utilisateurs/${id}/desactiver`, { method: 'PUT' });
  }

  async function validateFacture(id) {
    return request(`/factures/${id}/valider`, { method: 'POST' });
  }

  async function rejectFacture(id) {
    return request(`/factures/${id}/rejeter`, { method: 'POST' });
  }

  async function deleteFacture(id) {
    return request(`/factures/${id}`, { method: 'DELETE' });
  }

  function setAuthContext(user) {
    if (user && user.role) {
      sessionStorage.setItem('role', user.role);
    }
    if (user && user.nomComplet) {
      sessionStorage.setItem('nomComplet', user.nomComplet);
    }
    if (user && user.username) {
      sessionStorage.setItem('username', user.username);
    }
  }

  function clearAuthContext() {
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('nomComplet');
    sessionStorage.removeItem('username');
  }

  window.preFactureApi = {
    login,
    logout,
    me,
    getFactures,
    getFacture,
    uploadScan,
    updateFacture,
    isOcrIncomplete,
    getImageScanUrl,
    uploadReceptionPhoto,
    confirmerReceptionMarchandise,
    validateFacture,
    rejectFacture,
    deleteFacture,
    adminListUtilisateurs,
    adminCreateUtilisateur,
    adminDesactiverUtilisateur,
    setAuthContext,
    clearAuthContext,
    getStoredRole,
  };
})();
