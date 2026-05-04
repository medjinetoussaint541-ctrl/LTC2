/**
 * accueil.js — LTC App
 * Interactions de la page d'accueil
 */

'use strict';

let unreadMessageCount = Number.parseInt(document.body?.dataset.unreadMessages ?? '0', 10);
if (!Number.isFinite(unreadMessageCount) || unreadMessageCount < 0) {
    unreadMessageCount = 0;
}

let notificationPollingTimerId = null;

//Active l'état visuel de l'élément sélectionné dans la navigation basse
function initBottomNav() {
    const navItems = document.querySelectorAll('.bottom-nav .nav-item');

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            navItems.forEach(i => {
                i.classList.remove('active');
                i.removeAttribute('aria-current');
            });
            item.classList.add('active');
            item.setAttribute('aria-current', 'page');
        });
    });
}

//Met en évidence le lien de navigation desktop correspondant
//à la route actuellement affichée dans le navigateur.
function syncDesktopNav() {
    const currentPath = window.location.pathname;
    const desktopLinks = document.querySelectorAll('.desktop-nav-link');

    desktopLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (!href || href === '#') {
            return;
        }

        if ((href !== '/' && currentPath.startsWith(href)) || href === currentPath) {
            link.classList.add('active');
            link.setAttribute('aria-current', 'page');
        } else {
            link.classList.remove('active');
            link.removeAttribute('aria-current');
        }
    });
}

//Récupère les informations CSRF injectées par Spring Security dans la page
function getCsrfConfig() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    return {
        token,
        headerName
    };
}

//Anime la disparition d'une carte après traitement d'une demande.
function dismissCard(card, direction) {
    const translateX = direction === 'accept' ? '30px' : '-30px';

    card.style.transition = 'opacity 0.3s ease, transform 0.3s ease, max-height 0.35s ease 0.1s, padding 0.35s ease 0.1s, margin 0.35s ease 0.1s';
    card.style.opacity = '0';
    card.style.transform = `translateX(${translateX})`;

    setTimeout(() => {
        card.style.overflow = 'hidden';
        card.style.maxHeight = `${card.offsetHeight}px`;

        requestAnimationFrame(() => {
            card.style.maxHeight = '0';
            card.style.paddingTop = '0';
            card.style.paddingBottom = '0';
            card.style.marginBottom = '0';
        });

        setTimeout(() => {
            card.remove();
            refreshPendingUi();
        }, 380);
    }, 260);
}

/**
 * Recalcule les indicateurs visuels liés aux demandes en attente.
 *
 * Cette fonction garde cohérents :
 * - le nombre affiché dans les statistiques ;
 * - le badge de navigation ;
 * - les pastilles de notification ;
 * - l'état vide lorsque plus aucune demande n'existe.
 */

function refreshPendingUi() {
    const pendingCards = document.querySelectorAll('.pending-card');
    const emptyState = document.getElementById('pendingEmptyState');
    const badge = document.querySelector('.nav-badge');
    const statDemandes = document.querySelector('.stat-value.gold');

    const count = pendingCards.length;

    if (statDemandes) {
        statDemandes.textContent = String(count);
    }

    if (badge) {
        badge.textContent = String(count);
        badge.style.display = count > 0 ? 'flex' : 'none';
    }

    syncNotificationIndicators(count, unreadMessageCount);

    if (count === 0) {
        if (emptyState) {
            emptyState.hidden = false;
            emptyState.style.display = 'block';
        }
    }
}

//Active ou désactive les boutons d'action d'une carte.
function setButtonsDisabled(card, disabled) {
    if (!card) {
        return;
    }

    card.querySelectorAll('.btn-accept, .btn-decline').forEach(button => {
        button.disabled = disabled;
    });
}
/** Identifiant du timer utilisé pour masquer automatiquement le toast. */
let toastTimeoutId;
//Affiche un message toast temporaire.
function showToast(message, variant = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) {
        return;
    }

    toast.textContent = message;
    toast.classList.remove('is-success', 'is-error', 'is-visible');
    toast.classList.add(variant === 'error' ? 'is-error' : 'is-success');

    requestAnimationFrame(() => {
        toast.classList.add('is-visible');
    });

    window.clearTimeout(toastTimeoutId);
    toastTimeoutId = window.setTimeout(() => {
        toast.classList.remove('is-visible');
    }, 2800);
}

