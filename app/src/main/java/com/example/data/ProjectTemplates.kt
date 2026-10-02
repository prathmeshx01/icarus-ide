package com.example.data

object ProjectTemplates {

    val DEFAULT_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ICARUS Project</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="container">
    <header class="hero">
      <div class="status-pill">
        <span class="dot"></span> Web Application
      </div>
      <h1>Welcome to <span>ICARUS</span></h1>
      <p class="subtitle">Focused code workspace with real-time execution.</p>
    </header>

    <main class="card">
      <h2>Interactive Demo</h2>
      <p class="description">Test JavaScript state, event listeners, and DOM updates.</p>
      
      <div class="counter-box">
        <div class="counter-display" id="counter">0</div>
        <div class="button-group">
          <button id="btn-decrement" class="btn btn-secondary">-</button>
          <button id="btn-reset" class="btn btn-outline">Reset</button>
          <button id="btn-increment" class="btn btn-primary">+</button>
        </div>
      </div>

      <div class="log-box" id="log-output">
        Status: JavaScript initialized. Tap buttons above!
      </div>
    </main>

    <footer>
      <p>Built with ICARUS</p>
    </footer>
  </div>

  <script src="script.js"></script>
</body>
</html>
"""

    val DEFAULT_CSS = """/* ICARUS Stylesheet */
:root {
  --bg-color: #0f172a;
  --card-bg: #1e293b;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --primary: #f59e0b;
  --primary-hover: #d97706;
  --accent: #06b6d4;
  --border: #334155;
  --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  background-color: var(--bg-color);
  color: var(--text-main);
  font-family: var(--font-family);
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
}

.container {
  width: 100%;
  max-width: 460px;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.hero {
  text-align: center;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background-color: rgba(6, 182, 212, 0.15);
  color: var(--accent);
  padding: 0.35rem 0.85rem;
  border-radius: 9999px;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 1rem;
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 8px #10b981;
}

h1 {
  font-size: 1.85rem;
  font-weight: 800;
  letter-spacing: -0.025em;
  margin-bottom: 0.5rem;
}

h1 span {
  color: var(--primary);
}

.subtitle {
  color: var(--text-muted);
  font-size: 0.95rem;
  line-height: 1.5;
}

.card {
  background-color: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
}

.card h2 {
  font-size: 1.15rem;
  margin-bottom: 0.5rem;
}

.card .description {
  color: var(--text-muted);
  font-size: 0.85rem;
  margin-bottom: 1.25rem;
}

.counter-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  background-color: rgba(15, 23, 42, 0.6);
  padding: 1.25rem;
  border-radius: 0.75rem;
  border: 1px solid var(--border);
}

.counter-display {
  font-size: 3rem;
  font-weight: 800;
  color: var(--primary);
}

.button-group {
  display: flex;
  gap: 0.75rem;
  width: 100%;
}

.btn {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1.1rem;
  font-weight: 700;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  transition: transform 0.1s ease, background-color 0.15s ease;
}

.btn:active {
  transform: scale(0.96);
}

.btn-primary {
  background-color: var(--primary);
  color: #0f172a;
}

.btn-secondary {
  background-color: #3b82f6;
  color: white;
}

.btn-outline {
  background-color: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border);
}

.log-box {
  margin-top: 1rem;
  font-family: monospace;
  font-size: 0.8rem;
  color: #a5f3fc;
  background-color: rgba(0, 0, 0, 0.4);
  padding: 0.75rem;
  border-radius: 0.5rem;
  word-break: break-word;
}

footer {
  text-align: center;
  font-size: 0.75rem;
  color: var(--text-muted);
}
"""

    val DEFAULT_JS = """// ICARUS JavaScript Engine
console.log("[ICARUS] script.js initialized!");

let count = 0;
const counterEl = document.getElementById("counter");
const logEl = document.getElementById("log-output");

