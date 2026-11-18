import { Route, Routes } from 'react-router-dom'

function App() {
  return (
    <main className="app-shell">
      <Routes>
        <Route
          path="/"
          element={
            <section className="welcome-card" aria-labelledby="welcome-title">
              <p className="eyebrow">Job Tracker</p>
              <h1 id="welcome-title">Organize your job search</h1>
              <p className="welcome-text">
                Track applications, recruitment stages, contacts and documents.
              </p>
            </section>
          }
        />
        <Route path="*" element={<h1>Page not found</h1>} />
      </Routes>
    </main>
  )
}

export default App
