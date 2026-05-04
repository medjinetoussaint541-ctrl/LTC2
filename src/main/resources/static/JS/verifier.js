/**
 * verifier.js — LTC App
 * 
 * COMPORTEMENT IMPLEMENTE :
 * - Si l'utilisateur est déjà vérifié : la section de vérification (caméra) n'apparaît PAS
 * - Si l'utilisateur a déjà une photo de profil : le message d'invitation à ajouter une photo n'apparaît PAS
 * - Seule la section "Recherche par image" s'affiche pour les utilisateurs vérifiés
 * - Les autres comportements (caméra, upload, recherche par image) restent inchangés
 * - Gestion de la demande de relation : envoi et mise à jour du bouton
 */

'use strict';

let cameraStream = null;
let toastTimeoutId = null;

function qs(selector) {
    return document.querySelector(selector);
}

function setHidden(element, hidden) {
    if (element) {
        element.hidden = hidden;
    }
}

function showToast(message, variant = 'success') {
    const toast = qs('#toast');
    if (!toast) return;

    toast.textContent = message;
    toast.classList.remove('is-success', 'is-error', 'is-visible');
    toast.classList.add(variant === 'error' ? 'is-error' : 'is-success');

    requestAnimationFrame(() => toast.classList.add('is-visible'));

    clearTimeout(toastTimeoutId);
    toastTimeoutId = setTimeout(() => {
        toast.classList.remove('is-visible');
    }, 2800);
}

function getCsrfConfig() {
    const token = qs('meta[name="_csrf"]')?.getAttribute('content');
    const headerName = qs('meta[name="_csrf_header"]')?.getAttribute('content');
    return { token, headerName };
}

function getVerifyState() {
    const root = qs('#verifyRoot');

    return {
        isVerified: root?.dataset.isVerified === 'true',
        hasProfilePhoto: root?.dataset.hasProfilePhoto === 'true'
    };
}

function updateVerifyStateInHtml(isVerified) {
    const root = qs('#verifyRoot');
    if (!root) return;

    root.dataset.isVerified = String(isVerified);
    renderVerifyState();
}

/**
 * RENDRE L'INTERFACE SELON L'ÉTAT DE L'UTILISATEUR
 * 
 * Règles d'affichage :
 * 1. Si utilisateur déjà vérifié :
 *    - N'afficher QUE la section "Recherche par image" (verifiedView)
 *    - Masquer COMPLÈTEMENT la section de vérification (notVerifiedView, missingPhotoBox, cameraVerifyBox)
 * 
 * 2. Si utilisateur non vérifié ET a une photo de profil :
 *    - Afficher la section de vérification par caméra (cameraVerifyBox)
 *    - Masquer le message "Photo de profil requise" (missingPhotoBox)
 * 
 * 3. Si utilisateur non vérifié ET n'a PAS de photo de profil :
 *    - Afficher le message invitant à ajouter une photo (missingPhotoBox)
 *    - Masquer la section caméra (cameraVerifyBox)
 */
function renderVerifyState() {
    const { isVerified, hasProfilePhoto } = getVerifyState();

    const verifyHero = qs('#verifyhero');

    const notVerifiedView = qs('#notVerifiedView');
    const verifiedView = qs('#verifiedView');
    const missingPhotoBox = qs('#missingPhotoBox');
    const cameraVerifyBox = qs('#cameraVerifyBox');

    // CAS 1 : Utilisateur déjà vérifié → seule la recherche par image s'affiche
    if (isVerified) {
        setHidden(verifyHero, true); // Masque toute la section héro (optionnel, selon design)
        setHidden(notVerifiedView, true);   // Masque toute la section non vérifié
        setHidden(verifiedView, false);      // Affiche la recherche par image
        setHidden(missingPhotoBox, true);    // Masque le message photo manquante
        setHidden(cameraVerifyBox, true);    // Masque la caméra
        stopCamera();                        // Arrête la caméra si elle était active
        return;
    }

    // CAS 2 : Utilisateur non vérifié → affiche la section de vérification
    setHidden(notVerifiedView, false);  // Affiche la section non vérifié
    setHidden(verifiedView, true);      // Masque la recherche par image

    // SOUS-CAS A : Pas de photo de profil → message d'ajout de photo
    if (!hasProfilePhoto) {
        setHidden(missingPhotoBox, false);  // Affiche le message "Photo de profil requise"
        setHidden(cameraVerifyBox, true);   // Masque la caméra
        stopCamera();                       // Arrête la caméra si elle était active
        return;
    }

    // SOUS-CAS B : Photo de profil présente → affiche la caméra pour vérification
    setHidden(missingPhotoBox, true);    // Masque le message photo manquante
    setHidden(cameraVerifyBox, false);    // Affiche la caméra
}

