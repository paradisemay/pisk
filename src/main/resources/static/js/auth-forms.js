(function() {
    const toggleButtons = document.querySelectorAll('[data-password-toggle]');
    toggleButtons.forEach((button) => {
        const wrapper = button.closest('.password-field');
        const input = wrapper ? wrapper.querySelector('[data-password-field]') : null;
        if (!input) {
            return;
        }

        const showLabel = button.dataset.showLabel || 'Показать';
        const hideLabel = button.dataset.hideLabel || 'Скрыть';
        const eyeOpenIcon = button.querySelector('[data-eye-open]');
        const eyeClosedIcon = button.querySelector('[data-eye-closed]');

        const setVisibility = (visible) => {
            input.setAttribute('type', visible ? 'text' : 'password');
            button.setAttribute('aria-label', visible ? hideLabel : showLabel);
            button.dataset.visible = visible ? 'true' : 'false';

            if (eyeOpenIcon && eyeClosedIcon) {
                eyeOpenIcon.classList.toggle('hidden', !visible);
                eyeClosedIcon.classList.toggle('hidden', visible);
            }
        };

        setVisibility(false);

        button.addEventListener('click', () => {
            const isCurrentlyHidden = input.getAttribute('type') === 'password';
            setVisibility(isCurrentlyHidden);
        });
    });
})();
