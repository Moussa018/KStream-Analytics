import { useEffect, useRef, useState, useCallback } from "react";
import "./Analytics.css";

const PAGES = ["P1", "P2"];
const COLORS = {
  P1: { stroke: "#2563eb", fill: "rgba(37, 99, 235, 0.06)" },
  P2: { stroke: "#7c3aed", fill: "rgba(124, 58, 237, 0.06)" },
};

export default function Analytics() {
  const canvasRef = useRef(null);
  const chartRef = useRef(null);
  const seriesRef = useRef({});

  const [connected, setConnected] = useState(false);
  const [lastValues, setLastValues] = useState({ P1: 0, P2: 0 });
  const [totalEvents, setTotalEvents] = useState({ P1: 0, P2: 0 });
  const [error, setError] = useState(null);
  const [uptime, setUptime] = useState(0);        // secondes depuis connexion
  const [peakValues, setPeakValues] = useState({ P1: 0, P2: 0 });
  const uptimeRef = useRef(null);
  const connectedAt = useRef(null);

  // ── Initialisation du graphique SmoothieCharts ──────────────────────────
  const initChart = useCallback(() => {
    if (!canvasRef.current || !window.SmoothieChart) return;

    const chart = new window.SmoothieChart({
      responsive: true,
      millisPerPixel: 60,
      grid: {
        fillStyle: "#ffffff",
        strokeStyle: "#f1f5f9",
        verticalSections: 5,
        borderVisible: false,
      },
      labels: {
        fillStyle: "#64748b",
        fontSize: 11,
        fontFamily: "Inter, system-ui, sans-serif",
      },
      tooltip: true,
      tooltipFormatter: (ts, sets) => {
        return sets.map(s =>
          `<span style="color:${s.options.strokeStyle}">${s.options.label || "?"}: ${s.currentValue}</span>`
        ).join("<br>");
      },
      maxValueScale: 1.15,
      minValue: 0,
      interpolation: "bezier",
    });

    PAGES.forEach((page) => {
      const series = new window.TimeSeries();
      seriesRef.current[page] = series;
      chart.addTimeSeries(series, {
        strokeStyle: COLORS[page].stroke,
        fillStyle: COLORS[page].fill,
        lineWidth: 2,
        label: page,
      });
    });

    chart.streamTo(canvasRef.current, 1000);
    chartRef.current = chart;
  }, []);

  // ── Chargement de SmoothieCharts depuis CDN ──────────────────────────────
  useEffect(() => {
    const script = document.createElement("script");
    script.src = "https://cdnjs.cloudflare.com/ajax/libs/smoothie/1.34.0/smoothie.min.js";
    script.async = true;
    script.onload = initChart;
    document.head.appendChild(script);
    return () => {
      if (chartRef.current) chartRef.current.stop();
      document.head.removeChild(script);
    };
  }, [initChart]);

  // ── Connexion SSE ────────────────────────────────────────────────────────
  useEffect(() => {
    const evtSource = new EventSource("/analytics");

    evtSource.onmessage = (event) => {
      if (!connected) {
        connectedAt.current = Date.now();
        setConnected(true);
        setError(null);
      }

      const data = JSON.parse(event.data);
      const now = Date.now();

      setLastValues(prev => {
        const next = { ...prev };
        PAGES.forEach(page => {
          const val = data[page] ?? 0;
          next[page] = val;
          seriesRef.current[page]?.append(now, val);
        });
        return next;
      });

      // Cumul total & peak
      setTotalEvents(prev => {
        const next = { ...prev };
        PAGES.forEach(page => { next[page] += data[page] ?? 0; });
        return next;
      });
      setPeakValues(prev => {
        const next = { ...prev };
        PAGES.forEach(page => { if ((data[page] ?? 0) > prev[page]) next[page] = data[page]; });
        return next;
      });
    };

    evtSource.onerror = () => {
      setConnected(false);
      setError("Connexion perdue. Vérifiez le backend sur :8081");
      clearInterval(uptimeRef.current);
    };

    return () => evtSource.close();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Timer uptime ─────────────────────────────────────────────────────────
  useEffect(() => {
    if (connected) {
      uptimeRef.current = setInterval(() => {
        setUptime(Math.floor((Date.now() - connectedAt.current) / 1000));
      }, 1000);
    }
    return () => clearInterval(uptimeRef.current);
  }, [connected]);

  const formatUptime = (s) => {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return m > 0 ? `${m}m ${sec}s` : `${sec}s`;
  };

  return (
    <div className="dashboard-wrapper">
      {/* ── Header ── */}
      <header className="dashboard-header">
        <div className="header-left">
          <span className="badge">Kafka Streams · KRaft</span>
          <h1>Real-time Page Traffic</h1>
        </div>
        <div className="header-right">
          <div className={`status-pill ${connected ? "is-online" : "is-offline"}`}>
            <span className="dot" />
            {connected ? `Live · ${formatUptime(uptime)}` : "Offline"}
          </div>
        </div>
      </header>

      <main className="dashboard-content">
        {/* ── Stats cards ── */}
        <section className="stats-grid">
          {PAGES.map((page) => (
            <div key={page} className="stats-card">
              <div className="card-header">
                <span className="card-indicator" style={{ backgroundColor: COLORS[page].stroke }} />
                <h3>{page} — Trafic actuel</h3>
              </div>
              <div className="card-body">
                <span className="value">{lastValues[page]}</span>
                <span className="unit">req / 5s</span>
              </div>
              <div className="card-footer">
                <span className="meta">Peak: <strong>{peakValues[page]}</strong></span>
                <span className="meta">Total: <strong>{totalEvents[page]}</strong></span>
              </div>
            </div>
          ))}
        </section>

        {/* ── Graphique ── */}
        <section className="chart-container">
          <div className="chart-header">
            <h4>Vélocité du trafic · Fenêtre 60s</h4>
            <div className="legend">
              {PAGES.map(p => (
                <span key={p} className="legend-item">
                  <span className="legend-dot" style={{ background: COLORS[p].stroke }} />
                  {p}
                </span>
              ))}
            </div>
          </div>
          <div className="canvas-wrapper">
            <canvas ref={canvasRef} height={320} />
          </div>
        </section>


        {error && <div className="error-banner">{error}</div>}
      </main>
    </div>
  );
}