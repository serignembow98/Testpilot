import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import type { Stats, Suggestion } from './types';

const baseStats: Stats = { total: 0, proposed: 0, approved: 0, rejected: 0, acceptanceRate: 0, redactions: 0 };

function suggestion(overrides: Partial<Suggestion> = {}): Suggestion {
  return {
    id: 's1',
    className: 'Pricing',
    methodName: 'total',
    generatedTest: '@Test\nvoid total_nominalCase() {}',
    status: 'PROPOSED',
    createdAt: '2026-10-23T10:00:00Z',
    ...overrides,
  };
}

function json(body: unknown, status = 200) {
  return Promise.resolve({ ok: status < 400, status, json: async () => body } as Response);
}

/** Faux serveur : état en mémoire + routage par URL. */
function installFakeServer(initial: Suggestion[] = []) {
  let suggestions = [...initial];
  const calls: { url: string; body?: unknown }[] = [];

  const fetchMock = vi.fn((url: string, init?: RequestInit) => {
    const body = init?.body ? JSON.parse(init.body as string) : undefined;
    calls.push({ url, body });

    if (url === '/api/suggestions') return json(suggestions);
    if (url === '/api/stats') {
      const approved = suggestions.filter((s) => s.status === 'APPROVED').length;
      const rejected = suggestions.filter((s) => s.status === 'REJECTED').length;
      const reviewed = approved + rejected;
      return json({
        ...baseStats,
        total: suggestions.length,
        approved,
        rejected,
        acceptanceRate: reviewed ? approved / reviewed : 0,
        redactions: 2,
      });
    }
    if (url === '/api/analyses') {
      if (!body.source.includes('class')) return json({ detail: 'Aucune classe trouvee dans le code fourni' }, 400);
      const created = suggestion({ id: 'new', className: 'Invoice', methodName: 'compute' });
      suggestions = [created, ...suggestions];
      return json({ className: 'Invoice', suggestions: [created], redactions: 2 }, 201);
    }
    const match = url.match(/^\/api\/suggestions\/(.+)\/review$/);
    if (match) {
      suggestions = suggestions.map((s) => (s.id === match[1] ? { ...s, status: body.decision } : s));
      return json(suggestions.find((s) => s.id === match[1]));
    }
    return json({ detail: 'not found' }, 404);
  });

  vi.stubGlobal('fetch', fetchMock);
  return { calls };
}

beforeEach(() => vi.restoreAllMocks());
afterEach(() => vi.unstubAllGlobals());

describe('App', () => {
  it('affiche un état vide puis les suggestions existantes', async () => {
    installFakeServer([suggestion()]);

    render(<App />);

    const card = await screen.findByRole('article', { name: 'Pricing.total' });
    expect(within(card).getByText('À relire')).toBeInTheDocument();
    expect(within(card).getByText(/total_nominalCase/)).toBeInTheDocument();
  });

  it("indique qu'il n'y a aucune suggestion", async () => {
    installFakeServer([]);

    render(<App />);

    expect(await screen.findByText("Aucune suggestion pour l'instant.")).toBeInTheDocument();
  });

  it("désactive l'analyse tant que le champ est vide", async () => {
    installFakeServer([]);
    render(<App />);

    expect(await screen.findByRole('button', { name: 'Analyser le code' })).toBeDisabled();
  });

  it('charge un exemple, lance l’analyse et affiche la nouvelle suggestion', async () => {
    const user = userEvent.setup();
    const { calls } = installFakeServer([]);
    render(<App />);

    await user.click(await screen.findByRole('button', { name: 'Charger un exemple' }));
    const textarea = screen.getByLabelText('Code Java à analyser') as HTMLTextAreaElement;
    expect(textarea.value).toContain('InvoiceCalculator');

    await user.click(screen.getByRole('button', { name: 'Analyser le code' }));

    expect(await screen.findByRole('article', { name: 'Invoice.compute' })).toBeInTheDocument();
    expect(calls.some((c) => c.url === '/api/analyses')).toBe(true);
    expect(await screen.findByText('Secrets masqués')).toBeInTheDocument();
  });

  it("affiche l'erreur renvoyée par l'API", async () => {
    const user = userEvent.setup();
    installFakeServer([]);
    render(<App />);

    await user.type(await screen.findByLabelText('Code Java à analyser'), 'int x = 1;');
    await user.click(screen.getByRole('button', { name: 'Analyser le code' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Aucune classe trouvee dans le code fourni');
  });

  it('approuve une suggestion : badge mis à jour, boutons désactivés, taux affiché', async () => {
    const user = userEvent.setup();
    const { calls } = installFakeServer([suggestion()]);
    render(<App />);

    const card = await screen.findByRole('article', { name: 'Pricing.total' });
    await user.click(within(card).getByRole('button', { name: 'Approuver' }));

    await waitFor(() => expect(within(card).getByText('Approuvé')).toBeInTheDocument());
    expect(calls.find((c) => c.url === '/api/suggestions/s1/review')?.body).toEqual({ decision: 'APPROVED' });
    expect(within(card).getByRole('button', { name: 'Approuver' })).toBeDisabled();
    expect(within(card).getByRole('button', { name: 'Rejeter' })).toBeDisabled();
    expect(screen.getByText('100 %')).toBeInTheDocument();
    expect(within(card).getByRole('button', { name: 'Copier le test' })).toBeInTheDocument();
  });

  it('rejette une suggestion', async () => {
    const user = userEvent.setup();
    installFakeServer([suggestion()]);
    render(<App />);

    const card = await screen.findByRole('article', { name: 'Pricing.total' });
    await user.click(within(card).getByRole('button', { name: 'Rejeter' }));

    await waitFor(() => expect(within(card).getByText('Rejeté')).toBeInTheDocument());
    expect(within(card).queryByRole('button', { name: 'Copier le test' })).not.toBeInTheDocument();
  });

  it('copie le test approuvé dans le presse-papiers', async () => {
    const user = userEvent.setup();
    installFakeServer([suggestion({ status: 'APPROVED' })]);
    render(<App />);
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();

    await user.click(await screen.findByRole('button', { name: 'Copier le test' }));

    expect(writeText).toHaveBeenCalledWith('@Test\nvoid total_nominalCase() {}');
  });

  it("affiche une erreur si le chargement initial échoue", async () => {
    vi.stubGlobal('fetch', vi.fn(() => json({ detail: 'Service indisponible' }, 503)));

    render(<App />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Service indisponible');
  });

  it("affiche l'erreur si la revue échoue", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn((url: string) => {
      if (url === '/api/suggestions') return json([suggestion()]);
      if (url === '/api/stats') return json(baseStats);
      return json({ detail: 'Cette suggestion a deja ete relue (APPROVED)' }, 409);
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<App />);

    await user.click(await screen.findByRole('button', { name: 'Approuver' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('deja ete relue');
  });
});
