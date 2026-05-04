/**
 * Gère les interactions de la page Chat.
 *
 * Responsabilités principales :
 * - envoyer un message à la relation active ;
 * - récupérer les nouveaux messages par polling léger ;
 * - maintenir l'interface synchronisée sans rechargement complet.
 */

(() => {
    'use strict';

    const form = document.getElementById('chatForm');
    const input = document.getElementById('chatInput');
    const sendButton = document.getElementById('sendButton');
    const messageList = document.getElementById('messageList');
    const conversationEmpty = document.getElementById('conversationEmpty');
    const toast = document.getElementById('toast');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content ?? '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content ?? 'X-CSRF-TOKEN';

    let toastTimer = null;
    let pollingTimer = null;
    let sending = false;
    let pollingStopped = false;
    let lastMessageId = getLastMessageId();

    if (!form || !input || !messageList) {
        return;
    }

    autoResizeInput();
    scrollToBottom();
    markVisibleMessagesAsRead();

    input.addEventListener('input', () => {
        autoResizeInput();
    });

    input.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();

        const content = input.value.trim();
        if (!content || sending) {
            return;
        }

        setComposerDisabled(true);

        try {
            const response = await fetch('/chat/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                    [csrfHeader]: csrfToken
                },
                credentials: 'same-origin',
                body: JSON.stringify({content})
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw createRequestError(response.status, payload);
            }

            appendMessage(payload);
            input.value = '';
            autoResizeInput();
            toggleEmptyState(false);
            lastMessageId = payload.id ?? lastMessageId;
            scrollToBottom();
        } catch (error) {
            showToast(error.message || 'Impossible d’envoyer le message.', 'error');
        } finally {
            setComposerDisabled(false);
            input.focus();
        }
    });

    pollingTimer = window.setInterval(() => {
        if (document.hidden || sending || pollingStopped) {
            return;
        }
        refreshMessages();
    }, 5000);

    document.addEventListener('visibilitychange', () => {
        if (!document.hidden && !sending && !pollingStopped) {
            refreshMessages();
        }
    });

    async function refreshMessages() {
        try {
            const url = lastMessageId
                ? `/chat/messages?after=${encodeURIComponent(lastMessageId)}`
                : '/chat/messages';

            const response = await fetch(url, {
                headers: {
                    Accept: 'application/json'
                },
                credentials: 'same-origin'
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw createRequestError(response.status, payload);
            }

            updatePartnerPresence(payload.partnerPresence);

            const messages = Array.isArray(payload.messages) ? payload.messages : [];
            if (!messages.length) {
                return;
            }

            const shouldStickToBottom = isNearBottom();
            toggleEmptyState(false);
            let latestPartnerMessageId = null;

            messages.forEach(message => {
                appendMessage(message);
                lastMessageId = message.id ?? lastMessageId;
                if (!message.mine && message.id != null) {
                    latestPartnerMessageId = message.id;
                }
            });

            if (shouldStickToBottom) {
                scrollToBottom();
            }

            if (latestPartnerMessageId != null) {
                await markConversationAsRead(latestPartnerMessageId);
            }
        } catch (error) {
            stopPolling();
            setComposerDisabled(true);
            showToast(
                error.status === 409
                    ? 'Le chat n’est plus disponible sans relation active.'
                    : (error.message || 'Impossible de synchroniser la conversation.'),
                'error'
            );
        }
    }

    function appendMessage(message) {
        if (!message || !messageList || message.id == null) {
            return;
        }
        if (messageList.querySelector(`[data-message-id="${message.id}"]`)) {
            return;
        }

        const row = document.createElement('article');
        row.className = `message-row ${message.mine ? 'is-mine' : 'is-partner'}`;
        row.dataset.messageId = String(message.id);

        if (!message.mine) {
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.setAttribute('aria-hidden', 'true');

            if (message.senderPhotoUrl) {
                const image = document.createElement('img');
                image.src = message.senderPhotoUrl;
                image.alt = '';
                avatar.appendChild(image);
            } else {
                const initials = document.createElement('span');
                initials.className = 'avatar-initials';
                initials.textContent = message.senderInitials || 'U';
                avatar.appendChild(initials);
            }

            row.appendChild(avatar);
        }

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        if (message.newForCurrentUser) {
            bubble.classList.add('is-new');
        }

        if (message.newForCurrentUser) {
            const newDot = document.createElement('span');
            newDot.className = 'message-new-dot';
            newDot.setAttribute('aria-hidden', 'true');
            bubble.appendChild(newDot);
        }

        const text = document.createElement('div');
        text.className = 'message-text';
        text.textContent = message.content || '';

        const meta = document.createElement('div');
        meta.className = 'message-meta';

        const sender = document.createElement('span');
        sender.textContent = message.senderName || (message.mine ? 'Vous' : 'Partenaire');

        const time = document.createElement('time');
        time.textContent = message.timeLabel || 'À l’instant';
        if (message.dateTimeLabel) {
            time.dateTime = message.dateTimeLabel;
        }

        meta.append(sender, time);
        bubble.append(text, meta);
        row.appendChild(bubble);
        messageList.appendChild(row);
    }

    function setComposerDisabled(disabled) {
        sending = disabled;
        input.disabled = disabled;
        sendButton.disabled = disabled;
    }

    function autoResizeInput() {
        input.style.height = 'auto';
        input.style.height = `${Math.min(input.scrollHeight, 180)}px`;
    }

    function toggleEmptyState(visible) {
        if (!conversationEmpty) {
            return;
        }
        conversationEmpty.classList.toggle('is-hidden', !visible);
    }

    function updatePartnerPresence(presence) {
        if (!presence) {
            return;
        }

        const presenceLine = document.getElementById('partnerPresenceLine');
        const presenceLabel = document.getElementById('partnerPresenceLabel');
        const presenceDetail = document.getElementById('partnerPresenceDetail');
        const presenceChip = document.getElementById('partnerPresenceChip');
        const presenceChipLabel = document.getElementById('partnerPresenceChipLabel');

        const presenceClass = presence.online ? 'is-online' : 'is-offline';
        const obsoleteClass = presence.online ? 'is-offline' : 'is-online';

        if (presenceLine) {
            presenceLine.classList.remove(obsoleteClass);
            presenceLine.classList.add(presenceClass);
        }
        if (presenceChip) {
            presenceChip.classList.remove(obsoleteClass);
            presenceChip.classList.add(presenceClass);
        }
        if (presenceLabel) {
            presenceLabel.textContent = presence.statusLabel || '';
        }
        if (presenceChipLabel) {
            presenceChipLabel.textContent = presence.statusLabel || '';
        }
        if (presenceDetail) {
            presenceDetail.textContent = presence.detailLabel || '';
        }
    }

    function getLastMessageId() {
        const lastMessage = messageList?.querySelector('.message-row:last-of-type');
        if (!lastMessage?.dataset.messageId) {
            return null;
        }
        const parsed = Number(lastMessage.dataset.messageId);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function isNearBottom() {
        const container = document.getElementById('messagesContainer');
        if (!container) {
            return true;
        }
        return container.scrollHeight - container.scrollTop - container.clientHeight < 80;
    }

    function scrollToBottom() {
        const container = document.getElementById('messagesContainer');
        if (!container) {
            return;
        }
        container.scrollTop = container.scrollHeight;
    }

    function getLastVisiblePartnerMessageId() {
        const partnerMessages = messageList?.querySelectorAll('.message-row.is-partner');
        const lastPartnerMessage = partnerMessages?.[partnerMessages.length - 1];
        if (!lastPartnerMessage?.dataset.messageId) {
            return null;
        }

        const parsed = Number(lastPartnerMessage.dataset.messageId);
        return Number.isFinite(parsed) ? parsed : null;
    }

    async function markConversationAsRead(lastVisibleMessageId) {
        if (lastVisibleMessageId == null) {
            return;
        }

        try {
            await fetch('/chat/read', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                    [csrfHeader]: csrfToken
                },
                credentials: 'same-origin',
                body: JSON.stringify({lastVisibleMessageId})
            });
        } catch (error) {
            console.error(error);
        }
    }

    function markVisibleMessagesAsRead() {
        const lastVisiblePartnerMessageId = getLastVisiblePartnerMessageId();
        if (lastVisiblePartnerMessageId != null) {
            void markConversationAsRead(lastVisiblePartnerMessageId);
        }
    }

    function createRequestError(status, payload) {
        const error = new Error(payload.message || payload.detail || 'Une erreur est survenue.');
        error.status = status;
        return error;
    }

    function stopPolling() {
        pollingStopped = true;
        if (pollingTimer) {
            window.clearInterval(pollingTimer);
            pollingTimer = null;
        }
    }

    function showToast(message, type) {
        if (!toast || !message) {
            return;
        }

        toast.textContent = message;
        toast.classList.remove('success', 'error', 'show');
        if (type) {
            toast.classList.add(type);
        }

        window.clearTimeout(toastTimer);
        window.requestAnimationFrame(() => {
            toast.classList.add('show');
        });

        toastTimer = window.setTimeout(() => {
            toast.classList.remove('show');
        }, 2800);
    }
})();
