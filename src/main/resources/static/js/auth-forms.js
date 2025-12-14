(function() {
    const toggleButtons = document.querySelectorAll('[data-password-toggle]');
    toggleButtons.forEach((button) => {
        const wrapper = button.closest('.password-field');
        const input = wrapper ? wrapper.querySelector('[data-password-field]') : null;
        if (!input) {
            return;
        }
        button.addEventListener('click', () => {
            const isHidden = input.getAttribute('type') === 'password';
            input.setAttribute('type', isHidden ? 'text' : 'password');
            button.textContent = isHidden ? button.dataset.hideLabel || 'Скрыть' : button.dataset.showLabel || 'Показать';
        });
    });
})();
