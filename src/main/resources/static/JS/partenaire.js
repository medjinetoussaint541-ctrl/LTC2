(() => {
    'use strict';

    const SEARCH_DELAY_MS = 380;
    const MIN_QUERY_LEN = 2;
    const ONLINE_REFRESH_MS = 30000;

    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearBtn');
    const resultsSection = document.getElementById('resultsSection');
    const onlineUsersPanel = document.getElementById('onlineUsersPanel');
    const toast = document.getElementById('toast');

    const confirmModal = document.getElementById('confirmModal');
    const confirmBackdrop = document.getElementById('confirmBackdrop');
    const confirmMessage = document.getElementById('confirmMessage');
    const confirmCancelBtn = document.getElementById('confirmCancelBtn');
    const confirmProceedBtn = document.getElementById('confirmProceedBtn');
    const confirmCrushBtn = document.getElementById('confirmCrushBtn');

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content ?? '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content ?? 'X-CSRF-TOKEN';

    let debounceTimer = null;
    let lastQuery = '';
    let toastTimer = null;
    let pendingModalActions = null;
    let onlineRefreshTimer = null;

    const sentMap = new Map();
    const crushMap = new Map();

    searchInput?.addEventListener('input', onInput);
    clearBtn?.addEventListener('click', clearSearch);
    confirmCancelBtn?.addEventListener('click', closeConfirmModal);
    confirmBackdrop?.addEventListener('click', closeConfirmModal);
    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('visibilitychange', onVisibilityChange);
    confirmProceedBtn?.addEventListener('click', () => executeModalAction('confirm'));
    confirmCrushBtn?.addEventListener('click', () => executeModalAction('crush'));

    function onInput() {
        const query = searchInput?.value.trim() ?? '';
        clearTimeout(debounceTimer);

        if (query.length < MIN_QUERY_LEN) {
            showHint();
            lastQuery = '';
            return;
        }

        if (query === lastQuery) {
            return;
        }

        showSkeleton();
        debounceTimer = window.setTimeout(() => doSearch(query), SEARCH_DELAY_MS);
    }

    function clearSearch() {
        if (!searchInput) {
            return;
        }

        searchInput.value = '';
        searchInput.focus();
        showHint();
        lastQuery = '';
    }

    async function doSearch(query) {
        lastQuery = query;

        try {
            const resp = await fetch(`/partenaire/rechercher?q=${encodeURIComponent(query)}`, {
                headers: { Accept: 'application/json' }
            });

            if (!resp.ok) {
                throw new Error('Erreur réseau');
            }

            const results = await resp.json();
            renderResults(results, query);
        } catch (err) {
            showError('Impossible de charger les résultats. Réessayez.');
        }
    }

    async function loadOnlineUsers(options = {}) {
        if (!onlineUsersPanel) {
            return;
        }

        const silent = options.silent === true;
        if (!silent) {
            showOnlineSkeleton();
        }

        onlineUsersPanel.setAttribute('aria-busy', 'true');

        try {
            const resp = await fetch('/partenaire/en-ligne', {
                headers: { Accept: 'application/json' }
            });

            if (!resp.ok) {
                throw new Error('Erreur réseau');
            }

            const users = await resp.json();
            renderOnlineUsers(Array.isArray(users) ? users : []);
        } catch (err) {
            if (!silent) {
                renderOnlineError();
            }
        } finally {
            onlineUsersPanel.setAttribute('aria-busy', 'false');
        }
    }

    async function sendRequest(user, btn, options = {}) {
        const confirmer = options.confirmer === true;

        if (!confirmer && user.aUneRelationActive) {
            openConfirmModal({
                message: "Cette personne est déjà dans une relation active. Vous pouvez envoyer la demande quand même, ou l'ajouter directement comme crush.",
                onConfirm: () => sendRequest(user, btn, { confirmer: true }),
                onCrush: () => addCrush(user),
                showCrushButton: true
            });
            return;
        }

        btn.disabled = true;
        const originalContent = btn.innerHTML;
        btn.innerHTML = '<span class="material-symbols-outlined" style="animation:spin 0.8s linear infinite;display:inline-block">progress_activity</span>';

        try {
            const resp = await fetch(`/partenaire/demande/${user.userId}?confirmer=${confirmer}`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken,
                    Accept: 'application/json'
                }
            });

            const data = await safeReadJson(resp);

            if (resp.ok && data.success) {
                syncActionState(user.userId, 'sent');
                showToast(data.message ?? 'Demande envoyée !', 'success');
                return;
            }

            btn.disabled = false;
            btn.innerHTML = originalContent;

            if (data.requiresConfirmation) {
                openConfirmModal({
                    message: data.message ?? 'Cet utilisateur est déjà dans une relation active. Souhaitez-vous continuer ?',
                    onConfirm: () => sendRequest(user, btn, { confirmer: true }),
                    onCrush: data.canAddCrush ? () => addCrush(user) : null,
                    showCrushButton: data.canAddCrush === true
                });
                return;
            }

            if (data.code === 'ALREADY_IN_RELATION_WITH_YOU') {
                syncActionState(user.userId, 'relation');
            } else if (data.code === 'PENDING_REQUEST_ALREADY_EXISTS') {
                syncActionState(user.userId, 'sent');
            }

            showToast(data.message ?? 'Une erreur est survenue.', 'error');
        } catch (err) {
            btn.disabled = false;
            btn.innerHTML = originalContent;
            showToast('Erreur réseau. Réessayez.', 'error');
        }
    }

    async function addCrush(user) {
        try {
            const resp = await fetch(`/partenaire/crush/${user.userId}`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken,
                    Accept: 'application/json'
                }
            });

            const data = await safeReadJson(resp);

            if (!resp.ok || !data.success) {
                throw new Error(data.message || "Impossible d'ajouter ce crush.");
            }

            crushMap.set(user.userId, 'Crush ajouté');
            markUserAsCrush(user.userId);
            closeConfirmModal();
            showToast(data.message ?? 'Crush ajouté.', 'success');
        } catch (error) {
            showToast(error.message || "Impossible d'ajouter ce crush.", 'error');
        }
    }

    function renderResults(results, query) {
        if (!results || results.length === 0) {
            renderEmpty(query);
            return;
        }

        const list = document.createElement('div');
        list.className = 'result-list';

        results.forEach((user, idx) => {
            list.appendChild(buildCard(user, idx));
        });

        const hd = document.createElement('div');
        hd.className = 'results-hd';
        hd.textContent = `${results.length} résultat${results.length > 1 ? 's' : ''}`;

        resultsSection.innerHTML = '';
        resultsSection.appendChild(hd);
        resultsSection.appendChild(list);
    }

    function renderOnlineUsers(users) {
        if (!onlineUsersPanel) {
            return;
        }

        onlineUsersPanel.classList.toggle('is-empty', users.length === 0);

        if (users.length === 0) {
            onlineUsersPanel.innerHTML = `
                <div class="empty-state">
                    <span class="material-symbols-outlined" aria-hidden="true">wifi</span>
                    <strong>Aucun utilisateur en ligne</strong>
                    <span>Les profils connectés apparaîtront ici dès qu'ils seront disponibles.</span>
                </div>`;
            return;
        }

        const list = document.createElement('div');
        list.className = 'result-list';

        users.forEach((user, idx) => {
            list.appendChild(buildCard(user, idx));
        });

        onlineUsersPanel.innerHTML = '';
        onlineUsersPanel.appendChild(list);
    }

    function buildCard(user, idx) {
        const card = document.createElement('div');
        card.className = 'result-card';
        card.style.animationDelay = `${idx * 0.05}s`;
        card.dataset.userId = String(user.userId);

        const avatar = document.createElement('div');
        avatar.className = 'result-av';
        avatar.setAttribute('aria-hidden', 'true');

        if (user.photoUrl) {
            const img = document.createElement('img');
            img.src = user.photoUrl;
            img.alt = '';
            avatar.appendChild(img);
        } else {
            const initials = document.createElement('span');
            initials.className = 'avatar-initials';
            initials.textContent = user.initials ?? 'U';
            avatar.appendChild(initials);
        }

        const info = document.createElement('div');
        info.className = 'result-info';

        const name = document.createElement('div');
        name.className = 'result-name';
        name.textContent = user.fullName;

        const email = document.createElement('div');
        email.className = 'result-email';
        email.textContent = user.email;

        info.appendChild(name);
        info.appendChild(email);

        const meta = document.createElement('div');
        meta.className = 'result-meta';

        if (user.aUneRelationActive) {
            meta.appendChild(buildBadge('warning', 'warning', 'Déjà en relation'));
        }

        if (isUserCrush(user)) {
            const crushLabel = getCrushLabel(user);
            const crushType = crushLabel === 'Ex-crush' ? 'ex-crush' : 'crush';
            const crushIcon = crushLabel === 'Ex-crush' ? 'heart_broken' : 'visibility';
            meta.appendChild(buildBadge(crushType, crushIcon, crushLabel));
        }

        if (meta.childElementCount > 0) {
            info.appendChild(meta);
        }

        const button = buildActionButton(user);

        card.appendChild(avatar);
        card.appendChild(info);
        card.appendChild(button);

        return card;
    }

    function buildBadge(type, icon, label) {
        const badge = document.createElement('span');
        badge.className = `result-badge result-badge-${type}`;
        badge.dataset.badgeType = type;
        badge.innerHTML = `<span class="material-symbols-outlined" aria-hidden="true">${icon}</span> ${label}`;
        return badge;
    }

    function isUserCrush(user) {
        return Boolean(crushMap.get(user.userId)) || user.crushAjoute === true;
    }

    function getCrushLabel(user) {
        return crushMap.get(user.userId) || user.crushStatusLabel || 'Crush ajouté';
    }

    function markCardAsCrush(card) {
        if (!card) {
            return;
        }

        const info = card.querySelector('.result-info');
        if (!info) {
            return;
        }

        let meta = info.querySelector('.result-meta');
        if (!meta) {
            meta = document.createElement('div');
            meta.className = 'result-meta';
            info.appendChild(meta);
        }

        const existingCrushBadge = meta.querySelector('[data-badge-type="crush"], [data-badge-type="ex-crush"]');
        if (existingCrushBadge) {
            existingCrushBadge.remove();
        }

        meta.appendChild(buildBadge('crush', 'visibility', 'Crush ajouté'));
    }

    function markUserAsCrush(userId) {
        document.querySelectorAll(`.result-card[data-user-id="${String(userId)}"]`)
            .forEach(markCardAsCrush);
    }

    function buildActionButton(user) {
        const btn = document.createElement('button');
        btn.className = 'result-action-btn';
        btn.dataset.userId = String(user.userId);
        btn.setAttribute('aria-label', `Envoyer une demande à ${user.firstName ?? user.fullName ?? 'cet utilisateur'}`);

        const localState = sentMap.get(user.userId);
        const state = localState ?? (user.dejaEnRelation ? 'relation' : user.dejaEnDemande ? 'sent' : 'idle');

        if (state === 'relation') {
            setButtonInRelation(btn);
        } else if (state === 'sent') {
            setButtonSent(btn);
        } else {
            btn.classList.add('btn-send');
            btn.innerHTML = '<span class="material-symbols-outlined">send</span> Envoyer';
            btn.addEventListener('click', () => sendRequest(user, btn));
        }

        return btn;
    }

    function syncActionState(userId, state) {
        sentMap.set(userId, state);

        document.querySelectorAll(`.result-action-btn[data-user-id="${String(userId)}"]`)
            .forEach((btn) => {
                if (state === 'relation') {
                    setButtonInRelation(btn);
                    return;
                }

                if (state === 'sent') {
                    setButtonSent(btn);
                }
            });
    }

    function setButtonSent(btn) {
        btn.disabled = true;
        btn.className = 'result-action-btn btn-sent';
        btn.innerHTML = '<span class="material-symbols-outlined">check</span> Envoyée';
    }

    function setButtonInRelation(btn) {
        btn.disabled = true;
        btn.className = 'result-action-btn btn-relation';
        btn.innerHTML = '<span class="material-symbols-outlined">favorite</span> En relation';
    }

    function renderEmpty(query) {
        resultsSection.innerHTML = `
            <div class="empty-state">
                <span class="material-symbols-outlined" aria-hidden="true">person_search</span>
                <strong>Aucun résultat</strong>
                <span>Aucun utilisateur ne correspond à « ${escHtml(query)} ».</span>
            </div>`;
    }

    function showHint() {
        resultsSection.innerHTML = `
            <div class="hint-state">
                <span class="material-symbols-outlined" aria-hidden="true">manage_search</span>
                <strong>Trouvez votre partenaire</strong>
                <span>Tapez au moins 2 caractères pour lancer la recherche.</span>
            </div>`;
    }

    function showSkeleton() {
        resultsSection.innerHTML = `
            <div class="skeleton-list" aria-label="Chargement…" role="status">
                ${Array.from({ length: 3 }, () => `
                <div class="skeleton-card" aria-hidden="true">
                    <div class="skeleton-av"></div>
                    <div class="skeleton-lines">
                        <div class="skeleton-line short"></div>
                        <div class="skeleton-line shorter"></div>
                    </div>
                </div>`).join('')}
            </div>`;
    }

    function showOnlineSkeleton() {
        if (!onlineUsersPanel) {
            return;
        }

        onlineUsersPanel.classList.remove('is-empty');
        onlineUsersPanel.innerHTML = `
            <div class="skeleton-list" aria-label="Chargement des utilisateurs en ligne" role="status">
                ${Array.from({ length: 4 }, () => `
                <div class="skeleton-card" aria-hidden="true">
                    <div class="skeleton-av"></div>
                    <div class="skeleton-lines">
                        <div class="skeleton-line short"></div>
                        <div class="skeleton-line shorter"></div>
                    </div>
                </div>`).join('')}
            </div>`;
    }

    function showError(msg) {
        resultsSection.innerHTML = `
            <div class="empty-state">
                <span class="material-symbols-outlined" aria-hidden="true">wifi_off</span>
                <strong>Oups…</strong>
                <span>${escHtml(msg)}</span>
            </div>`;
    }

    function renderOnlineError() {
        if (!onlineUsersPanel) {
            return;
        }

        onlineUsersPanel.classList.add('is-empty');
        onlineUsersPanel.innerHTML = `
            <div class="empty-state">
                <span class="material-symbols-outlined" aria-hidden="true">wifi_off</span>
                <strong>Impossible de charger la liste</strong>
                <span>Réessayez dans quelques instants pour voir les profils en ligne.</span>
            </div>`;
    }

    function showToast(msg, type = '') {
        if (!toast) {
            return;
        }

        clearTimeout(toastTimer);
        toast.textContent = msg;
        toast.className = `toast show${type ? ` ${type}` : ''}`;

        toastTimer = window.setTimeout(() => {
            toast.classList.remove('show');
        }, 3500);
    }

    function openConfirmModal({ message, onConfirm, onCrush, showCrushButton = false }) {
        pendingModalActions = {
            onConfirm: typeof onConfirm === 'function' ? onConfirm : null,
            onCrush: typeof onCrush === 'function' ? onCrush : null
        };

        if (confirmMessage) {
            confirmMessage.textContent = message;
        }

        if (confirmCrushBtn) {
            confirmCrushBtn.hidden = !showCrushButton;
        }

        if (confirmModal) {
            confirmModal.hidden = false;
            confirmModal.setAttribute('aria-hidden', 'false');
        }

        document.body.style.overflow = 'hidden';
        window.setTimeout(() => confirmProceedBtn?.focus(), 0);
    }

    function closeConfirmModal() {
        pendingModalActions = null;

        if (confirmModal) {
            confirmModal.hidden = true;
            confirmModal.setAttribute('aria-hidden', 'true');
        }

        document.body.style.overflow = '';
    }

    function executeModalAction(type) {
        const action = type === 'crush'
            ? pendingModalActions?.onCrush
            : pendingModalActions?.onConfirm;

        closeConfirmModal();

        if (action) {
            action();
        }
    }

    function onKeyDown(event) {
        if (event.key === 'Escape' && confirmModal && !confirmModal.hidden) {
            closeConfirmModal();
            return;
        }

        if (event.key === 'Enter' && document.activeElement === confirmProceedBtn && pendingModalActions?.onConfirm) {
            event.preventDefault();
            executeModalAction('confirm');
        }
    }

    function onVisibilityChange() {
        if (!document.hidden) {
            loadOnlineUsers({ silent: true });
        }
    }

    function startOnlineRefreshLoop() {
        if (onlineRefreshTimer !== null || !onlineUsersPanel) {
            return;
        }

        onlineRefreshTimer = window.setInterval(() => {
            if (!document.hidden) {
                loadOnlineUsers({ silent: true });
            }
        }, ONLINE_REFRESH_MS);
    }

    async function safeReadJson(response) {
        const contentType = response.headers.get('content-type') ?? '';

        if (!contentType.includes('application/json')) {
            return { message: response.ok ? '' : 'Une erreur est survenue.' };
        }

        try {
            return await response.json();
        } catch (err) {
            return { message: response.ok ? '' : 'Une erreur est survenue.' };
        }
    }

    function escHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    showHint();
    loadOnlineUsers();
    startOnlineRefreshLoop();
})();
