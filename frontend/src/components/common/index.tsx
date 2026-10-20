import React from 'react';

export const Badge: React.FC<{
  variant?: 'accent' | 'neutral' | 'risk' | 'private';
  children: React.ReactNode;
}> = ({ variant = 'neutral', children }) => (
  <span className={`badge badge--${variant}`}>{children}</span>
);

export const Card: React.FC<{
  className?: string;
  children: React.ReactNode;
}> = ({ className = '', children }) => (
  <div className={`history-card ${className}`.trim()}>{children}</div>
);
