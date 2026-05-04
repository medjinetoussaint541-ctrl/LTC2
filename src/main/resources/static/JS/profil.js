/**
 * Gère les interactions de la page "Moi".
 * - Modification de la photo de profil
 * - Achat PRO (simulation)
 * - Sauvegarde des préférences (toggles, langue, unité)
 */

(() => {
    'use strict';

    const toast = document.getElementById('toast');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content ?? '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content ?? 'X-CSRF-TOKEN';
    let toastTimer = null;

    function showToast(message, variant = '') {
        if (!toast) return;
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = `toast show${variant ? ' ' + variant : ''}`;
        toastTimer = setTimeout(() => toast.classList.remove('show'), 3200);
    }

    // Gestion de la photo de profil
    const editPhotoBtn = document.getElementById('editPhotoBtn');
    if (editPhotoBtn) {
        editPhotoBtn.addEventListener('click', () => {
            // Simuler un upload ou un appel API
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = 'image/jpeg, image/png';
            input.onchange = async (e) => {
                const file = e.target.files[0];
                if (!file) return;
                // Ici vous pouvez envoyer le fichier vers votre endpoint
                // Exemple d'appel à /profil/photo
                const formData = new FormData();
                formData.append('photo', file);
                try {
                    const response = await fetch('/profil/photo', {
                        method: 'POST',
                        headers: {
                            [csrfHeader]: csrfToken
                        },
                        body: formData
                    });
                    if (response.ok) {
                        const data = await response.json();
                        const avatarImg = document.querySelector('.profil-avatar-large img');
                        if (avatarImg) avatarImg.src = data.photoUrl;
                            const headerAvatar = document.querySelector('.user-avatar-btn img');
                                if (headerAvatar) headerAvatar.src = data.photoUrl;
                                    showToast('Photo de profil mise à jour', 'success');
                    } else {
                            throw new Error();
                    }
                } catch (error) {
                    showToast('Erreur lors de la mise à jour', 'error');
                }
            };
            input.click();
        });
    }

    // Bouton Section Symbiose
    const symbioseBtn = document.getElementById('buyProBtn');
    if (symbioseBtn) {
        
        symbioseBtn.addEventListener('click', () => {
           
            window.location.href = '/symbiose';

            showToast('Créez ensemble des souvenirs inoubliables', 'symbiose');
        });
    }

    // Sauvegarde des actions dans la section paramettre
const toggles = document.querySelectorAll('.toggle-switch input');
toggles.forEach(toggle => {

    const key = toggle.id;
    const isProfileVisibilityToggle = key === 'adultToggle';
    const saved = isProfileVisibilityToggle ? null : localStorage.getItem(`pref_${key}`);

    if (saved !== null) toggle.checked = saved === 'true';

    toggle.addEventListener('change', async (e) => {
        const checked = e.target.checked;

        if (isProfileVisibilityToggle) {
            toggle.disabled = true;

            try {
                const response = await fetch('/profil/visibilite', {
                    method: 'POST',
                    headers: {
                        [csrfHeader]: csrfToken,
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                        Accept: 'application/json'
                    },
                    body: new URLSearchParams({ visible: String(checked) }).toString()
                });

                const data = await response.json().catch(() => ({}));

                if (!response.ok || data.success !== true) {
                    throw new Error(data.message || 'Impossible de mettre à jour la visibilité du profil.');
                }

                showToast(data.message || 'Préférence mise à jour.', 'info');
            } catch (error) {
                e.target.checked = !checked;
                showToast(error.message || 'Erreur lors de la mise à jour', 'error');
            } finally {
                toggle.disabled = false;
            }
            return;
        }

        localStorage.setItem(`pref_${key}`, checked);

        // Message personnalisé selon le toggle
        let message = '';
        let variant = 'success';

        if (key === 'adultToggle') {
            const etat = checked ? 'visible' : 'masqué';
            message = `Votre profil est désormais ${etat} aux autres utilisateurs.`;
            variant = 'info';
        } else if (key === 'locationToggle') {
            message = `Autorisation de position ${checked ? 'autorisée' : 'refusée'}`;
            variant = 'info';
        } else if (key === 'notifToggle') {

            message = `Notifications ${checked ? 'activées' : 'désactivées'}`;
            variant = 'info';

        } else {

            message = `Préférence mise à jour : ${key}`;

        }

        showToast(message, variant);
    });
});

    // Sélecteurs de langue / unité (simulation)
    const selectLikes = document.querySelectorAll('.select-like');
    selectLikes.forEach(select => {
        select.addEventListener('click', () => {
           
            const current = select.querySelector('span:first-child');
            if (select.innerText.includes('Français')) {

                current.innerText = 'English';

            } else if (select.innerText.includes('kilomètres')) {

                current.innerText = 'miles';

            } else {

                current.innerText = select.innerText.includes('English') ? 'Français' : 'kilomètres';

            }

            showToast('Modification effectuee', 'info');
        });
    });
})();