async function openCamera() {
    const video = qs('#cameraVideo');
    const placeholder = qs('#cameraPlaceholder');
    const openBtn = qs('#openCameraBtn');
    const captureBtn = qs('#captureFaceBtn');
    const stopBtn = qs('#stopCameraBtn');
    const status = qs('#verifyStatus');

    if (!navigator.mediaDevices?.getUserMedia) {
        showToast('Votre navigateur ne supporte pas l’accès à la caméra.', 'error');
        return;
    }

    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'user' },
            audio: false
        });

        if (video) {
            video.srcObject = cameraStream;
        }

        setHidden(placeholder, true);

        if (openBtn) openBtn.disabled = true;
        if (captureBtn) captureBtn.disabled = false;
        if (stopBtn) stopBtn.disabled = false;

        if (status) {
            status.textContent = 'Caméra ouverte. Placez votre visage au centre.';
        }

        showToast('Caméra ouverte avec succès.', 'success');
    } catch (error) {
        if (status) {
            status.textContent = 'Accès caméra refusé ou indisponible.';
        }

        showToast('Impossible d’ouvrir la caméra.', 'error');
    }
}

function stopCamera() {
    const video = qs('#cameraVideo');
    const placeholder = qs('#cameraPlaceholder');
    const openBtn = qs('#openCameraBtn');
    const captureBtn = qs('#captureFaceBtn');
    const stopBtn = qs('#stopCameraBtn');
    const status = qs('#verifyStatus');

    if (cameraStream) {
        cameraStream.getTracks().forEach(track => track.stop());
        cameraStream = null;
    }

    if (video) {
        video.srcObject = null;
    }

    setHidden(placeholder, false);

    if (openBtn) openBtn.disabled = false;
    if (captureBtn) captureBtn.disabled = true;
    if (stopBtn) stopBtn.disabled = true;
    if (status) status.textContent = '';
}

function captureCurrentFaceImage() {
    const video = qs('#cameraVideo');

    if (!video || !cameraStream) {
        return null;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    const context = canvas.getContext('2d');
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    return new Promise(resolve => {
        canvas.toBlob(blob => {
            resolve(blob);
        }, 'image/jpeg', 0.92);
    });
}

async function submitFaceVerification() {
    const status = qs('#verifyStatus');
    const captureBtn = qs('#captureFaceBtn');
    const { token, headerName } = getCsrfConfig();

    if (!cameraStream) {
        showToast('Veuillez ouvrir la caméra avant de scanner.', 'error');
        return;
    }

    try {
        if (captureBtn) captureBtn.disabled = true;

        if (status) {
            status.textContent = 'Vérification du visage en cours...';
        }

        const selfieBlob = await captureCurrentFaceImage();

        if (!selfieBlob) {
            throw new Error('Impossible de capturer le selfie.');
        }

        const formData = new FormData();
        formData.append('selfie', selfieBlob, 'selfie.jpg');

        const headers = {
            Accept: 'application/json'
        };

        if (token && headerName) {
            headers[headerName] = token;
        }

        const response = await fetch('/compreface/verify-selfie', {
            method: 'POST',
            headers,
            body: formData,
            credentials: 'same-origin'
        });

        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(payload.message || 'Erreur pendant la vérification.');
        }

        if (payload.verified === true || payload.success === true) {
            updateVerifyStateInHtml(true);
            stopCamera();

            showToast(
                payload.message || 'Compte vérifié avec succès.',
                'success'
            );

            return;
        }

        if (status) {
            status.textContent = payload.message || 'Vérification refusée.';
        }

        showToast(
            payload.message || 'Vérification refusée.',
            'error'
        );

    } catch (error) {
        if (status) {
            status.textContent = error.message || 'Erreur pendant la vérification.';
        }

        showToast(
            error.message || 'Erreur pendant la vérification.',
            'error'
        );

    } finally {
        if (captureBtn && cameraStream) {
            captureBtn.disabled = false;
        }
    }
}

