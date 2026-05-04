// symbiose.js – Interactions de la page Symbiose (toasts, jeux, films)
(function() {
    'use strict';

    const toast = document.getElementById('toast');
    let toastTimer = null;

    function showToast(message, variant = '') {
        if (!toast) return;
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = `toast show${variant ? ' ' + variant : ''}`;
        toastTimer = setTimeout(() => toast.classList.remove('show'), 3000);
    }

    // Gestion des boutons jeux/films
    document.querySelectorAll('.btn-symbiose').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const game = btn.dataset.game;
            const movie = btn.dataset.movie;
            if (game) {
                const container = btn.closest('.game-card');
                const gameTitle = container?.querySelector('.game-title')?.innerText || 'ce jeu';
                showToast(`🎮 Lancement du jeu "${gameTitle}" – bientôt disponible !`, 'symbiose');
            } else if (movie) {
                const container = btn.closest('.movie-card');
                const movieTitle = container?.querySelector('.movie-title')?.innerText || 'ce film';
                showToast(`🎬 Fiche du film "${movieTitle}" à venir`, 'symbiose');
            } else {
                showToast('Fonctionnalité en cours de développement', 'info');
            }
        });
    });
})();