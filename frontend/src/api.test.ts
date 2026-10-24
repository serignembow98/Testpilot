import { afterEach, describe, expect, it, vi } from 'vitest';
import { analyze, getStats, listSuggestions, review } from './api';

function mockFetch(response: Partial<Response> & { json?: () => Promise<unknown> }) {
  const fn = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fn);
  return fn;
}

afterEach(() => vi.unstubAllGlobals());

describe('api', () => {
  it('analyze envoie le code source en POST', async () => {
    const fetchMock = mockFetch({ ok: true, json: async () => ({ className: 'A', suggestions: [], redactions: 0 }) });

    const result = await analyze('public class A {}');

    expect(result.className).toBe('A');
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/analyses');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ source: 'public class A {}' });
  });

  it('review envoie la décision sur la bonne ressource', async () => {
    const fetchMock = mockFetch({ ok: true, json: async () => ({ id: '42', status: 'APPROVED' }) });

    await review('42', 'APPROVED');

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/suggestions/42/review');
    expect(JSON.parse(init.body)).toEqual({ decision: 'APPROVED' });
  });

  it('listSuggestions et getStats appellent leurs endpoints', async () => {
    const fetchMock = mockFetch({ ok: true, json: async () => [] });

    await listSuggestions();
    await getStats();

    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual(['/api/suggestions', '/api/stats']);
  });

  it("remonte le champ 'detail' renvoyé par l'API", async () => {
    mockFetch({ ok: false, status: 400, json: async () => ({ detail: 'Aucune classe trouvee' }) });

    await expect(analyze('x')).rejects.toThrow('Aucune classe trouvee');
  });

  it('utilise un message générique quand le corps est illisible', async () => {
    mockFetch({
      ok: false,
      status: 500,
      json: async () => {
        throw new Error('not json');
      },
    });

    await expect(getStats()).rejects.toThrow('Erreur 500');
  });
});
