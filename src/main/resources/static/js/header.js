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

    let hideTimeoutId;

    closeMenu();

    function openMenu() {
        clearTimeout(hideTimeoutId);
        dropdown.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
    }

    function closeMenu() {
        clearTimeout(hideTimeoutId);
        dropdown.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
    }

    function scheduleClose() {
        hideTimeoutId = window.setTimeout(() => closeMenu(), 120);
    }

    menu.addEventListener('pointerenter', openMenu);
    menu.addEventListener('pointerleave', scheduleClose);
    menu.addEventListener('focusin', openMenu);
    menu.addEventListener('focusout', (event) => {
        if (!menu.contains(event.relatedTarget)) {
            scheduleClose();
        }
    });

    document.addEventListener('pointerdown', (event) => {
        if (!menu.contains(event.target)) {
            closeMenu();
        }
    });

    trigger.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeMenu();
        }
    });
})();
