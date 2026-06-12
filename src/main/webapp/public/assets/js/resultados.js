const state = {
            data: null,
            filters: {
                canton: "",
                parroquia: "",
                recinto: ""
            },
            tableSearch: "",
            currentPage: 1,
            rowsPerPage: 10,
            chart: null,
            loading: false,
            nextRefreshAt: null
        };

        const colors = ["#1d4ed8", "#047857", "#b45309", "#7c3aed", "#be123c", "#0891b2", "#4d7c0f", "#c2410c"];

        document.querySelectorAll(".tab").forEach(button => {
            button.addEventListener("click", () => {
                document.querySelectorAll(".tab").forEach(item => item.classList.remove("active"));
                document.querySelectorAll(".panel").forEach(panel => panel.classList.add("hidden"));
                button.classList.add("active");
                document.getElementById(button.dataset.tab).classList.remove("hidden");
                if (button.dataset.tab === "summaryPanel") {
                    drawChart(state.data ? state.data.results : []);
                }
            });
        });

        document.getElementById("refreshButton").addEventListener("click", loadResults);
        document.getElementById("clearFiltersButton").addEventListener("click", () => {
            state.filters.canton = "";
            state.filters.parroquia = "";
            state.filters.recinto = "";
            state.tableSearch = "";
            state.currentPage = 1;
            document.getElementById("tableSearch").value = "";
            renderFilters();
            renderTables();
        });
        document.getElementById("tableSearch").addEventListener("input", event => {
            state.tableSearch = event.target.value;
            state.currentPage = 1;
            renderTables();
        });
        document.getElementById("prevPageButton").addEventListener("click", () => {
            state.currentPage = Math.max(state.currentPage - 1, 1);
            renderTables();
        });
        document.getElementById("nextPageButton").addEventListener("click", () => {
            const totalPages = getTotalPages();
            state.currentPage = Math.min(state.currentPage + 1, totalPages);
            renderTables();
        });
        document.getElementById("cantonFilter").addEventListener("change", event => {
            state.filters.canton = event.target.value;
            state.filters.parroquia = "";
            state.filters.recinto = "";
            state.currentPage = 1;
            renderFilters();
            renderTables();
        });
        document.getElementById("parroquiaFilter").addEventListener("change", event => {
            state.filters.parroquia = event.target.value;
            state.filters.recinto = "";
            state.currentPage = 1;
            renderFilters();
            renderTables();
        });
        document.getElementById("recintoFilter").addEventListener("change", event => {
            state.filters.recinto = event.target.value;
            state.currentPage = 1;
            renderTables();
        });

        async function loadResults() {
            if (window.location.protocol === "file:") {
                document.getElementById("runtimeWarning").style.display = "block";
                document.getElementById("processName").textContent = "Abrir desde el servidor";
                document.getElementById("updatedAt").textContent = "--";
                return;
            }
            setLoading(true);
            try {
                const response = await fetch("resultados.json", {cache: "default"});
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }
                state.data = await response.json();
                state.nextRefreshAt = Date.now() + 60000;
                renderAll();
            } catch (error) {
                console.error("No se pudo cargar resultados publicos", error);
                document.getElementById("statusPill").className = "status-pill warning";
                document.getElementById("statusPill").textContent = "Sin conexion";
                document.getElementById("refreshMeta").textContent = "No se pudo actualizar";
            } finally {
                setLoading(false);
            }
        }

        function renderAll() {
            const data = state.data;
            document.getElementById("processName").textContent = data.hasActiveProcess ? data.process.name : "Sin proceso activo";
            document.getElementById("statusPill").className = data.hasActiveProcess ? "status-pill" : "status-pill warning";
            document.getElementById("statusPill").textContent = data.hasActiveProcess ? "Publicado" : "Sin proceso activo";
            document.getElementById("updatedAt").textContent = formatDate(data.generatedAt);
            document.getElementById("totalMesas").textContent = formatNumber(data.summary.totalMesas);
            document.getElementById("mesasCerradas").textContent = formatNumber(data.summary.mesasCerradas);
            document.getElementById("mesasPendientes").textContent = formatNumber(data.summary.mesasPendientes);
            document.getElementById("totalVotos").textContent = formatNumber(data.summary.totalVotosListas);
            document.getElementById("avanceTexto").textContent = formatDecimal(data.summary.porcentajeMesasCerradas) + "%";
            document.getElementById("avanceBar").style.width = Math.min(data.summary.porcentajeMesasCerradasEntero || 0, 100) + "%";
            renderResults();
            renderFilters();
            renderTables();
        }

        function renderResults() {
            const results = state.data.results || [];
            document.getElementById("summaryContent").classList.toggle("hidden", results.length === 0);
            document.getElementById("emptyResults").classList.toggle("hidden", results.length > 0);
            const list = document.getElementById("resultsList");
            list.innerHTML = "";
            results.forEach((item, index) => {
                const row = document.createElement("div");
                row.className = "result-item";
                row.innerHTML = `
                    <div class="result-name">${escapeHtml(item.name)}</div>
                    <div class="label">Votos de lista</div>
                    <div class="result-votes">${formatNumber(item.votes)}</div>
                    <div class="label" style="text-align:right">${formatDecimal(item.percentage)}%</div>
                `;
                list.appendChild(row);
            });
            drawChart(results);
        }

        function renderFilters() {
            const tables = state.data.tables || [];
            fillSelect("cantonFilter", uniqueValues(tables, "canton"), "-- Todos --", state.filters.canton);
            const parroquias = uniqueValues(tables.filter(item => match(state.filters.canton, item.canton)), "parroquia");
            fillSelect("parroquiaFilter", parroquias, "-- Todas --", state.filters.parroquia);
            const recintos = uniqueValues(tables.filter(item =>
                match(state.filters.canton, item.canton) && match(state.filters.parroquia, item.parroquia)
            ), "recinto");
            fillSelect("recintoFilter", recintos, "-- Todos --", state.filters.recinto);
        }

        function renderTables() {
            const filtered = getFilteredTables();
            const totalPages = getTotalPages(filtered);
            state.currentPage = Math.min(Math.max(state.currentPage, 1), totalPages);
            const start = (state.currentPage - 1) * state.rowsPerPage;
            const visible = filtered.slice(start, start + state.rowsPerPage);
            const body = document.getElementById("tablesBody");
            body.innerHTML = "";
            visible.forEach(item => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${escapeHtml(item.canton)}</td>
                    <td>${escapeHtml(item.parroquia)}</td>
                    <td>${escapeHtml(item.recinto)}</td>
                    <td>${escapeHtml(item.mesa)}</td>
                    <td class="number">${formatNumber(item.sufragantes)}</td>
                    <td class="number">${formatNumber(item.votes)}</td>
                    <td>${formatDate(item.closedAt)}</td>
                `;
                body.appendChild(row);
            });
            document.getElementById("emptyTables").classList.toggle("hidden", filtered.length > 0);
            document.getElementById("tableCount").textContent = formatNumber(filtered.length);
            document.getElementById("pageInfo").textContent = `Pagina ${state.currentPage} de ${totalPages}`;
            document.getElementById("prevPageButton").disabled = state.currentPage <= 1;
            document.getElementById("nextPageButton").disabled = state.currentPage >= totalPages;
        }

        function drawChart(results) {
            if (!window.Chart) {
                drawCanvasFallback(results);
                return;
            }
            const ctx = document.getElementById("resultsChart").getContext("2d");
            if (state.chart) {
                state.chart.destroy();
            }
            state.chart = new Chart(ctx, {
                type: "bar",
                data: {
                    labels: (results || []).map(item => item.name),
                    datasets: [{
                        label: "Votos de lista",
                        data: (results || []).map(item => item.votes || 0),
                        backgroundColor: (results || []).map((_, index) => colors[index % colors.length] + "CC"),
                        borderColor: (results || []).map((_, index) => colors[index % colors.length]),
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    legend: {display: false},
                    tooltips: {
                        callbacks: {
                            label: function(tooltipItem) {
                                return " Votos: " + formatNumber(tooltipItem.yLabel);
                            }
                        }
                    },
                    scales: {
                        xAxes: [{
                            gridLines: {display: false},
                            ticks: {fontColor: "#334155", fontStyle: "bold"}
                        }],
                        yAxes: [{
                            ticks: {
                                beginAtZero: true,
                                precision: 0,
                                fontColor: "#475569",
                                callback: value => formatNumber(value)
                            },
                            gridLines: {color: "#e2e8f0"}
                        }]
                    }
                }
            });
        }

        function drawCanvasFallback(results) {
            const canvas = document.getElementById("resultsChart");
            const ctx = canvas.getContext("2d");
            ctx.strokeStyle = "#e2e8f0";
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            if (!results || results.length === 0) {
                return;
            }
            const padding = {top: 24, right: 24, bottom: 80, left: 70};
            const width = canvas.width - padding.left - padding.right;
            const height = canvas.height - padding.top - padding.bottom;
            const max = Math.max(...results.map(item => item.votes || 0), 1);
            const barGap = 18;
            const barWidth = Math.max(32, (width - barGap * (results.length - 1)) / results.length);
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(padding.left, padding.top);
            ctx.lineTo(padding.left, padding.top + height);
            ctx.lineTo(padding.left + width, padding.top + height);
            ctx.stroke();

            results.forEach((item, index) => {
                const value = item.votes || 0;
                const barHeight = (value / max) * (height - 10);
                const x = padding.left + index * (barWidth + barGap);
                const y = padding.top + height - barHeight;
                ctx.fillStyle = colors[index % colors.length];
                roundRect(ctx, x, y, barWidth, barHeight, 8);
                ctx.fill();

                ctx.fillStyle = "#0f172a";
                ctx.font = "bold 20px Arial";
                ctx.textAlign = "center";
                ctx.fillText(formatNumber(value), x + barWidth / 2, y - 8);

                ctx.save();
                ctx.translate(x + barWidth / 2, padding.top + height + 14);
                ctx.rotate(-Math.PI / 6);
                ctx.fillStyle = "#334155";
                ctx.font = "bold 15px Arial";
                ctx.textAlign = "right";
                ctx.fillText(item.name || "", 0, 0);
                ctx.restore();
            });
        }

        function getFilteredTables() {
            const search = normalize(state.tableSearch);
            return (state.data.tables || []).filter(item => {
                const matchesFilters = match(state.filters.canton, item.canton) &&
                    match(state.filters.parroquia, item.parroquia) &&
                    match(state.filters.recinto, item.recinto);
                if (!matchesFilters || !search) {
                    return matchesFilters;
                }
                return [item.canton, item.parroquia, item.recinto, item.mesa]
                    .some(value => normalize(value).includes(search));
            });
        }

        function getTotalPages(items) {
            const total = items ? items.length : getFilteredTables().length;
            return Math.max(Math.ceil(total / state.rowsPerPage), 1);
        }

        function setLoading(loading) {
            state.loading = loading;
            document.getElementById("loaderDot").classList.toggle("loading", loading);
            document.getElementById("refreshButton").disabled = loading;
            if (loading) {
                document.getElementById("refreshMeta").textContent = "Actualizando...";
            } else if (state.nextRefreshAt) {
                updateRefreshMeta();
            }
        }

        function roundRect(ctx, x, y, width, height, radius) {
            const r = Math.min(radius, width / 2, height / 2);
            ctx.beginPath();
            ctx.moveTo(x + r, y);
            ctx.arcTo(x + width, y, x + width, y + height, r);
            ctx.arcTo(x + width, y + height, x, y + height, r);
            ctx.arcTo(x, y + height, x, y, r);
            ctx.arcTo(x, y, x + width, y, r);
            ctx.closePath();
        }

        function fillSelect(id, values, placeholder, selected) {
            const select = document.getElementById(id);
            select.innerHTML = "";
            select.appendChild(new Option(placeholder, ""));
            values.forEach(value => select.appendChild(new Option(value, value)));
            select.value = selected || "";
        }

        function uniqueValues(items, key) {
            return [...new Set(items.map(item => item[key]).filter(Boolean))].sort((a, b) => a.localeCompare(b));
        }

        function match(filter, value) {
            return !filter || filter === value;
        }

        function normalize(value) {
            return String(value || "")
                .normalize("NFD")
                .replace(/[\u0300-\u036f]/g, "")
                .toLowerCase()
                .trim();
        }

        function formatNumber(value) {
            return Number(value || 0).toLocaleString("es-EC");
        }

        function formatDecimal(value) {
            return Number(value || 0).toLocaleString("es-EC", {minimumFractionDigits: 2, maximumFractionDigits: 2});
        }

        function formatDate(value) {
            if (!value) {
                return "--";
            }
            return new Date(value).toLocaleString("es-EC", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit"
            });
        }

        function escapeHtml(value) {
            return String(value || "").replace(/[&<>"']/g, char => ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                "\"": "&quot;",
                "'": "&#039;"
            }[char]));
        }

        loadResults();
        setInterval(updateRefreshMeta, 1000);
        setInterval(loadResults, 60000);

        function updateRefreshMeta() {
            if (!state.nextRefreshAt) {
                return;
            }
            const remaining = Math.max(Math.ceil((state.nextRefreshAt - Date.now()) / 1000), 0);
            document.getElementById("refreshMeta").textContent = `Proxima actualizacion en ${remaining}s`;
        }
