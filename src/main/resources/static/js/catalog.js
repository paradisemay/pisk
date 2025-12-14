document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('.filters');
  if (!form) return;

  form.addEventListener('change', () => {
    const tagsInput = form.querySelector('input[name="tags"]');
    if (tagsInput && tagsInput.value) {
      tagsInput.value = tagsInput.value
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean)
        .join(',');
    }
    form.submit();
  });
});
