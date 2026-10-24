import type { AnalysisResult, Decision, Stats, Suggestion } from './types';

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!response.ok) {
    let detail = `Erreur ${response.status}`;
    try {
      const body = await response.json();
      if (body?.detail) detail = body.detail;
    } catch {
      // corps non JSON : on garde le message générique
    }
    throw new Error(detail);
  }
  return response.json() as Promise<T>;
}

export const analyze = (source: string) =>
  request<AnalysisResult>('/api/analyses', { method: 'POST', body: JSON.stringify({ source }) });

export const listSuggestions = () => request<Suggestion[]>('/api/suggestions');

export const review = (id: string, decision: Decision) =>
  request<Suggestion>(`/api/suggestions/${id}/review`, {
    method: 'POST',
    body: JSON.stringify({ decision }),
  });

export const getStats = () => request<Stats>('/api/stats');
