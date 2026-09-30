/* Portal público de resultados: consume resultados.json y lo presenta sin dependencias externas. */
(function () {
    "use strict";

    const REFRESH_MS = 60000;

    const state = {
        data: null,
        filters: {canton: "", parroquia: "", recinto: ""},
        tableSearch: "",
        currentPage: 1,
        rowsPerPage: 10,
        loading: false,
        nextRefreshAt: null,
        // Diferencia entre el reloj del servidor y el del navegador (cabecera Date).
        offsetMs: 0,
        // Momento del último intento de cargar resultados al terminar la cuenta regresiva.
        ultimoIntentoPublicacion: 0
    };

    // Color fijo de cada lista según su número de orden (no cambia aunque cambie su posición).
    const colors = ["#1d4ed8", "#047857", "#b45309", "#7c3aed", "#be123c", "#0891b2", "#4d7c0f", "#c2410c"];

    const $ = id => document.getElementById(id);

    /* ---------- Pestañas ---------- */
    const tabs = Array.from(document.querySelectorAll(".tab"));

    function activarPestana(button) {
        tabs.forEach(item => {
            const activa = item === button;
            item.classList.toggle("is-active", activa);
            item.setAttribute("aria-selected", String(activa));
            item.tabIndex = activa ? 0 : -1;
            $(item.dataset.tab).classList.toggle("hidden", !activa);
        });
    }

    tabs.forEach((button, index) => {
        button.addEventListener("click", () => activarPestana(button));
        button.addEventListener("keydown", event => {
            if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") {
                return;
            }
            const siguiente = tabs[(index + (event.key === "ArrowRight" ? 1 : tabs.length - 1)) % tabs.length];
            siguiente.focus();
            activarPestana(siguiente);
        });
    });

    /* ---------- Eventos ---------- */
    $("refreshButton").addEventListener("click", loadResults);
    $("clearFiltersButton").addEventListener("click", () => {
        state.filters = {canton: "", parroquia: "", recinto: ""};
        state.tableSearch = "";
        state.currentPage = 1;
        $("tableSearch").value = "";
        renderScope();
    });
    $("tableSearch").addEventListener("input", event => {
        state.tableSearch = event.target.value;
        state.currentPage = 1;
        renderTables();
    });
    $("prevPageButton").addEventListener("click", () => {
        state.currentPage = Math.max(state.currentPage - 1, 1);
        renderTables();
    });
    $("nextPageButton").addEventListener("click", () => {
        state.currentPage = Math.min(state.currentPage + 1, getTotalPages());
        renderTables();
    });
    $("cantonFilter").addEventListener("change", event => {
        state.filters = {canton: event.target.value, parroquia: "", recinto: ""};
        state.currentPage = 1;
        renderScope();
    });
    $("parroquiaFilter").addEventListener("change", event => {
        state.filters.parroquia = event.target.value;
        state.filters.recinto = "";
        state.currentPage = 1;
        renderScope();
    });
    $("recintoFilter").addEventListener("change", event => {
        state.filters.recinto = event.target.value;
        state.currentPage = 1;
        renderScope();
    });

    /* ---------- Carga de datos ---------- */
    /** @param {boolean} [revalidar] consulta al servidor sin usar la copia en caché del navegador. */
    async function loadResults(revalidar) {
        if (window.location.protocol === "file:") {
            $("runtimeWarning").style.display = "block";
            $("processName").textContent = "Abrir desde el servidor";
            $("updatedAt").textContent = "--";
            return;
        }
        if (state.loading) {
            return;
        }
        setLoading(true);
        try {
            const response = await fetch(buildResultsUrl(), {cache: revalidar === true ? "no-cache" : "default"});
            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }
            const fechaServidor = Date.parse(response.headers.get("Date") || "");
            if (!Number.isNaN(fechaServidor)) {
                state.offsetMs = fechaServidor - Date.now();
            }
            state.data = await response.json();
            state.nextRefreshAt = Date.now() + REFRESH_MS;
            renderAll();
        } catch (error) {
            console.error("No se pudo cargar resultados públicos", error);
            setStatus(false, "Sin conexión");
            $("refreshMeta").textContent = "No se pudo actualizar";
        } finally {
            setLoading(false);
        }
    }

    function buildResultsUrl() {
        const configuredUrl = readConfiguredResultsUrl();
        return configuredUrl || new URL("resultados.json", window.location.href).toString();
    }

    function readConfiguredResultsUrl() {
        const meta = document.querySelector('meta[name="resultados-api-url"]');
        if (meta && meta.content) {
            return meta.content.trim();
        }
        if (typeof window.RESULTADOS_API_URL === "string" && window.RESULTADOS_API_URL.trim()) {
            return window.RESULTADOS_API_URL.trim();
        }
        return "";
    }

    /* ---------- Modelo ---------- */

    /** Listas del proceso en su orden oficial, con color fijo y nombre para mostrar. */
    function getListas() {
        return ((state.data && state.data.results) || []).map((item, index) => ({
            id: item.id,
            color: colors[index % colors.length],
            titulo: item.number ? "Lista " + item.number : (item.name || ""),
            nombre: item.number ? (item.listName || "") : "",
            slogan: item.slogan || ""
        }));
    }

    function hayFiltroZona() {
        return Boolean(state.filters.canton || state.filters.parroquia || state.filters.recinto);
    }

    function mesasDeLaZona() {
        return ((state.data && state.data.tables) || []).filter(item =>
            match(state.filters.canton, item.canton) &&
            match(state.filters.parroquia, item.parroquia) &&
            match(state.filters.recinto, item.recinto));
    }

    /**
     * Votos de la zona elegida. Sin filtro se usan los totales oficiales del servidor; con
     * filtro se suman los votos de las mesas validadas de esa zona.
     */
    function calcularResumen() {
        const data = state.data;
        const listas = getListas();
        const votos = new Map(listas.map(lista => [lista.id, 0]));
        let blancos = 0;
        let nulos = 0;
        let mesas = 0;
        if (!hayFiltroZona()) {
            (data.results || []).forEach(item => votos.set(item.id, Number(item.votes) || 0));
            blancos = Number(data.summary.blankVotes) || 0;
            nulos = Number(data.summary.nullVotes) || 0;
            mesas = Number(data.summary.mesasCerradas) || 0;
        } else {
            mesasDeLaZona().forEach(mesa => {
                mesas++;
                blancos += Number(mesa.blankVotes) || 0;
                nulos += Number(mesa.nullVotes) || 0;
                Object.keys(mesa.listVotes || {}).forEach(id => {
                    const clave = Number(id);
                    if (votos.has(clave)) {
                        votos.set(clave, votos.get(clave) + (Number(mesa.listVotes[id]) || 0));
                    }
                });
            });
        }
        const validos = Array.from(votos.values()).reduce((a, b) => a + b, 0);
        const ranking = listas
            .map((lista, orden) => ({...lista, orden, votos: votos.get(lista.id) || 0}))
            .map(item => ({...item, porcentaje: validos > 0 ? item.votos * 100 / validos : 0}))
            .sort((a, b) => b.votos - a.votos || a.orden - b.orden);
        return {ranking, validos, blancos, nulos, total: validos + blancos + nulos, mesas};
    }

    /* ---------- Presentación ---------- */
    /* ---------- Publicación: cuenta regresiva hasta el cierre de la votación ---------- */

    /** Un servidor anterior, sin el bloque publication, publica siempre. */
    function publicacionDisponible() {
        const pub = state.data && state.data.publication;
        return !pub || pub.available === true;
    }

    function ahoraServidor() {
        return Date.now() + state.offsetMs;
    }

    function renderCountdown() {
        const pub = state.data.publication || {};
        const inicio = pub.votingStart ? Date.parse(pub.votingStart) : NaN;
        const fin = pub.votingEnd ? Date.parse(pub.votingEnd) : NaN;
        const programada = !Number.isNaN(fin);
        $("countdownClock").classList.toggle("hidden", !programada);
        $("countdownSchedule").classList.toggle("hidden", !programada);
        $("countdownPending").classList.toggle("hidden", programada);
        if (programada) {
            $("countdownSchedule").innerHTML = textoHorario(inicio, fin);
        }
        tickCountdown();
    }

    function textoHorario(inicio, fin) {
        const dia = fecha => new Date(fecha).toLocaleDateString("es-EC",
            {weekday: "long", day: "numeric", month: "long", year: "numeric"});
        const hora = fecha => new Date(fecha).toLocaleTimeString("es-EC", {hour: "2-digit", minute: "2-digit", hour12: false});
        if (Number.isNaN(inicio)) {
            return `La votación termina el <strong>${dia(fin)}</strong> a las <strong>${hora(fin)}</strong>.`;
        }
        if (dia(inicio) === dia(fin)) {
            return `Votación: <strong>${dia(inicio)}</strong>, de <strong>${hora(inicio)}</strong> a <strong>${hora(fin)}</strong>. `
                + "Los resultados se mostrarán aquí al terminar la votación.";
        }
        return `Votación: del <strong>${dia(inicio)}, ${hora(inicio)}</strong> al <strong>${dia(fin)}, ${hora(fin)}</strong>. `
            + "Los resultados se mostrarán aquí al terminar la votación.";
    }

    /** Actualiza días, horas, minutos y segundos; al llegar a cero pide los resultados al servidor. */
    function tickCountdown() {
        if (!state.data || publicacionDisponible()) {
            return;
        }
        const pub = state.data.publication || {};
        const fin = pub.votingEnd ? Date.parse(pub.votingEnd) : NaN;
        if (Number.isNaN(fin)) {
            return;
        }
        const inicio = pub.votingStart ? Date.parse(pub.votingStart) : NaN;
        const ahora = ahoraServidor();
        const enCurso = !Number.isNaN(inicio) && ahora >= inicio && ahora < fin;
        $("countdownBadge").textContent = enCurso ? "Votación en curso" : (ahora >= fin ? "Publicando resultados" : "Próximamente");
        $("countdownBadge").classList.toggle("is-live", enCurso);
        setStatus(false, enCurso ? "Votación en curso" : "Próximamente");

        const restante = Math.max(fin - ahora, 0);
        const segundosTotales = Math.floor(restante / 1000);
        $("cdDias").textContent = String(Math.floor(segundosTotales / 86400));
        $("cdHoras").textContent = dosDigitos(Math.floor(segundosTotales / 3600) % 24);
        $("cdMinutos").textContent = dosDigitos(Math.floor(segundosTotales / 60) % 60);
        $("cdSegundos").textContent = dosDigitos(segundosTotales % 60);

        // Terminó la votación: se consulta al servidor, que decide la publicación (reintento cada 5 s).
        if (restante === 0 && Date.now() - state.ultimoIntentoPublicacion > 5000) {
            state.ultimoIntentoPublicacion = Date.now();
            loadResults(true);
        }
    }

    /** La votación tiene hora de cierre y todavía no llega: no se actualiza nada hasta ese momento. */
    function enCuentaRegresiva() {
        if (!state.data || publicacionDisponible()) {
            return false;
        }
        const pub = state.data.publication || {};
        const fin = pub.votingEnd ? Date.parse(pub.votingEnd) : NaN;
        return !Number.isNaN(fin) && fin - ahoraServidor() > 0;
    }

    function dosDigitos(valor) {
        return String(valor).padStart(2, "0");
    }

    function renderAll() {
        const data = state.data;
        const summary = data.summary || {};
        $("processName").textContent = data.hasActiveProcess ? data.process.name : "Sin proceso activo";
        $("updatedAt").textContent = formatDate(data.generatedAt);
        const disponible = publicacionDisponible();
        $("countdownSection").classList.toggle("hidden", disponible || !data.hasActiveProcess);
        $("resultsContent").classList.toggle("hidden", !disponible);
        if (!data.hasActiveProcess) {
            setStatus(false, "Sin proceso activo");
        } else if (!disponible) {
            renderCountdown();
            return;
        } else {
            setStatus(true, "Publicado");
        }
        $("totalMesas").textContent = formatNumber(summary.totalMesas);
        $("mesasCerradas").textContent = formatNumber(summary.mesasCerradas);
        $("mesasPendientes").textContent = formatNumber(summary.mesasPendientes);
        const avance = Math.min(summary.porcentajeMesasCerradasEntero || 0, 100);
        $("avanceTexto").textContent = formatPercent(summary.porcentajeMesasCerradas);
        $("avanceBar").style.width = avance + "%";
        $("avanceProgress").setAttribute("aria-valuenow", String(avance));
        renderTableHead();
        renderScope();
    }

    /** Todo lo que depende de la zona elegida: filtros, ganador, ranking y tabla. */
    function renderScope() {
        if (!state.data) {
            return;
        }
        renderFilters();
        const resumen = calcularResumen();
        renderLeader(resumen);
        renderRanking(resumen);
        renderVotes(resumen);
        renderTables();
    }

    function setStatus(ok, texto) {
        $("statusPill").className = ok ? "status-pill" : "status-pill is-warning";
        $("statusPill").textContent = texto;
    }

    function textoZona() {
        const partes = [state.filters.canton, state.filters.parroquia, state.filters.recinto].filter(Boolean);
        return partes.length ? "En " + partes.join(" · ") : "En todo el proceso";
    }

    function renderLeader(resumen) {
        const summary = state.data.summary || {};
        const final = Number(summary.porcentajeMesasCerradas) >= 100 && !hayFiltroZona();
        const [primero, segundo] = resumen.ranking;
        const hayVotos = primero && primero.votos > 0;
        const empate = hayVotos && segundo && segundo.votos === primero.votos;
        const card = $("leaderCard");

        $("leaderScope").textContent = textoZona();
        $("leaderBody").classList.toggle("hidden", !hayVotos);
        $("leaderLead").classList.toggle("hidden", !hayVotos);
        $("leaderEmpty").classList.toggle("hidden", Boolean(hayVotos));

        const status = $("leaderStatus");
        status.classList.toggle("is-final", final);
        status.textContent = final
            ? "Resultado con el 100 % de las mesas validadas"
            : "Resultado parcial · " + formatPercent(summary.porcentajeMesasCerradas) + " de las mesas contadas y validadas";

        if (!hayVotos) {
            $("leaderLabel").textContent = "¿Quién va ganando?";
            card.style.removeProperty("--leader-color");
            return;
        }
        if (empate) {
            const empatadas = resumen.ranking.filter(item => item.votos === primero.votos);
            $("leaderLabel").textContent = "Empate";
            $("leaderName").textContent = empatadas.map(item => item.titulo).join(" y ");
            $("leaderListName").textContent = empatadas.map(item => item.nombre).filter(Boolean).join(" · ");
            $("leaderSlogan").textContent = "";
            $("leaderLead").textContent = `${empatadas.length} listas tienen la misma cantidad de votos.`;
            card.style.removeProperty("--leader-color");
        } else {
            $("leaderLabel").textContent = final ? "Lista más votada" : "Va ganando";
            $("leaderName").textContent = primero.titulo;
            $("leaderListName").textContent = primero.nombre;
            $("leaderSlogan").textContent = primero.slogan;
            card.style.setProperty("--leader-color", primero.color);
            if (segundo) {
                const diferencia = primero.votos - segundo.votos;
                const puntos = primero.porcentaje - segundo.porcentaje;
                $("leaderLead").innerHTML = `Supera a la <strong>${escapeHtml(segundo.titulo)}</strong> por `
                    + `<strong>${formatNumber(diferencia)}</strong> ${diferencia === 1 ? "voto" : "votos"} `
                    + `(${formatDecimal(puntos)} puntos).`;
            } else {
                $("leaderLead").textContent = "Es la única lista con votos.";
            }
        }
        $("leaderPercent").textContent = formatPercent(primero.porcentaje);
        $("leaderVotes").textContent = formatNumber(primero.votos);
    }

    function renderRanking(resumen) {
        const hayVotos = resumen.ranking.some(item => item.votos > 0);
        const list = $("resultsList");
        list.innerHTML = "";
        list.classList.toggle("hidden", !hayVotos);
        $("emptyResults").classList.toggle("hidden", hayVotos);
        $("rankingSubtitle").textContent = (hayFiltroZona() ? textoZona() + ". " : "")
            + "De la lista más votada a la menos votada. El porcentaje se calcula sobre los votos válidos.";
        if (!hayVotos) {
            return;
        }
        const maximo = resumen.ranking[0].votos;
        resumen.ranking.forEach((item, posicion) => {
            const ancho = maximo > 0 ? item.votos * 100 / maximo : 0;
            const row = document.createElement("li");
            row.className = "rank-item" + (posicion === 0 && item.votos > 0 && item.votos !== (resumen.ranking[1] || {}).votos ? " is-first" : "");
            row.style.setProperty("--rank-color", item.color);
            row.innerHTML = `
                <span class="rank-position" aria-label="Posición ${posicion + 1}">${posicion + 1}</span>
                <span class="rank-name">${escapeHtml(item.titulo)}${item.nombre ? ` <span>· ${escapeHtml(item.nombre)}</span>` : ""}</span>
                <span class="rank-figures"><strong>${formatNumber(item.votos)}</strong><span>${formatPercent(item.porcentaje)}</span></span>
                <span class="progress rank-bar" aria-hidden="true"><span class="progress-bar" style="width:${ancho}%"></span></span>
            `;
            list.appendChild(row);
        });
    }

    function renderVotes(resumen) {
        $("validVotes").textContent = formatNumber(resumen.validos);
        $("blankVotes").textContent = formatNumber(resumen.blancos);
        $("nullVotes").textContent = formatNumber(resumen.nulos);
        $("totalVotes").textContent = formatNumber(resumen.total);
    }

    function renderFilters() {
        const tables = state.data.tables || [];
        fillSelect("cantonFilter", uniqueValues(tables, "canton"), "Todos", state.filters.canton);
        const parroquias = uniqueValues(tables.filter(item => match(state.filters.canton, item.canton)), "parroquia");
        fillSelect("parroquiaFilter", parroquias, "Todas", state.filters.parroquia);
        const recintos = uniqueValues(tables.filter(item =>
            match(state.filters.canton, item.canton) && match(state.filters.parroquia, item.parroquia)
        ), "recinto");
        fillSelect("recintoFilter", recintos, "Todos", state.filters.recinto);
    }

    /** Columnas: ubicación, una por lista (en su orden oficial), blancos, nulos y validación. */
    function getColumnas() {
        const listas = getListas();
        return [
            {titulo: "Cantón", valor: item => escapeHtml(item.canton)},
            {titulo: "Parroquia", valor: item => escapeHtml(item.parroquia)},
            {titulo: "Recinto", valor: item => escapeHtml(item.recinto)},
            {titulo: "Mesa", valor: item => escapeHtml(item.mesa)},
            ...listas.map(lista => ({
                titulo: lista.titulo,
                ayuda: lista.nombre,
                lista,
                numero: true
            })),
            {titulo: "En blanco", valor: item => formatNumber(item.blankVotes), numero: true, gris: true},
            {titulo: "Nulos", valor: item => formatNumber(item.nullVotes), numero: true, gris: true},
            {titulo: "Validación", valor: item => formatDate(item.validatedAt), gris: true}
        ];
    }

    function renderTableHead() {
        const cabecera = getColumnas().map(col => {
            const clases = [col.numero ? "number" : "", col.lista ? "list-col" : ""].filter(Boolean).join(" ");
            const estilo = col.lista ? ` style="--rank-color:${col.lista.color}"` : "";
            const ayuda = col.ayuda ? ` title="${escapeHtml(col.ayuda)}"` : "";
            return `<th scope="col"${clases ? ` class="${clases}"` : ""}${estilo}${ayuda}>${escapeHtml(col.titulo)}</th>`;
        }).join("");
        $("tablesHead").innerHTML = `<tr>${cabecera}</tr>`;
    }

    function renderTables() {
        const columnas = getColumnas();
        const filtered = getFilteredTables();
        const totalPages = getTotalPages(filtered);
        state.currentPage = Math.min(Math.max(state.currentPage, 1), totalPages);
        const start = (state.currentPage - 1) * state.rowsPerPage;
        const visible = filtered.slice(start, start + state.rowsPerPage);
        const body = $("tablesBody");
        body.innerHTML = "";
        visible.forEach(item => {
            const votos = item.listVotes || {};
            const maximo = Math.max(0, ...columnas.filter(col => col.lista).map(col => Number(votos[col.lista.id]) || 0));
            const row = document.createElement("tr");
            row.innerHTML = columnas.map(col => {
                if (col.lista) {
                    const valor = Number(votos[col.lista.id]) || 0;
                    const top = maximo > 0 && valor === maximo;
                    const etiqueta = col.titulo + (col.ayuda ? " · " + col.ayuda : "");
                    return `<td data-label="${escapeHtml(etiqueta)}" class="number${top ? " is-top" : ""}"`
                        + ` style="--rank-color:${col.lista.color}">${formatNumber(valor)}</td>`;
                }
                const clases = [col.numero ? "number" : "", col.gris ? "muted" : ""].filter(Boolean).join(" ");
                return `<td data-label="${escapeHtml(col.titulo)}"${clases ? ` class="${clases}"` : ""}>${col.valor(item)}</td>`;
            }).join("");
            body.appendChild(row);
        });
        $("emptyTables").classList.toggle("hidden", filtered.length > 0);
        document.querySelector("#tablesPanel .table-wrap").classList.toggle("hidden", filtered.length === 0);
        $("tableCount").textContent = formatNumber(filtered.length);
        $("pageInfo").textContent = `Página ${state.currentPage} de ${totalPages}`;
        $("prevPageButton").disabled = state.currentPage <= 1;
        $("nextPageButton").disabled = state.currentPage >= totalPages;
    }

    /* ---------- Utilidades ---------- */
    function getFilteredTables() {
        const search = normalize(state.tableSearch);
        return mesasDeLaZona().filter(item => !search
            || [item.canton, item.parroquia, item.recinto, item.mesa].some(value => normalize(value).includes(search)));
    }

    function getTotalPages(items) {
        const total = items ? items.length : getFilteredTables().length;
        return Math.max(Math.ceil(total / state.rowsPerPage), 1);
    }

    function setLoading(loading) {
        state.loading = loading;
        $("loaderDot").classList.toggle("loading", loading);
        $("refreshButton").disabled = loading;
        if (loading) {
            $("refreshMeta").textContent = "Actualizando…";
        } else if (state.nextRefreshAt) {
            updateRefreshMeta();
        }
    }

    function updateRefreshMeta() {
        if (!state.nextRefreshAt || state.loading) {
            return;
        }
        if (enCuentaRegresiva()) {
            $("refreshMeta").textContent = "Se actualizará al terminar la votación";
            return;
        }
        const remaining = Math.max(Math.ceil((state.nextRefreshAt - Date.now()) / 1000), 0);
        $("refreshMeta").textContent = `Próxima actualización en ${remaining} s`;
    }

    function fillSelect(id, values, placeholder, selected) {
        const select = $(id);
        select.innerHTML = "";
        select.appendChild(new Option(placeholder, ""));
        values.forEach(value => select.appendChild(new Option(value, value)));
        select.value = selected || "";
    }

    function uniqueValues(items, key) {
        return [...new Set(items.map(item => item[key]).filter(Boolean))].sort((a, b) => a.localeCompare(b, "es"));
    }

    function match(filter, value) {
        return !filter || filter === value;
    }

    function normalize(value) {
        return String(value || "")
            .normalize("NFD")
            .replace(/[̀-ͯ]/g, "")
            .toLowerCase()
            .trim();
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString("es-EC");
    }

    function formatDecimal(value) {
        return Number(value || 0).toLocaleString("es-EC", {minimumFractionDigits: 2, maximumFractionDigits: 2});
    }

    function formatPercent(value) {
        return formatDecimal(value) + " %";
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
    setInterval(() => {
        updateRefreshMeta();
        tickCountdown();
    }, 1000);
    // Mientras corre la cuenta regresiva no se consulta al servidor: la única consulta ocurre al llegar a cero.
    setInterval(() => {
        if (!enCuentaRegresiva()) {
            loadResults();
        }
    }, REFRESH_MS);
})();
