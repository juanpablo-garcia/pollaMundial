// Restaurar foco en el input activo después de cada ajax parcial
(function() {
	var lastFocusedId = null;

	// Rastrear el último input con foco en todo momento
	$(document).on('focusin', 'input, textarea', function() {
		lastFocusedId = this.id;
	});

	$(document).on('pfAjaxComplete', function() {
		if (!lastFocusedId) return;
		// Pequeño delay para que PrimeFaces termine de actualizar el DOM
		setTimeout(function() {
			var el = document.getElementById(lastFocusedId);
			if (el) {
				el.focus();
				el.select();
			}
		}, 50);
	});
}());
