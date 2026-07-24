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

  const installButtons = [...document.querySelectorAll('[data-install-app]')];
  const installStatus = document.querySelector('[data-install-status]');
  const installDialog = document.querySelector('[data-install-dialog]');
  const installDialogTitle = installDialog?.querySelector('[data-install-dialog-title]');
  const installDialogCopy = installDialog?.querySelector('[data-install-dialog-copy]');
  const installDialogSteps = installDialog?.querySelector('[data-install-dialog-steps]');
  const nativeShell = Boolean(window.__TAURI_INTERNALS__ || window.__TAURI__);
  const userAgent = navigator.userAgent.toLowerCase();
  const platformHint = (navigator.userAgentData?.platform || navigator.platform || '').toLowerCase();
  const iosDevice = /iphone|ipad|ipod/i.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const platform = (() => {
    if (iosDevice) return 'ios';
    if (/android/.test(userAgent)) return 'android';
    if (/mac/.test(platformHint) || /macintosh/.test(userAgent)) return 'macos';
    if (/win/.test(platformHint) || /windows/.test(userAgent)) return 'windows';
    if (/linux/.test(platformHint) || /linux/.test(userAgent)) return 'linux';
    return 'other';
  })();
  const standalone = nativeShell ||
    window.matchMedia('(display-mode: standalone)').matches ||
    window.navigator.standalone === true;
  let installPrompt = null;

  const installGuides = {
    ios: {
      buttonLabel: 'Add to Home Screen',
      title: 'Install on iPhone or iPad',
      copy: 'Apple requires installation from Safari’s Share menu.',
      status: 'On iPhone or iPad, open this page in Safari, tap Share, then choose Add to Home Screen.',
      steps: [
        'Open this page in Safari.',
        'Tap the Share button in the Safari toolbar.',
        'Choose Add to Home Screen.',
        'Confirm the name, then tap Add.'
      ]
    },
    macos: {
      buttonLabel: 'Install on macOS',
      title: 'Install on macOS',
      copy: 'Use Safari 17 or newer, Chrome, or Edge to add GitHub Rock as an app.',
      status: 'On macOS, use Safari → File → Add to Dock, or use the Install option in Chrome or Edge.',
      steps: [
        'In Safari 17 or newer, choose File → Add to Dock.',
        'In Chrome or Edge, choose Install GitHub Rock from the address bar or browser menu.',
        'Confirm Add or Install, then open GitHub Rock from the Dock or Applications.'
      ]
    },
    windows: {
      buttonLabel: 'Install on Windows',
      title: 'Install on Windows',
      copy: 'Chrome and Microsoft Edge can install the web companion as a standalone app.',
      status: 'On Windows, open the browser menu and choose Apps or Install GitHub Rock.',
      steps: [
        'Open this page in Chrome or Microsoft Edge.',
        'Select the install icon in the address bar, or open the browser menu and choose Apps → Install this site as an app.',
        'Confirm Install, then open GitHub Rock from Start.'
      ]
    },
    linux: {
      buttonLabel: 'Install on Linux',
      title: 'Install on Linux',
      copy: 'Use Chrome, Chromium, Brave, or another Chromium-based browser.',
      status: 'On Linux, use a Chromium-based browser and choose Install page as app from its menu.',
      steps: [
        'Open this page in a Chromium-based browser.',
        'Open the browser menu and choose Install page as app or Create shortcut.',
        'Enable Open as window when offered, then confirm Install.'
      ]
    },
    android: {
      buttonLabel: 'Install web companion',
      title: 'Install on Android',
      copy: 'Install the lightweight web companion or download the full native APK.',
      status: 'On Android, use Chrome’s Install app option for the web companion, or download the native APK.',
      steps: [
        'Open this page in Chrome.',
        'Tap the browser menu and choose Install app or Add to Home screen.',
        'Confirm Install. For the full app, use the Download APK button instead.'
      ]
    },
    other: {
      buttonLabel: 'Installation instructions',
      title: 'Install the web companion',
      copy: 'Use a browser that supports installable web apps.',
      status: 'Open your browser menu and look for Install app, Add to Home Screen, or Create shortcut.',
      steps: [
        'Open this page in an installable-web-app browser.',
        'Open the browser menu and choose Install app, Add to Home Screen, or Create shortcut.',
        'Confirm the installation.'
      ]
    }
  };
  const installGuide = installGuides[platform];

  const setInstallButtons = (visible, currentPlatformLabel = null) => {
    installButtons.forEach((button) => {
      const requestedPlatform = button.dataset.installPlatform;
      const buttonGuide = installGuides[requestedPlatform] || installGuide;
      const targetsCurrentPlatform = !requestedPlatform || requestedPlatform === platform;
      button.hidden = !visible;
      button.textContent = currentPlatformLabel && targetsCurrentPlatform
        ? currentPlatformLabel
        : buttonGuide.buttonLabel;
    });
  };

  const showInstallGuide = (guide = installGuide) => {
    if (!installDialog || !installDialogSteps) return;
    if (installDialogTitle) installDialogTitle.textContent = guide.title;
    if (installDialogCopy) installDialogCopy.textContent = guide.copy;
    installDialogSteps.replaceChildren();
    guide.steps.forEach((step) => {
      const item = document.createElement('li');
      item.textContent = step;
      installDialogSteps.append(item);
    });

    if (typeof installDialog.showModal === 'function') {
      if (!installDialog.open) installDialog.showModal();
    } else {
      installDialog.setAttribute('open', '');
      installDialog.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  installDialog?.querySelectorAll('[data-install-dialog-close]').forEach((button) => {
    button.addEventListener('click', () => {
      if (typeof installDialog.close === 'function') installDialog.close();
      else installDialog.removeAttribute('open');
    });
  });

  installDialog?.addEventListener('click', (event) => {
    if (event.target === installDialog && typeof installDialog.close === 'function') {
      installDialog.close();
    }
  });

  if (standalone) {
    setInstallButtons(false);
    if (installStatus) {
      installStatus.textContent = nativeShell
        ? 'The GitHub Rock companion is installed on this device.'
        : 'The web companion is installed on this device.';
    }
  } else {
    setInstallButtons(true, installGuide.buttonLabel);
    if (installStatus) installStatus.textContent = installGuide.status;
  }

  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    installPrompt = event;
    setInstallButtons(true);
    if (installStatus) {
      installStatus.textContent = 'This browser can install the GitHub Rock web companion as a standalone app.';
    }
  });

  installButtons.forEach((button) => {
    button.addEventListener('click', async () => {
      const requestedPlatform = button.dataset.installPlatform;
      const buttonGuide = installGuides[requestedPlatform] || installGuide;
      const targetsCurrentPlatform = !requestedPlatform || requestedPlatform === platform;
      if (!installPrompt || !targetsCurrentPlatform) {
        showInstallGuide(buttonGuide);
        return;
      }

      installPrompt.prompt();
      const choice = await installPrompt.userChoice;
      installPrompt = null;
      if (choice.outcome === 'accepted') {
        setInstallButtons(false);
        if (installStatus) installStatus.textContent = 'Installation accepted. GitHub Rock is being added to this device.';
      } else {
        setInstallButtons(true);
        if (installStatus) installStatus.textContent = installGuide.status;
      }
    });
  });

  window.addEventListener('appinstalled', () => {
    setInstallButtons(false);
    if (installStatus) installStatus.textContent = 'The web companion is installed and ready to open.';
  });

  if (!nativeShell && 'serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js').catch(() => {
        if (installStatus) {
          installStatus.textContent = 'The web companion is available online, but offline installation could not be prepared in this browser.';
        }
      });
    });
  }

  if (nativeShell && window.__TAURI__?.opener?.openUrl) {
    document.addEventListener('click', (event) => {
      const link = event.target.closest?.('a[href]');
      if (!link) return;

      const destination = new URL(link.href, window.location.href);
      if (!['http:', 'https:'].includes(destination.protocol) ||
          destination.origin === window.location.origin) return;

      event.preventDefault();
      window.__TAURI__.opener.openUrl(destination.href);
    });
  }

  const latestRelease = document.querySelector('[data-latest-release]');
  const releaseName = latestRelease?.querySelector('[data-release-name]');
  const releaseDate = latestRelease?.querySelector('[data-release-date]');
  const releaseStatus = latestRelease?.querySelector('[data-release-status]');
  const releaseAssets = latestRelease?.querySelector('[data-release-assets]');

  const formatAssetBytes = (bytes) => {
    if (!Number.isFinite(bytes) || bytes <= 0) return 'Size unavailable';
    const units = ['B', 'KB', 'MB', 'GB'];
    const unitIndex = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    const value = bytes / (1024 ** unitIndex);
    return `${value >= 10 || unitIndex === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[unitIndex]}`;
  };

  const assetDescription = (name) => {
    const normalized = name.toLowerCase();
    if (normalized.endsWith('.apk')) return 'Android package';
    if (normalized.endsWith('.dmg') || normalized.endsWith('.pkg')) return 'macOS package';
    if (normalized.endsWith('.msi') ||
        normalized.endsWith('.msix') ||
        normalized.endsWith('.exe') ||
        (normalized.includes('windows') && normalized.endsWith('.zip'))) return 'Windows package';
    if (normalized.endsWith('.appimage') ||
        normalized.endsWith('.deb') ||
        normalized.endsWith('.rpm') ||
        normalized.endsWith('.pkg.tar.zst')) return 'Linux package';
    if (normalized.endsWith('.ipa')) return 'iOS package';
    if (normalized.includes('sha256')) return 'SHA-256 checksum';
    if (normalized.endsWith('.sig') || normalized.endsWith('.asc')) return 'Signature';
    return 'Release file';
  };

  if (latestRelease && releaseAssets) {
    fetch('https://api.github.com/repos/Sayanthrock-Developer/GitHub-Rock/releases?per_page=20', {
      headers: { Accept: 'application/vnd.github+json' }
    })
      .then((response) => {
        if (!response.ok) throw new Error(`GitHub returned ${response.status}`);
        return response.json();
      })
      .then((releases) => {
        const release = releases.find((candidate) => !candidate.draft);
        if (!release) throw new Error('No published release');

        if (releaseName) releaseName.textContent = release.name || release.tag_name;
        if (releaseDate) {
          const published = release.published_at
            ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(release.published_at))
            : 'Publication date unavailable';
          releaseDate.textContent = `Published ${published}`;
        }
        if (releaseStatus) releaseStatus.textContent = release.prerelease ? 'Pre-release' : 'Stable';

        releaseAssets.replaceChildren();
        const assets = Array.isArray(release.assets) ? release.assets : [];
        if (assets.length === 0) {
          const empty = document.createElement('div');
          empty.className = 'asset-row';
          const copy = document.createElement('div');
          const title = document.createElement('b');
          const detail = document.createElement('span');
          title.textContent = 'No uploaded files';
          detail.textContent = 'Source archives remain available on GitHub.';
          copy.append(title, detail);
          const state = document.createElement('strong');
          state.textContent = 'GitHub';
          const link = document.createElement('a');
          link.href = release.html_url;
          link.textContent = 'Open';
          empty.append(copy, state, link);
          releaseAssets.append(empty);
          return;
        }

        assets.forEach((asset) => {
          const row = document.createElement('div');
          row.className = 'asset-row';
          const copy = document.createElement('div');
          const title = document.createElement('b');
          const detail = document.createElement('span');
          title.textContent = asset.name;
          detail.textContent = assetDescription(asset.name);
          copy.append(title, detail);
          const size = document.createElement('strong');
          size.textContent = formatAssetBytes(asset.size);
          const link = document.createElement('a');
          link.href = asset.browser_download_url;
          link.textContent = 'Download';
          link.setAttribute('download', '');
          row.append(copy, size, link);
          releaseAssets.append(row);
        });
      })
      .catch(() => {
        if (releaseDate) releaseDate.textContent = 'Open GitHub Releases for the current published files.';
        if (releaseStatus) releaseStatus.textContent = 'GitHub';
      });
  }

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
