/**
 * Gère les interactions de la page "Mes liens".
 *
 * Responsabilités principales :
 * - accepter ou refuser des demandes reçues ;
 * - annuler une demande envoyée ;
 * - faire évoluer le statut d'un crush ;
 * - rompre une relation active ;
 * - refléter immédiatement les changements dans l'interface.
 */

(() => {
    'use strict';
     /** Élément toast utilisé pour les retours utilisateurs. */
    const toast = document.getElementById('toast');
     /** Métadonnées CSRF nécessaires aux requêtes POST vers Spring Security. */
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content ?? '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content ?? 'X-CSRF-TOKEN';
     /** Référence du timer de fermeture automatique du toast. */
    let toastTimer = null;
    
    
//    Délégation centralisée des clics sur les différents boutons d'action.
//    Cette approche évite de multiplier les listeners sur chaque carte.
    document.addEventListener('click', event => {
        const acceptBtn = event.target.closest('.btn-accept');
        const declineBtn = event.target.closest('.btn-decline');
        const cancelSentBtn = event.target.closest('.btn-cancel-sent');
        const markExCrushBtn = event.target.closest('.btn-mark-ex-crush');
        const breakRelationBtn = event.target.closest('.btn-break-relation');

        if (acceptBtn) {
            event.preventDefault();
            submitDecision(acceptBtn, 'accepter');
            return;
        }

        if (declineBtn) {
            event.preventDefault();
            submitDecision(declineBtn, 'refuser');
            return;
        }

        if (cancelSentBtn) {
            event.preventDefault();
            cancelSentRequest(cancelSentBtn);
            return;
        }

        if (markExCrushBtn) {
            event.preventDefault();
            markCrushAsExCrush(markExCrushBtn);
            return;
        }

        if (breakRelationBtn) {
            event.preventDefault();
            breakRelation(breakRelationBtn);
        }
    });
    
//    Envoie au serveur la décision sur une demande reçue.
    async function submitDecision(button, action) {
        const requestId = button.dataset.id;
        const firstName = button.dataset.label || 'cet utilisateur';
        const card = button.closest('.request-card');
        if (!requestId || !card) {
            return;
        }

        setButtonsDisabled(card, true);

        try {
            const response = await fetch(`/demandes/${requestId}/${action}`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken,
                    Accept: 'application/json'
                },
                credentials: 'same-origin'
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || payload.detail || 'Une erreur est survenue lors du traitement de la demande.');
            }

            updateCardStatus(card, action === 'accepter' ? 'Acceptée' : 'Refusée');
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
    
    //Annule une demande précédemment envoyée par l'utilisateur courant.
    async function cancelSentRequest(button) {
        const requestId = button.dataset.id;
        const firstName = button.dataset.label || 'cet utilisateur';
        const card = button.closest('.request-card');
        if (!requestId || !card) {
            return;
        }

        setButtonsDisabled(card, true);

        try {
            const response = await fetch(`/liens/demandes/${requestId}/annuler`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken,
                    Accept: 'application/json'
                },
                credentials: 'same-origin'
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || payload.detail || "Impossible d'annuler la demande.");
            }

            updateCardStatus(card, payload.statusLabel || 'Annulée');
            showToast(`Demande envoyée à ${firstName} annulée.`, 'success');
        } catch (error) {
            setButtonsDisabled(card, false);
            showToast(error.message || "Impossible d'annuler la demande.", 'error');
        }
    }
    
    //Bascule un crush vers le statut ex-crush après confirmation utilisateur.
    async function markCrushAsExCrush(button) {
        const crushId = button.dataset.id;
        const firstName = button.dataset.label || 'cette personne';
        const card = button.closest('.crush-card');
        if (!crushId || !card) {
            return;
        }

        const confirmed = window.confirm(`Voulez-vous vraiment passer ${firstName} en ex-crush ?`);
        if (!confirmed) {
            return;
        }

        setButtonsDisabled(card, true);

        try {
            const response = await fetch(`/liens/crushs/${crushId}/ex-crush`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken,
                    Accept: 'application/json'
                },
                credentials: 'same-origin'
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || payload.detail || "Impossible de passer ce crush en ex-crush.");
            }

            updateCrushCardStatus(card, payload.statusLabel || 'Ex-crush');
            showToast(payload.message || `${firstName} est maintenant un ex-crush.`, 'success');
        } catch (error) {
            setButtonsDisabled(card, false);
            showToast(error.message || "Impossible de passer ce crush en ex-crush.", 'error');
        }
    }
    
    //Rompt une relation active après confirmation explicite.
    async function breakRelation(button) {
        const relationId = button.dataset.id;
        const firstName = button.dataset.label || 'cette personne';
        if (!relationId) {
            return;
        }

        const confirmed = window.confirm(`Voulez-vous vraiment rompre votre relation avec ${firstName} ?`);
        if (!confirmed) {
            return;
        }

        button.disabled = true;

        try {
            const response = await fetch(`/liens/relations/${relationId}/rompre`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken,
                    Accept: 'application/json'
                },
                credentials: 'same-origin'
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || payload.detail || 'Impossible de rompre la relation.');
            }

            showToast(payload.message || 'Relation rompue avec succès.', 'success');
            
            // Rechargement léger pour refléter l'état global de la page après rupture.
            window.setTimeout(() => {
                window.location.reload();
            }, 900);
        } catch (error) {
            button.disabled = false;
            showToast(error.message || 'Impossible de rompre la relation.', 'error');
        }
    }
    
    
    //Met à jour le badge et retire les actions d'une carte de demande.
    function updateCardStatus(card, label) {
        const badge = card.querySelector('.status-badge');
        const actions = card.querySelector('.request-actions');

        if (badge) {
            badge.textContent = label;
            badge.classList.remove('is-pending', 'is-accepted', 'is-declined', 'is-cancelled');
            if (label === 'Acceptée') {
                badge.classList.add('is-accepted');
            } else if (label === 'Annulée') {
                badge.classList.add('is-cancelled');
            } else {
                badge.classList.add('is-declined');
            }
        }

        if (actions) {
            actions.remove();
        }
    }
    
    //Met à jour l'affichage d'une carte de crush après changement de statut.
    function updateCrushCardStatus(card, label) {
        const badge = card.querySelector('.status-badge');
        const actions = card.querySelector('.request-actions');
        const meta = card.querySelector('.item-meta');

        if (badge) {
            badge.textContent = label;
            badge.classList.remove('is-crush', 'is-ex-crush');
            badge.classList.add(label === 'Ex-crush' ? 'is-ex-crush' : 'is-crush');
        }

        if (meta && label === 'Ex-crush') {
            meta.textContent = "Passé en ex-crush à l'instant";
        }

        if (actions) {
            actions.remove();
        }
    }
    
    //Active ou désactive tous les boutons d'action d'une carte.
    function setButtonsDisabled(card, disabled) {
        card.querySelectorAll('.request-btn').forEach(btn => {
            btn.disabled = disabled;
        });
    }
    
    //Affiche un toast temporaire avec le message fourni.
    function showToast(message, variant = '') {
        if (!toast) {
            return;
        }
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = `toast show${variant ? ' ' + variant : ''}`;
        toastTimer = window.setTimeout(() => {
            toast.classList.remove('show');
        }, 3200);
    }
})();
