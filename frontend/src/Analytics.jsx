import { useEffect, useRef, useState } from "react";
import "./Analytics.css";

const PAGES = ["P1", "P2"];
const COLORS = {
  P1: { stroke: "#2563eb", fill: "rgba(37, 99, 235, 0.05)" }, // Indigo
  P2: { stroke: "#64748b", fill: "rgba(100, 116, 139, 0.05)" }, // Slate
};

export default function Analytics() {
  const canvasRef = useRef(null);
  const chartRef = useRef(null);
  const seriesRef = useRef({});
  const [connected, setConnected] = useState(false);
  const [lastValues, setLastValues] = useState({ P1: 0, P2: 0 });
  const [error, setError] = useState(null);

  useEffect(() => {
    const script = document.createElement("script");
    script.src = "https://cdnjs.cloudflare.com/ajax/libs/smoothie/1.34.0/smoothie.min.js";
    script.async = true;
    script.onload = () => {
      if (canvasRef.current) initChart();
    };
    document.head.appendChild(script);

    return () => {
      if (chartRef.current) chartRef.current.stop();
      document.head.removeChild(script);
    };
  }, []);

  const initChart = () => {
    const chart = new window.SmoothieChart({
      responsive: true,
      millisPerPixel: 50,
      grid: {
        fillStyle: "#ffffff",
        strokeStyle: "#f1f5f9", // Light gray grid
        verticalSections: 6,
        borderVisible: false,
      },
      labels: {
        fillStyle: "#64748b",
        fontSize: 12,
        fontFamily: "Inter, system-ui, sans-serif",
      },
      tooltip: true,
      maxValueScale: 1.1,
      minValue: 0,
      interpolation: 'bezier' // Smoother lines for professional look
    });

    PAGES.forEach((page) => {
      const series = new window.TimeSeries();
      seriesRef.current[page] = series;
      chart.addTimeSeries(series, {
        strokeStyle: COLORS[page].stroke,
        fillStyle: COLORS[page].fill,
        lineWidth: 2,
      });
    });

    chart.streamTo(canvasRef.current, 1000);
    chartRef.current = chart;
  };

  useEffect(() => {
    const evtSource = new EventSource("/analytics");

    evtSource.onmessage = (event) => {
      setConnected(true);
      setError(null);
      const data = JSON.parse(event.data);
      const now = new Date().getTime();

      const newVals = {};
      PAGES.forEach((page) => {
        const val = data[page] ?? 0;
        newVals[page] = val;
        if (seriesRef.current[page]) {
          seriesRef.current[page].append(now, val);
        }
      });
      setLastValues(newVals);
    };

    evtSource.onerror = () => {
      setConnected(false);
      setError("Connection lost. Verify backend status on :8081");
    };

    return () => evtSource.close();
  }, []);

  return (
    <div className="dashboard-wrapper">
      <header className="dashboard-header">
        <div className="header-left">
          <span className="badge">System Monitor</span>
          <h1>Real-time Page Traffic</h1>
        </div>
        <div className="header-right">
          <div className={`status-pill ${connected ? "is-online" : "is-offline"}`}>
            <span className="dot" />
            {connected ? "Live System" : "Offline"}
          </div>
        </div>
      </header>

      <main className="dashboard-content">
        <section className="stats-grid">
          {PAGES.map((page) => (
            <div key={page} className="stats-card">
              <div className="card-header">
                <span className="card-indicator" style={{ backgroundColor: COLORS[page].stroke }} />
                <h3>{page} Traffic</h3>
              </div>
              <div className="card-body">
                <span className="value">{lastValues[page]}</span>
                <span className="unit">req/sec</span>
              </div>
            </div>
          ))}
        </section>

        <section className="chart-container">
          <div className="chart-header">
            <h4>Traffic Velocity (Last 60s)</h4>
          </div>
          <div className="canvas-wrapper">
            <canvas ref={canvasRef} height={350} />
          </div>
        </section>

        {error && <div className="error-banner">{error}</div>}
      </main>
    </div>
  );
}