function updateDisplay(action) {
  counterEl.textContent = count;
  const timeStr = new Date().toLocaleTimeString();
  const msg = "[" + timeStr + "] " + action + " -> " + count;
  logEl.textContent = msg;
  console.log("[State]", msg);
}

document.getElementById("btn-increment").addEventListener("click", function() {
  count++;
  updateDisplay("Incremented");
});

document.getElementById("btn-decrement").addEventListener("click", function() {
  count--;
  updateDisplay("Decremented");
});

document.getElementById("btn-reset").addEventListener("click", function() {
  count = 0;
  updateDisplay("Reset");
});
"""

    // Template 2: Canvas Particle Game
    val CANVAS_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Canvas Particle Physics</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { background: #0b0c10; color: #fff; font-family: sans-serif; overflow: hidden; }
    canvas { display: block; width: 100vw; height: 100vh; }
    .hud { position: absolute; top: 16px; left: 16px; pointer-events: none; }
    .hud h1 { font-size: 1.1rem; color: #66fcf1; }
    .hud p { font-size: 0.8rem; color: #c5c6c7; margin-top: 4px; }
  </style>
</head>
<body>
  <div class="hud">
    <h1>Particle Wave Simulation</h1>
    <p>Touch or drag on screen to interact with particles</p>
  </div>
  <canvas id="canvas"></canvas>
  <script src="script.js"></script>
</body>
</html>
"""

    val CANVAS_JS = """const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");

let width = canvas.width = window.innerWidth;
let height = canvas.height = window.innerHeight;

window.addEventListener("resize", () => {
  width = canvas.width = window.innerWidth;
  height = canvas.height = window.innerHeight;
});

const particles = [];
const PARTICLE_COUNT = 60;

class Particle {
  constructor() {
    this.reset();
  }
  reset() {
    this.x = Math.random() * width;
    this.y = Math.random() * height;
    this.vx = (Math.random() - 0.5) * 2;
    this.vy = (Math.random() - 0.5) * 2;
    this.radius = Math.random() * 3 + 2;
    this.color = "#45f3ff";
  }
  update(pointer) {
    this.x += this.vx;
    this.y += this.vy;

    if (pointer.active) {
      const dx = pointer.x - this.x;
      const dy = pointer.y - this.y;
      const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < 120) {
        this.x -= dx * 0.05;
        this.y -= dy * 0.05;
      }
    }

    if (this.x < 0 || this.x > width) this.vx *= -1;
    if (this.y < 0 || this.y > height) this.vy *= -1;
  }
  draw() {
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
    ctx.fillStyle = this.color;
    ctx.fill();
  }
}

for (let i = 0; i < PARTICLE_COUNT; i++) {
  particles.push(new Particle());
}

const pointer = { x: 0, y: 0, active: false };

window.addEventListener("pointerdown", (e) => { pointer.x = e.clientX; pointer.y = e.clientY; pointer.active = true; });
window.addEventListener("pointermove", (e) => { pointer.x = e.clientX; pointer.y = e.clientY; });
window.addEventListener("pointerup", () => { pointer.active = false; });

function animate() {
  ctx.fillStyle = "rgba(11, 12, 16, 0.2)";
  ctx.fillRect(0, 0, width, height);

  for (let i = 0; i < particles.length; i++) {
    particles[i].update(pointer);
    particles[i].draw();

    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x;
      const dy = particles[i].y - particles[j].y;
      const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < 80) {
        ctx.strokeStyle = "rgba(102, 252, 241, " + (1 - dist / 80) * 0.5 + ")";
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(particles[i].x, particles[i].y);
        ctx.lineTo(particles[j].x, particles[j].y);
        ctx.stroke();
      }
    }
  }

  requestAnimationFrame(animate);
}

animate();
console.log("Canvas particle loop started.");
"""

    // Template 3: Portfolio / Landing Page
    val PORTFOLIO_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Developer Portfolio</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="wrap">
    <header class="navbar">
      <div class="brand">alex.dev</div>
      <button id="theme-btn" class="pill">Toggle Glow</button>
    </header>

    <section class="intro">
      <div class="tag">SOFTWARE ENGINEER</div>
      <h1>Building tools for <span class="highlight">creative minds</span>.</h1>
      <p class="bio">I craft responsive apps, creative graphics, and minimal software.</p>
    </section>

    <section class="grid">
      <div class="card">
        <h3>Project Alpha</h3>
        <p>Real-time distributed data visualizer.</p>
        <div class="card-footer"><span>TypeScript</span> &bull; <span>Web Audio</span></div>
      </div>
      <div class="card">
        <h3>Vector Shader</h3>
        <p>WebGL compute shader renderer.</p>
        <div class="card-footer"><span>GLSL</span> &bull; <span>Canvas</span></div>
      </div>
    </section>
  </div>
  <script src="script.js"></script>