function initImageUpload() {
    const input = qs('#searchImageInput');
    const previewBox = qs('#selectedImagePreview');
    const selectedImage = qs('#selectedImage');
    const selectedImageName = qs('#selectedImageName');
    const searchBtn = qs('#searchByImageBtn');
    const resultCard = qs('#searchResultCard');

    if (!input) return;

    input.addEventListener('change', () => {
        const file = input.files?.[0];

        setHidden(resultCard, true);

        if (!file) {
            setHidden(previewBox, true);
            if (searchBtn) searchBtn.disabled = true;
            return;
        }

        selectedImage.src = URL.createObjectURL(file);
        selectedImageName.textContent = file.name;

        setHidden(previewBox, false);
        if (searchBtn) searchBtn.disabled = false;
    });
}

// ==================== NOUVEAU : GESTION DE LA DEMANDE DE RELATION ====================

/**
 * Envoie une demande de relation à l'utilisateur ciblé
 * @param {string|number} receveurId - L'ID de l'utilisateur à qui envoyer la demande
 * @param {HTMLElement} button - Le bouton de demande de relation
 */
async function sendRelationRequest(receveurId, button) {
    const { token, headerName } = getCsrfConfig();

    try {
        // Désactiver le bouton pendant l'envoi
        const originalContent = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Envoi...';

        const headers = {
            'Accept': 'application/json'
        };

        if (token && headerName) {
            headers[headerName] = token;
        }

        const response = await fetch(`/demande/${receveurId}`, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin'
        });

        const data = await response.json().catch(() => ({}));

        if (response.ok) {
            // Succès - Demande envoyée ou relation confirmée
            if (data.alreadyConfirmed) {
                updateButtonForConfirmedRelation(button);
                showToast('Vous êtes déjà en relation avec cette personne !', 'info');
            } else if (data.requiresConfirmation === false && data.success) {
                updateButtonForConfirmedRelation(button);
                showToast('Demande acceptée ! Vous êtes maintenant en relation.', 'success');
            } else {
                updateButtonForPendingRequest(button);
                showToast('Demande envoyée avec succès !', 'success');
            }
        } else if (response.status === 409) {
            // Conflit - Demande déjà existante ou relation existante
            if (data.code === 'PENDING_REQUEST_ALREADY_EXISTS') {
                if (confirm('Une demande est déjà en attente. Voulez-vous confirmer la relation maintenant ?')) {
                    await confirmRelationRequest(receveurId, button);
                } else {
                    restoreButton(button);
                }
            } else if (data.code === 'ALREADY_IN_RELATION_WITH_YOU') {
                updateButtonForConfirmedRelation(button);
                showToast('Vous êtes déjà en relation avec cette personne !', 'info');
            } else if (data.requiresConfirmation && data.pendingFromOther) {
                if (confirm('Cette personne vous a déjà envoyé une demande. Voulez-vous l\'accepter ?')) {
                    await confirmRelationRequest(receveurId, button);
                } else {
                    restoreButton(button);
                }
            } else {
                restoreButton(button);
                showToast(data.message || 'Une demande est déjà en attente', 'error');
            }
        } else {
            // Autre erreur
            let errorMessage = 'Une erreur est survenue';
            switch (data.code) {
                case 'SELF_REQUEST_NOT_ALLOWED':
                    errorMessage = 'Vous ne pouvez pas envoyer une demande à vous-même';
                    break;
                case 'USER_NOT_FOUND':
                    errorMessage = 'Utilisateur non trouvé';
                    break;
                default:
                    errorMessage = data.message || errorMessage;
            }
            restoreButton(button);
            showToast(errorMessage, 'error');
        }
    } catch (error) {
        console.error('Erreur lors de l\'envoi de la demande:', error);
        restoreButton(button);
        showToast('Erreur de connexion. Veuillez réessayer.', 'error');
    } finally {
        button.disabled = false;
    }
}