function setHiddenState(element, hidden) {
    if (!element) {
        return;
    }

    if (hidden) {
        element.setAttribute('hidden', 'hidden');
    } else {
        element.removeAttribute('hidden');
    }
}

function syncNotificationIndicators(pendingCount, unreadCount) {
    const hasNotifications = pendingCount > 0 || unreadCount > 0;

    document.querySelectorAll('[data-notification-dot]').forEach(dot => {
        setHiddenState(dot, !hasNotifications);
    });

    document.querySelectorAll('[data-chat-unread-dot]').forEach(dot => {
        setHiddenState(dot, unreadCount === 0);
    });
}

async function refreshChatNotifications() {
    try {
        const response = await fetch('/chat/notifications', {
            headers: {
                Accept: 'application/json'
            },
            credentials: 'same-origin'
        });

        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || payload.detail || 'Impossible de charger les notifications.');
        }

        const nextUnreadCount = Number.parseInt(payload.unreadMessages ?? 0, 10);
        const normalizedUnreadCount = Number.isFinite(nextUnreadCount) && nextUnreadCount > 0 ? nextUnreadCount : 0;
        const previousUnreadCount = unreadMessageCount;

        unreadMessageCount = normalizedUnreadCount;
        syncNotificationIndicators(document.querySelectorAll('.pending-card').length, unreadMessageCount);

        if (normalizedUnreadCount > previousUnreadCount) {
            const newMessages = normalizedUnreadCount - previousUnreadCount;
            showToast(
                newMessages === 1
                    ? 'Vous avez reçu 1 nouveau message.'
                    : `Vous avez reçu ${newMessages} nouveaux messages.`,
                'success'
            );
        }
    } catch (error) {
        console.error(error);
    }
}

function initChatNotificationPolling() {
    syncNotificationIndicators(document.querySelectorAll('.pending-card').length, unreadMessageCount);

    notificationPollingTimerId = window.setInterval(() => {
        if (document.hidden) {
            return;
        }
        refreshChatNotifications();
    }, 5000);

    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            refreshChatNotifications();
        }
    });
}

//Envoie au serveur la décision prise sur une demande reçue.
async function submitDecision(button, action) {
    const requestId = button.dataset.id;
    const firstName = button.dataset.label || 'cet utilisateur';
    const card = button.closest('.pending-card');
    const { token, headerName } = getCsrfConfig();

    if (!requestId || !card) {
        return;
    }

    setButtonsDisabled(card, true);

    try {
        const headers = {
            'Accept': 'application/json'
        };

        if (token && headerName) {
            headers[headerName] = token;
        }

        const response = await fetch(`/demandes/${requestId}/${action}`, {
            method: 'POST',
            headers,
            credentials: 'same-origin'
        });

        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(payload.message || payload.detail || 'Une erreur est survenue lors du traitement de la demande.');
        }

        dismissCard(card, action === 'accepter' ? 'accept' : 'decline');
        showToast(
            action === 'accepter'
                ? `Demande de ${firstName} acceptée.`
                : `Demande de ${firstName} refusée.`,
            'success'
        );
    } catch (error) {
        setButtonsDisabled(card, false);
        showToast(error.message || 'Impossible de traiter la demande.', 'error');
    }
}

function initDemandes() {
    document.addEventListener('click', event => {
        const acceptBtn = event.target.closest('.btn-accept');
        const declineBtn = event.target.closest('.btn-decline');

        if (acceptBtn) {
            event.preventDefault();
            submitDecision(acceptBtn, 'accepter');
        }

        if (declineBtn) {
            event.preventDefault();
            submitDecision(declineBtn, 'refuser');
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    initBottomNav();
    initDemandes();
    syncDesktopNav();
    refreshPendingUi();
    initChatNotificationPolling();
});