</body>
</html>
"""

    val PORTFOLIO_CSS = """* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  background-color: #0c0d12;
  color: #e5e7eb;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  padding: 1.5rem;
  line-height: 1.6;
}
.wrap { max-width: 520px; margin: 0 auto; display: flex; flex-direction: column; gap: 2rem; }
.navbar { display: flex; justify-content: space-between; align-items: center; }
.brand { font-size: 1.1rem; font-weight: 800; font-family: monospace; color: #60a5fa; }
.pill { background: #1f2937; color: #fff; border: 1px solid #374151; padding: 0.35rem 0.8rem; border-radius: 9999px; font-size: 0.8rem; cursor: pointer; }
.tag { font-size: 0.75rem; letter-spacing: 0.1em; color: #a78bfa; font-weight: 700; margin-bottom: 0.5rem; }
h1 { font-size: 1.8rem; font-weight: 800; line-height: 1.2; }
.highlight { color: #38bdf8; }
.bio { color: #9ca3af; font-size: 0.95rem; margin-top: 0.5rem; }
.grid { display: grid; gap: 1rem; }
.card { background: #13151f; border: 1px solid #242938; padding: 1.25rem; border-radius: 0.75rem; }
.card h3 { font-size: 1.05rem; margin-bottom: 0.25rem; color: #f9fafb; }
.card p { color: #9ca3af; font-size: 0.85rem; }
.card-footer { margin-top: 0.75rem; font-size: 0.75rem; color: #60a5fa; font-family: monospace; }
"""

    val PORTFOLIO_JS = """const btn = document.getElementById("theme-btn");
btn.addEventListener("click", () => {
  document.body.classList.toggle("glow-active");
  console.log("Toggled portfolio theme glow.");
});
"""

    // Template 4: Kotlin Algorithms
    val KOTLIN_CODE = """package com.example

data class Item(val id: Int, val name: String, val score: Double)

fun main() {
    println("--- ICARUS Kotlin Engine ---")
    val items = listOf(
        Item(1, "Syntax Analysis", 98.5),
        Item(2, "Real-time Linter", 94.0),
        Item(3, "Virtual File System", 99.2)
    )

    val topItems = items.filter { it.score > 95.0 }
        .sortedByDescending { it.score }

    println("Top items found:")
    topItems.forEach { println(" - ${'$'}{it.name}: ${'$'}{it.score}") }
}
"""

    val PYTHON_CODE = """# ICARUS Python Script Template
import sys
import json
from datetime import datetime

def process_data(data):
    \"\"\"Process collection of developer stats.\"\"\"
    summary = {
        "timestamp": datetime.now().isoformat(),
        "total_records": len(data),
        "status": "ready"
    }
    return summary

def main():
    print("ICARUS Python Environment")
    test_data = [{"id": i, "score": i * 10} for i in range(1, 6)]
    result = process_data(test_data)
    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main()
"""

    val BLANK_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Scratchpad</title>
  <style>
    body { background: #121316; color: #fff; font-family: monospace; padding: 2rem; }
  </style>
</head>
<body>
  <h1>Hello, World!</h1>
  <p>Start editing this file in ICARUS.</p>
</body>
</html>
"""
}