/**
 * Confirme une demande de relation existante
 * @param {string|number} receveurId - L'ID de l'utilisateur
 * @param {HTMLElement} button - Le bouton de demande de relation
 */
async function confirmRelationRequest(receveurId, button) {
    const { token, headerName } = getCsrfConfig();

    try {
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Confirmation...';

        const headers = {
            'Accept': 'application/json'
        };

        if (token && headerName) {
            headers[headerName] = token;
        }

        const response = await fetch(`/demande/${receveurId}?confirmer=true`, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin'
        });

        if (response.ok) {
            updateButtonForConfirmedRelation(button);
            showToast('Relation confirmée avec succès !', 'success');
        } else {
            const data = await response.json().catch(() => ({}));
            restoreButton(button);
            showToast(data.message || 'Erreur lors de la confirmation', 'error');
        }
    } catch (error) {
        console.error('Erreur lors de la confirmation:', error);
        restoreButton(button);
        showToast('Erreur de connexion. Veuillez réessayer.', 'error');
    } finally {
        button.disabled = false;
    }
}

/**
 * Met à jour le bouton pour l'état "demande en attente"
 * @param {HTMLElement} button 
 */
function updateButtonForPendingRequest(button) {
    button.innerHTML = `
        <span class="material-symbols-outlined">hourglass_top</span>
        Demande envoyée
    `;
    button.classList.add('btn-pending');
    button.disabled = true;
    button.setAttribute('data-request-status', 'pending');
}

/**
 * Met à jour le bouton pour l'état "en relation"
 * @param {HTMLElement} button 
 */
function updateButtonForConfirmedRelation(button) {
    button.innerHTML = `
        <span class="material-symbols-outlined">check_circle</span>
        En relation
    `;
    button.classList.add('btn-confirmed');
    button.disabled = true;
    button.setAttribute('data-request-status', 'confirmed');
}

/**
 * Restaure le bouton à son état original
 * @param {HTMLElement} button 
 */
function restoreButton(button) {
    button.innerHTML = `
        <span class="material-symbols-outlined">favorite</span>
        Demander une relation
    `;
    button.classList.remove('btn-pending', 'btn-confirmed');
    button.disabled = false;
    button.setAttribute('data-request-status', 'none');
}

/**
 * Initialise l'écouteur d'événement pour le bouton de demande de relation
 */
function initRelationRequestButton() {
    const relationBtn = qs('.relation-request-btn');
    
    if (!relationBtn) return;
    
    // Supprimer l'ancien écouteur pour éviter les doublons
    const newBtn = relationBtn.cloneNode(true);
    relationBtn.parentNode?.replaceChild(newBtn, relationBtn);
    
    // Ajouter le nouvel écouteur
    newBtn.addEventListener('click', (event) => {
        event.preventDefault();
        const targetId = newBtn.dataset.userId || newBtn.dataset.id;
        
        if (!targetId) {
            showToast('ID utilisateur manquant', 'error');
            return;
        }
        
        // Vérifier si le bouton n'est pas déjà dans un état final
        const status = newBtn.getAttribute('data-request-status');
        if (status === 'pending' || status === 'confirmed') {
            showToast('Une demande est déjà en cours', 'info');
            return;
        }
        
        sendRelationRequest(targetId, newBtn);
    });
}

