(function() {
    const menu = document.querySelector('[data-user-menu]');
    if (!menu) {
        return;
    }

    const trigger = menu.querySelector('.user-trigger');
    const dropdown = menu.querySelector('[data-user-dropdown]');
    if (!trigger || !dropdown) {
        return;
    }

    function closeMenu() {
        dropdown.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
    }

    function toggleMenu() {
        const isHidden = dropdown.hidden;
        if (isHidden) {
            dropdown.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
        } else {
            closeMenu();
        }
    }

    trigger.addEventListener('click', (event) => {
        event.stopPropagation();
        toggleMenu();
    });

    document.addEventListener('click', (event) => {
        if (!menu.contains(event.target)) {
            closeMenu();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeMenu();
        }
    });
})();
