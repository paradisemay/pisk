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

    menu.addEventListener('mouseenter', openMenu);
    menu.addEventListener('mouseleave', scheduleClose);
    menu.addEventListener('focusin', openMenu);
    menu.addEventListener('focusout', (event) => {
        if (!menu.contains(event.relatedTarget)) {
            scheduleClose();
        }
    });

    trigger.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeMenu();
        }
    });
})();
