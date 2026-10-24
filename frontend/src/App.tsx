import { useCallback, useEffect, useState } from 'react';
import { analyze, getStats, listSuggestions, review } from './api';
import StatsBar from './components/StatsBar';
import SuggestionCard from './components/SuggestionCard';
import { SAMPLE_SOURCE } from './sample';
import type { Decision, Stats, Suggestion } from './types';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Une erreur inattendue est survenue';
}

export default function App() {
  const [source, setSource] = useState('');
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [stats, setStats] = useState<Stats | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    const [list, indicators] = await Promise.all([listSuggestions(), getStats()]);
    setSuggestions(list);
    setStats(indicators);
  }, []);

  useEffect(() => {
    refresh().catch((e) => setError(messageOf(e)));
  }, [refresh]);

  async function handleAnalyze() {
    setBusy(true);
    setError(null);
    try {
      await analyze(source);
      await refresh();
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  }

  async function handleReview(id: string, decision: Decision) {
    setError(null);
    try {
      await review(id, decision);
      await refresh();
    } catch (e) {
      setError(messageOf(e));
    }
  }

  return (
    <main className="layout">
      <header className="hero">
        <h1>TestPilot</h1>
        <p>
          L'IA propose des squelettes de tests unitaires, <strong>un humain valide</strong>. Les
          secrets sont masqués avant toute génération.
        </p>
      </header>

      <StatsBar stats={stats} />

      <section className="panel" aria-label="Analyse">
        <label htmlFor="source">Code Java à analyser</label>
        <textarea
          id="source"
          value={source}
          onChange={(e) => setSource(e.target.value)}
          placeholder="Collez ici une classe Java…"
          rows={12}
          spellCheck={false}
        />
        <div className="row">
          <button onClick={handleAnalyze} disabled={busy || source.trim() === ''}>
            {busy ? 'Analyse…' : 'Analyser le code'}
          </button>
          <button className="ghost" onClick={() => setSource(SAMPLE_SOURCE)}>
            Charger un exemple
          </button>
        </div>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
      </section>

      <section aria-label="Suggestions">
        <h2>Suggestions</h2>
        {suggestions.length === 0 ? (
          <p className="empty">Aucune suggestion pour l'instant.</p>
        ) : (
          suggestions.map((s) => <SuggestionCard key={s.id} suggestion={s} onReview={handleReview} />)
        )}
      </section>
    </main>
  );
}
