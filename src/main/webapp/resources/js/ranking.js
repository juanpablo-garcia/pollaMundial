var rankingChartInstance = null;

function colorForName(name) {
    // Hash determinista del nombre → tono HSL fijo
    var hash = 0;
    for (var i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
        hash |= 0;
    }
    var hue = ((hash % 360) + 360) % 360;
    return {
        bg:  'hsla(' + hue + ', 65%, 55%, 0.75)',
        brd: 'hsla(' + hue + ', 65%, 40%, 1)'
    };
}

function initRankingChart() {
    var hiddenEl = document.querySelector('[id$="chartJson"]');
    if (!hiddenEl) return;

    var canvas = document.getElementById('rankingChart');
    if (!canvas) return;

    var data;
    try {
        data = JSON.parse(hiddenEl.value);
    } catch (e) {
        return;
    }

    if (rankingChartInstance) {
        rankingChartInstance.destroy();
        rankingChartInstance = null;
    }

    // Altura dinámica: ~28px por participante para que quepan TODOS los nombres
    var wrapper = document.getElementById('rankingChartWrapper');
    if (wrapper) {
        wrapper.style.height = Math.max(500, data.labels.length * 28 + 80) + 'px';
    }

    var bgColors  = data.labels.map(function(name) { return colorForName(name).bg; });
    var brdColors = data.labels.map(function(name) { return colorForName(name).brd; });

    // Chart.js 2.9.4 (bundled in PrimeFaces 10)
    rankingChartInstance = new Chart(canvas, {
        type: 'horizontalBar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Puntos',
                data: data.values,
                backgroundColor: bgColors,
                borderColor: brdColors,
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            legend: { display: false },
            scales: {
                xAxes: [{
                    ticks: { beginAtZero: true }
                }]
            }
        }
    });
}

// Como la pestaña "Gráfica" es la primera (activa por defecto), inicializamos
// la gráfica al cargar la página, no solo al cambiar de pestaña.
document.addEventListener('DOMContentLoaded', function () {
    initRankingChart();
});
