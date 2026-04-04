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
