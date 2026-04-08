(function () {
    const AUTO_CLOSE_MS = 10000;
    const RESTART_ANIMATION_MS = 170;
    let dismissTimer = null;

    function clearDismissTimer() {
        if (dismissTimer) {
            clearTimeout(dismissTimer);
            dismissTimer = null;
        }
    }

    function ensureToastElement() {
        let toast = document.getElementById('appToast');
        if (toast) {
            return toast;
        }

        toast = document.createElement('div');
        toast.id = 'appToast';
        toast.className = 'app-toast app-toast-error';
        toast.setAttribute('role', 'status');
        toast.setAttribute('aria-live', 'polite');
        toast.setAttribute('aria-atomic', 'true');
        toast.innerHTML = '' +
            '<div class="app-toast-body">' +
            '  <span class="app-toast-icon">!</span>' +
            '  <div class="app-toast-content">' +
            '    <p class="app-toast-title"></p>' +
            '    <p class="app-toast-message"></p>' +
            '  </div>' +
            '  <button type="button" class="app-toast-close" aria-label="Cerrar">&times;</button>' +
            '</div>';

        document.body.appendChild(toast);
        return toast;
    }

    function hideToast() {
        const toast = document.getElementById('appToast');
        if (!toast) {
            return;
        }
        clearDismissTimer();
        toast.classList.remove('app-toast-visible');
    }

    function resolveVariant(message, type, variant) {
        if (variant) {
            return variant;
        }
        if (type !== 'success') {
            return 'error';
        }
        const text = (message || '').toLowerCase();
        if (text.includes('desactiv')) {
            return 'deactivate';
        }
        if (text.includes('elimin')) {
            return 'delete';
        }
        if (text.includes('edit') || text.includes('actualiz') || text.includes('modific')) {
            return 'edit';
        }
        if (text.includes('cread') || text.includes('nuevo') || text.includes('registr')) {
            return 'create';
        }
        return 'create';
    }

    function getVariantLabel(variant, type) {
        if (type !== 'success') {
            return 'Error en la operacion';
        }
        switch (variant) {
            case 'edit':
                return 'Edicion completada';
            case 'delete':
                return 'Eliminacion completada';
            case 'deactivate':
                return 'Desactivacion completada';
            case 'create':
            default:
                return 'Creacion completada';
        }
    }

    function applyVariantClass(toast, variant) {
        toast.classList.remove('app-toast-op-create', 'app-toast-op-edit', 'app-toast-op-delete', 'app-toast-op-deactivate', 'app-toast-op-error');
        const normalized = variant || 'error';
        toast.classList.add('app-toast-op-' + normalized);
    }

    function wait(ms) {
        return new Promise(function (resolve) {
            setTimeout(resolve, ms);
        });
    }

    async function renderToast(message, type, variant) {
        const toast = ensureToastElement();
        const toastType = type === 'success' ? 'success' : 'error';
        const toastVariant = resolveVariant(message, toastType, variant);
        const icon = toast.querySelector('.app-toast-icon');
        const title = toast.querySelector('.app-toast-title');
        const text = toast.querySelector('.app-toast-message');
        const closeButton = toast.querySelector('.app-toast-close');

        if (toast.classList.contains('app-toast-visible')) {
            toast.classList.add('app-toast-restarting');
            toast.classList.remove('app-toast-visible');
            await wait(RESTART_ANIMATION_MS);
            toast.classList.remove('app-toast-restarting');
        }

        toast.classList.remove('app-toast-success', 'app-toast-error');
        toast.classList.add(toastType === 'success' ? 'app-toast-success' : 'app-toast-error');
        applyVariantClass(toast, toastType === 'success' ? toastVariant : 'error');
        toast.style.setProperty('--toast-duration', AUTO_CLOSE_MS + 'ms');

        if (icon) {
            if (toastType !== 'success') {
                icon.textContent = '!';
            } else if (toastVariant === 'edit') {
                icon.textContent = '~';
            } else if (toastVariant === 'delete') {
                icon.textContent = 'x';
            } else if (toastVariant === 'deactivate') {
                icon.textContent = '-';
            } else {
                icon.textContent = '+';
            }
        }
        if (title) {
            title.textContent = getVariantLabel(toastVariant, toastType);
        }
        if (text) {
            text.textContent = message;
        }

        if (closeButton && !closeButton.dataset.bound) {
            closeButton.addEventListener('click', hideToast);
            closeButton.dataset.bound = 'true';
        }

        clearDismissTimer();
        toast.classList.remove('app-toast-visible');
        void toast.offsetWidth;
        toast.classList.add('app-toast-visible');
        dismissTimer = setTimeout(hideToast, AUTO_CLOSE_MS);
    }

    window.showAppToast = function (message, type, variant) {
        if (!message) {
            return;
        }
        renderToast(message, type, variant);
    };

    document.addEventListener('DOMContentLoaded', function () {
        const toast = document.getElementById('appToast');
        if (!toast) {
            return;
        }

        const closeButton = toast.querySelector('.app-toast-close');
        if (closeButton) {
            closeButton.addEventListener('click', hideToast);
        }

        const initialMessage = toast.getAttribute('data-toast-message');
        const initialType = toast.getAttribute('data-toast-type');
        const initialVariant = toast.getAttribute('data-toast-variant');
        if (initialMessage) {
            renderToast(initialMessage, initialType, initialVariant);
        }
    });
})();

