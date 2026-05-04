/**
 * Scripts d'interaction de la landing page.
 *
 * Ce fichier couvre trois comportements principaux :
 * 1. ouverture / fermeture du menu mobile ;
 * 2. animation d'apparition progressive des blocs au scroll ;
 * 3. réduction dynamique du padding de la navbar lors du défilement.
 */



// Sélection des principaux éléments d'interface manipulés dans le fichier.
const nav = document.querySelector('nav');
const navToggle = document.querySelector('.nav-toggle');
const navLinks = document.querySelector('.nav-links');
const mobileBreakpoint = window.matchMedia('(max-width: 768px)');

//Ferme le menu mobile et remet les attributs d'accessibilité dans un état cohérent.
const closeMenu = () => {
    if (!navToggle || !navLinks) return;
    navLinks.classList.remove('open');
    document.body.classList.remove('nav-open');
    navToggle.setAttribute('aria-expanded', 'false');
};

//Ouvre le menu mobile
const openMenu = () => {
    if (!navToggle || !navLinks) return;
    navLinks.classList.add('open');
    document.body.classList.add('nav-open');
    navToggle.setAttribute('aria-expanded', 'true');
};

// Observation des éléments à révéler lorsqu'ils entrent dans la zone visible de l'écran
const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            observer.unobserve(entry.target);
        }
    });
}, { threshold: 0.12 });

// Chaque élément marqué `.reveal` est observé une seule fois pour éviter des traitements inutiles.
document.querySelectorAll('.reveal').forEach((element) => observer.observe(element));

if (navToggle && navLinks) {
//    Bascule du menu hamburger.
    navToggle.addEventListener('click', () => {
        const isOpen = navLinks.classList.contains('open');
        if (isOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    });
    
//    Ferme automatiquement le menu après le clic sur un lien de navigation.
    navLinks.querySelectorAll('a').forEach((link) => {
        link.addEventListener('click', closeMenu);
    });
    
//    Fermeture clavier pour améliorer l'accessibilité et l'expérience mobile
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeMenu();
        }
    });
    
//    Si l'on repasse en mode desktop, on réinitialise l'état mobile.
    mobileBreakpoint.addEventListener('change', (event) => {
        if (!event.matches) {
            closeMenu();
        }
    });
}

//Ajustement visuel de la hauteur perçue de la navbar au scroll.
window.addEventListener('scroll', () => {
    nav.style.padding = window.scrollY > 60 ? '12px 0' : (mobileBreakpoint.matches ? '16px 0' : '20px 0');
});