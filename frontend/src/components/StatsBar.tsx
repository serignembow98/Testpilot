import type { Stats } from '../types';

interface Props {
  stats: Stats | null;
}

export default function StatsBar({ stats }: Props) {
  const rate = stats ? Math.round(stats.acceptanceRate * 100) : 0;
  const items = [
    { label: 'Suggestions', value: stats?.total ?? 0 },
    { label: 'Approuvées', value: stats?.approved ?? 0 },
    { label: 'Rejetées', value: stats?.rejected ?? 0 },
    { label: "Taux d'acceptation", value: `${rate} %` },
    { label: 'Secrets masqués', value: stats?.redactions ?? 0 },
  ];
  return (
    <section aria-label="Indicateurs" className="stats">
      {items.map((item) => (
        <div className="stat" key={item.label}>
          <span className="stat-value">{item.value}</span>
          <span className="stat-label">{item.label}</span>
        </div>
      ))}
    </section>
  );
}