/**
 * Ajoute les styles CSS pour les boutons de relation
 */
function addRelationButtonStyles() {
    if (document.querySelector('#relation-btn-styles')) return;
    
    const style = document.createElement('style');
    style.id = 'relation-btn-styles';
    style.textContent = `
        .relation-request-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }
        
        .btn-pending {
            background-color: #ff9800 !important;
            cursor: default !important;
        }
        
        .btn-confirmed {
            background-color: #4caf50 !important;
            cursor: default !important;
        }
        
        .spinner-border-sm {
            display: inline-block;
            width: 1rem;
            height: 1rem;
            border: 0.2em solid currentColor;
            border-right-color: transparent;
            border-radius: 50%;
            animation: spinner-border 0.75s linear infinite;
        }
        
        @keyframes spinner-border {
            to { transform: rotate(360deg); }
        }
    `;
    document.head.appendChild(style);
}

// ==================== FIN NOUVEAU ====================

async function searchByImage() {
    const input = qs('#searchImageInput');
    const searchBtn = qs('#searchByImageBtn');
    const resultCard = qs('#searchResultCard');
    const resultPhoto = qs('#resultPhoto');
    const resultName = qs('#resultName');
    const resultStatus = qs('#resultStatus');
    const relationBtn = qs('.relation-request-btn');

    const { token, headerName } = getCsrfConfig();
    const file = input?.files?.[0];

    if (!file) {
        showToast('Veuillez d’abord uploader une image.', 'error');
        return;
    }

    try {
        if (searchBtn) searchBtn.disabled = true;

        const formData = new FormData();
        formData.append('image', file);

        const headers = {
            Accept: 'application/json'
        };

        if (token && headerName) {
            headers[headerName] = token;
        }

        const response = await fetch('/compreface/recognize', {
            method: 'POST',
            headers,
            body: formData,
            credentials: 'same-origin'
        });

        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(payload.message || 'Erreur pendant la recherche. Veuillez réessayer.');
        }

        if (!payload.found || !payload.photoUrl) {
            setHidden(resultCard, true);
            showToast(payload.message || 'Aucun profil trouvé.', 'error');
            return;
        }

        const profile = payload;

        resultPhoto.src = profile.photoUrl || URL.createObjectURL(file);
        resultPhoto.alt = `Photo de ${profile.fullName || 'la personne trouvée'}`;
        resultName.textContent = profile.fullName || 'Nom non disponible';
        resultStatus.textContent = `Statut : ${profile.userStatus || 'Non renseigné'}`;
        
        // Mise à jour du data-id sur le bouton de relation
        if (relationBtn) {
            // Réinitialiser l'état du bouton avant de définir le nouvel ID
            const currentStatus = relationBtn.getAttribute('data-request-status');
            if (currentStatus !== 'pending' && currentStatus !== 'confirmed') {
                restoreButton(relationBtn);
            }
            relationBtn.dataset.userId = profile.userId || '';
            relationBtn.dataset.id = profile.userId || '';
        }

        setHidden(resultCard, false);
        showToast(payload.message || 'Profil trouvé.', 'success');
        
        // Réinitialiser l'écouteur du bouton avec le nouvel ID
        initRelationRequestButton();
        
    } catch (error) {
        setHidden(resultCard, true);
        showToast(error.message || 'Erreur pendant la recherche.', 'error');
    } finally {
        if (searchBtn) searchBtn.disabled = false;
    }
}

function initActions() {
    qs('#openCameraBtn')?.addEventListener('click', openCamera);
    qs('#stopCameraBtn')?.addEventListener('click', stopCamera);
    qs('#captureFaceBtn')?.addEventListener('click', submitFaceVerification);
    qs('#searchByImageBtn')?.addEventListener('click', searchByImage);
}

document.addEventListener('DOMContentLoaded', () => {
    renderVerifyState();
    initActions();
    initImageUpload();
    addRelationButtonStyles();        // Ajout des styles
    initRelationRequestButton();       // Initialisation du bouton de relation
});