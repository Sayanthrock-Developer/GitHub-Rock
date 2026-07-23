(() => {
  const root = document.documentElement;
  const storedTheme = localStorage.getItem('github-rock-theme');
  const systemLight = window.matchMedia('(prefers-color-scheme: light)').matches;
  root.dataset.theme = storedTheme || (systemLight ? 'light' : 'dark');

  const setThemeLabel = (button) => {
    const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
    button.setAttribute('aria-label', `Switch to ${nextTheme} theme`);
  };

  document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
    setThemeLabel(button);
    button.addEventListener('click', () => {
      root.dataset.theme = root.dataset.theme === 'dark' ? 'light' : 'dark';
      localStorage.setItem('github-rock-theme', root.dataset.theme);
      document.querySelectorAll('[data-theme-toggle]').forEach(setThemeLabel);
    });
  });

  const header = document.querySelector('[data-header]');
  const updateHeader = () => header?.classList.toggle('is-scrolled', window.scrollY > 12);
  updateHeader();
  window.addEventListener('scroll', updateHeader, { passive: true });

  const workspaceTabs = document.querySelectorAll('[data-workspace-tab]');
  const workspacePanels = document.querySelectorAll('[data-workspace-panel]');

  workspaceTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      const targetId = tab.dataset.workspaceTab;
      workspaceTabs.forEach((candidate) => {
        const active = candidate === tab;
        candidate.classList.toggle('is-active', active);
        candidate.setAttribute('aria-selected', String(active));
      });

      workspacePanels.forEach((panel) => {
        const active = panel.id === targetId;
        panel.hidden = !active;
        panel.classList.toggle('is-active', active);
      });
    });
  });

  const searchInput = document.querySelector('[data-repo-search]');
  const feedButtons = document.querySelectorAll('[data-feed-filter]');
  const repoCards = document.querySelectorAll('[data-repo-card]');
  const previewEmpty = document.querySelector('[data-preview-empty]');
  let activeFeed = 'all';

  const applyPreviewFilters = () => {
    const query = searchInput?.value.trim().toLowerCase() || '';
    let visibleCount = 0;

    repoCards.forEach((card) => {
      const name = card.dataset.name?.toLowerCase() || '';
      const tags = card.dataset.tags?.toLowerCase() || '';
      const matchesQuery = !query || `${name} ${tags}`.includes(query);
      const matchesFeed = activeFeed === 'all' || tags.includes(activeFeed);
      const visible = matchesQuery && matchesFeed;
      card.hidden = !visible;
      if (visible) visibleCount += 1;
    });

    if (previewEmpty) previewEmpty.hidden = visibleCount !== 0;
  };

  searchInput?.addEventListener('input', applyPreviewFilters);

  feedButtons.forEach((button) => {
    button.addEventListener('click', () => {
      activeFeed = button.dataset.feedFilter || 'all';
      feedButtons.forEach((candidate) => {
        const active = candidate === button;
        candidate.classList.toggle('is-active', active);
        candidate.setAttribute('aria-selected', String(active));
      });
      applyPreviewFilters();
    });
  });

  const revealItems = document.querySelectorAll('.reveal');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (!('IntersectionObserver' in window) || reducedMotion) {
    revealItems.forEach((item) => item.classList.add('is-visible'));
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  revealItems.forEach((item) => observer.observe(item));
})();
