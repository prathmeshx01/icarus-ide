package com.example.data

object ProjectTemplates {

    val DEFAULT_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Icarus Project</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div id="app">
    <h1>Hello, Icarus!</h1>
    <p>Start editing this file to build your application.</p>
  </div>
  <script src="script.js"></script>
</body>
</html>
"""

    val DEFAULT_CSS = """* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  background-color: #0d1117;
  color: #e6edf3;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

#app {
  text-align: center;
  padding: 2rem;
}

h1 {
  color: #58a6ff;
  margin-bottom: 0.5rem;
}

p {
  color: #8b949e;
}
"""

    val DEFAULT_JS = """// Icarus JavaScript Entry
console.log("Icarus workspace loaded successfully.");

document.addEventListener("DOMContentLoaded", () => {
  // Your code here
});
"""

    val REACT_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>React 18 Starter</title>
  <script src="https://unpkg.com/react@18/umd/react.development.js" crossorigin></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js" crossorigin></script>
  <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div id="root"></div>
  <script type="text/babel" src="app.jsx"></script>
</body>
</html>
"""

    val REACT_JSX = """function App() {
  const [count, setCount] = React.useState(0);

  return (
    <div className="card">
      <h1>React 18 Component</h1>
      <p>Count is: <strong>{count}</strong></p>
      <button onClick={() => setCount(count + 1)}>Increment</button>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
"""

    val VUE_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Vue 3 Starter</title>
  <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div id="app">
    <h1>{{ message }}</h1>
    <button @click="count++">Count: {{ count }}</button>
  </div>
  <script src="app.js"></script>
</body>
</html>
"""

    val VUE_JS = """const { createApp, ref } = Vue;

createApp({
  setup() {
    const message = ref('Hello from Vue 3!');
    const count = ref(0);
    return { message, count };
  }
}).mount('#app');
"""

    val TAILWIND_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Tailwind CSS Starter</title>
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-slate-900 text-slate-100 min-h-screen flex items-center justify-center p-6">
  <div class="max-w-md w-full bg-slate-800 rounded-xl p-8 border border-slate-700 shadow-2xl text-center">
    <h1 class="text-2xl font-bold text-sky-400 mb-2">Tailwind CSS Project</h1>
    <p class="text-slate-400 text-sm mb-6">Utility-first styling ready for development.</p>
    <button class="bg-sky-500 hover:bg-sky-600 text-white font-semibold px-4 py-2 rounded-lg transition-all">
      Action Button
    </button>
  </div>
</body>
</html>
"""

    val CANVAS_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Canvas Project</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { background: #0b0c10; color: #fff; overflow: hidden; font-family: sans-serif; }
    canvas { display: block; width: 100vw; height: 100vh; }
  </style>
</head>
<body>
  <canvas id="canvas"></canvas>
  <script src="script.js"></script>
</body>
</html>
"""

    val CANVAS_JS = """const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");

canvas.width = window.innerWidth;
canvas.height = window.innerHeight;

ctx.fillStyle = "#388bfd";
ctx.fillRect(window.innerWidth / 2 - 50, window.innerHeight / 2 - 50, 100, 100);

console.log("Canvas initialized.");
"""

    val PORTFOLIO_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Portfolio</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header>
    <h1>My Portfolio</h1>
  </header>
  <main>
    <p>Welcome to my developer portfolio.</p>
  </main>
  <script src="script.js"></script>
</body>
</html>
"""

    val PORTFOLIO_CSS = """body {
  background-color: #0c0d12;
  color: #f3f4f6;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  padding: 2rem;
  line-height: 1.5;
}

h1 {
  color: #60a5fa;
  margin-bottom: 0.5rem;
}

p {
  color: #9ca3af;
}
"""

    val PORTFOLIO_JS = """console.log("Portfolio workspace ready.");
"""

    val KOTLIN_CODE = """package com.example

fun main() {
    println("Hello from Icarus Kotlin!")
}
"""

    val PYTHON_CODE = """# Icarus Python Entry
def main():
    print("Hello from Icarus Python!")

if __name__ == "__main__":
    main()
"""

    val README_MD = """# Icarus Project

Clean developer workspace generated with Icarus IDE.

## Files
- `index.html`: Entry structure
- `style.css`: Stylesheet
- `script.js`: Interactive logic
"""

    val BLANK_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Scratchpad</title>
</head>
<body>
  <!-- Start typing HTML here -->
</body>
</html>
"""
}
