import React from 'react';
import { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface StatCardProps {
  title: string;
  value: string | number;
  icon?: LucideIcon;
  iconColor?: string;
  iconBg?: string;
  trend?: string;
  trendDirection?: 'up' | 'down' | 'neutral';
  trendLabel?: string;
  progress?: number;
  progressColor?: string;
  description?: string;
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon: Icon,
  iconColor = 'text-blue-600',
  iconBg = 'bg-blue-50',
  trend,
  trendDirection = 'neutral',
  trendLabel,
  progress,
  progressColor = 'bg-blue-600',
  description,
  className,
}) => {
  const trendColors = {
    up: 'text-emerald-700 bg-emerald-50 border-emerald-200/60',
    down: 'text-rose-700 bg-rose-50 border-rose-200/60',
    neutral: 'text-slate-600 bg-slate-100 border-slate-200/60',
  };

  return (
    <article
      className={cn(
        'bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs hover:shadow-md hover:border-slate-300 transition-all duration-200 flex flex-col justify-between group',
        className
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">{title}</p>
          <p className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">{value}</p>
        </div>
        {Icon && (
          <div
            className={cn(
              'w-11 h-11 rounded-xl flex items-center justify-center shrink-0 transition-transform group-hover:scale-105',
              iconBg,
              iconColor
            )}
          >
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      {(progress !== undefined || trend || description || trendLabel) && (
        <div className="mt-3 pt-3 border-t border-slate-100 space-y-2">
          {progress !== undefined && (
            <div className="space-y-1">
              <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                <div
                  className={cn('h-2 rounded-full transition-all duration-500', progressColor)}
                  style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
                />
              </div>
            </div>
          )}

          <div className="flex items-center justify-between text-xs">
            {trend && (
              <span
                className={cn(
                  'inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold border',
                  trendColors[trendDirection]
                )}
              >
                {trend}
              </span>
            )}
            {trendLabel && <span className="text-[11px] text-slate-500">{trendLabel}</span>}
            {description && !trendLabel && <span className="text-[11px] text-slate-500">{description}</span>}
          </div>
        </div>
      )}
    </article>
  );
};
