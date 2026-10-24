import type { Decision, Status, Suggestion } from '../types';

const LABELS: Record<Status, string> = {
  PROPOSED: 'À relire',
  APPROVED: 'Approuvé',
  REJECTED: 'Rejeté',
};

interface Props {
  suggestion: Suggestion;
  onReview: (id: string, decision: Decision) => void;
}

export default function SuggestionCard({ suggestion, onReview }: Props) {
  const pending = suggestion.status === 'PROPOSED';

  async function copy() {
    try {
      await navigator.clipboard.writeText(suggestion.generatedTest);
    } catch {
      // presse-papiers indisponible : sans conséquence
    }
  }

  return (
    <article className="card" aria-label={`${suggestion.className}.${suggestion.methodName}`}>
      <header className="card-header">
        <h3>
          {suggestion.className}.<strong>{suggestion.methodName}</strong>
        </h3>
        <span className={`badge badge-${suggestion.status.toLowerCase()}`}>
          {LABELS[suggestion.status]}
        </span>
      </header>
      <pre className="code">
        <code>{suggestion.generatedTest}</code>
      </pre>
      <footer className="card-actions">
        <button disabled={!pending} onClick={() => onReview(suggestion.id, 'APPROVED')}>
          Approuver
        </button>
        <button
          className="secondary"
          disabled={!pending}
          onClick={() => onReview(suggestion.id, 'REJECTED')}
        >
          Rejeter
        </button>
        {suggestion.status === 'APPROVED' && (
          <button className="ghost" onClick={copy}>
            Copier le test
          </button>
        )}
      </footer>
    </article>
  );
}
