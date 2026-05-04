/**
 * Gère l'aperçu local de l'avatar sélectionné lors de l'inscription.
 *
 * Responsabilités :
 * - récupérer les éléments du formulaire concernés ;
 * - réinitialiser l'aperçu si aucun fichier n'est sélectionné ;
 * - refuser les fichiers non image ;
 * - afficher un aperçu immédiat via FileReader sans envoi serveur.
 */

function updateAvatarPreview() {
    // Références DOM nécessaires à l'affichage de l'aperçu.
    const fileInput = document.getElementById('photo');
    const imgElement = document.getElementById('profileImage');
    const defaultIcon = document.getElementById('defaultIcon');
    
    // Cas nominal au chargement ou après suppression : aucun fichier disponible.
    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
        imgElement.removeAttribute('src');
        imgElement.style.display = 'none';
        defaultIcon.style.display = 'block';
        return;
    }
    
    // On ne traite que le premier fichier sélectionné.
    const file = fileInput.files[0];
    
    // Sécurisation côté client : on bloque immédiatement les formats non image.
    if (!file.type.startsWith('image/')) {
        fileInput.value = '';
        imgElement.removeAttribute('src');
        imgElement.style.display = 'none';
        defaultIcon.style.display = 'block';
        alert('Veuillez sélectionner un fichier image valide.');
        return;
    }
    
    // Lecture locale du fichier pour générer un aperçu instantané.
    const reader = new FileReader();
    reader.onload = function (event) {
         // Affiche l'image et masque l'icône par défaut dès que la lecture est terminée.
        imgElement.src = event.target.result;
        imgElement.style.display = 'block';
        defaultIcon.style.display = 'none';
    };

    reader.readAsDataURL(file);
}

// Réapplique l'état visuel à l'ouverture de la page (utile en cas de rechargement du formulaire).
window.onload = updateAvatarPreview;