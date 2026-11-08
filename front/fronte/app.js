// Shared shell logic and session-aware UI helpers.
(function () {
  const ROLE_META = {
    admin: { code: 'ADMIN', label: 'Administrateur', cls: 'role-badge--admin' },
    responsable: { code: 'RESP.', label: 'Responsable Facture', cls: '' },
    responsable_facture: { code: 'RESP.', label: 'Responsable Facture', cls: '' },
    it: { code: 'IT', label: 'Consultation IT', cls: 'role-badge--it' },
  };
  const NAV = {
    admin: [
      { href: 'dashboard.html', label: 'Tableau de bord', icon: '▤' },
      { href: 'factures.html', label: 'Factures', icon: '▦' },
      { href: 'utilisateurs.html', label: 'Utilisateurs', icon: '◉' },
    ],
    responsable: [
      { href: 'dashboard.html', label: 'Tableau de bord', icon: '▤' },
      { href: 'factures.html', label: 'Factures', icon: '▦' },
    ],
    it: [
      { href: 'dashboard.html', label: 'Tableau de bord', icon: '▤' },
      { href: 'factures.html', label: 'Factures', icon: '▦' },
    ],
  };

  function normalizeRole(role) {
    const raw = (role || '').toLowerCase();
    if (raw === 'responsable_facture' || raw === 'responsable') return 'responsable';
    if (raw === 'admin') return 'admin';
    if (raw === 'it') return 'it';
    return 'responsable';
  }

  async function ensureSession() {
    if (!window.preFactureApi) return null;
    try {
      const user = await window.preFactureApi.me();
      window.preFactureApi.setAuthContext(user);
      return user;
    } catch (error) {
      window.preFactureApi.clearAuthContext();
      if (!location.pathname.endsWith('login.html')) {
        const next = encodeURIComponent(location.pathname + location.search + location.hash);
        window.location.href = `login.html?next=${next}`;
      }
      return null;
    }
  }

  function renderShell(userRole) {
    const role = normalizeRole(userRole || sessionStorage.getItem('role') || 'responsable');
    const meta = ROLE_META[role] || ROLE_META.responsable;
    const nav = NAV[role] || NAV.responsable;
    const currentPath = location.pathname;
    const sidebar = document.querySelector('[data-shell="sidebar"]');
    if (sidebar) {
      sidebar.innerHTML = `
        <div class="sidebar__brand">
          <div class="sidebar__brand-name">SCG · Gabès</div>
          <div class="sidebar__brand-app">Préfacture</div>
        </div>
        <div class="sidebar__section">Navigation</div>
        <nav class="sidebar__nav">
          ${nav.map(n => `
            <a class="nav-item ${currentPath.endsWith(n.href) || (currentPath.endsWith('facture.html') && n.href === 'factures.html') ? 'is-active' : ''}" href="${n.href}">
              <span class="nav-item__icon">${n.icon}</span>
              <span>${n.label}</span>
            </a>
          `).join('')}
        </nav>
        <div class="sidebar__foot">
          v1.0.0 · <a href="login.html" style="color: var(--ciment-light);">Déconnexion</a>
        </div>
      `;
    }
    const topbarRight = document.querySelector('[data-shell="topbar-right"]');
    if (topbarRight) {
      topbarRight.innerHTML = `
        <span class="role-badge ${meta.cls}">
          <span class="role-badge__dot"></span>${meta.code}
        </span>
        <span class="user-chip__role">${meta.label}</span>
        <button type="button" class="btn btn--ghost btn--sm" id="btn-logout">Déconnexion</button>
      `;
      document.getElementById('btn-logout')?.addEventListener('click', async () => {
        try {
          await window.preFactureApi.logout();
        } catch (error) {
          // Ignore logout error and proceed to login.
        }
        window.preFactureApi.clearAuthContext();
        window.location.href = 'login.html';
      });
    }
    document.querySelectorAll('[data-role-link]').forEach(a => {
      const base = a.getAttribute('href').split('?')[0];
      a.setAttribute('href', base);
    });
    document.body.dataset.role = role;
  }

  window.verifierSession = ensureSession;
  window.libelleStatut = function (statut) {
    const map = {
      A_VERIFIER: 'À vérifier',
      EN_COURS: 'En cours',
      VALIDEE: 'Validée',
      REJETEE: 'Rejetée',
    };
    return map[statut] || statut;
  };
  window.classeStatut = function (statut) {
    const map = {
      A_VERIFIER: 'review',
      EN_COURS: 'pending',
      VALIDEE: 'valid',
      REJETEE: 'rejected',
    };
    return map[statut] || '';
  };
  window.memoriserAlertesBonCommande = function (alertes) {
    if (Array.isArray(alertes) && alertes.length > 0) {
      sessionStorage.setItem('scanAlertesBonCommande', JSON.stringify(alertes));
      return;
    }
    sessionStorage.removeItem('scanAlertesBonCommande');
  };

  function showToast(message, type = 'error') {
    const existing = document.querySelector('.toast-notification');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast-notification toast--${type}`;
    toast.textContent = message;
    toast.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 12px 20px;
      background: ${type === 'error' ? '#fee2e2' : '#dcfce7'};
      border-left: 4px solid ${type === 'error' ? '#ef4444' : '#22c55e'};
      color: ${type === 'error' ? '#991b1b' : '#166534'};
      border-radius: 4px;
      font-family: var(--font-body);
      font-size: 14px;
      z-index: 10000;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      animation: slideIn 0.3s ease-out;
    `;

    const style = document.createElement('style');
    style.textContent = `
      @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
      }
    `;
    document.head.appendChild(style);
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease-out';
      setTimeout(() => toast.remove(), 300);
    }, 5000);
  }

  window.showToast = showToast;

  document.addEventListener('DOMContentLoaded', async () => {
    if (window.preFactureApi) {
      const user = await ensureSession();
      renderShell(user?.role);
    } else {
      renderShell();
    }
  });
})();