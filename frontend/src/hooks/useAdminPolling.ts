import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchAdminSessions } from '../api/adminApi';
import { ApiError } from '../api/client';
import { AdminSessionResponse } from '../types/game';

interface UseAdminPollingResult {
  sessions: AdminSessionResponse[];
  loading: boolean;
  error: string | null;
  unauthorized: boolean;
  refreshSessions: () => Promise<void>;
}

export function useAdminPolling(adminKey: string, enabled: boolean = true): UseAdminPollingResult {
  const [sessions, setSessions] = useState<AdminSessionResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [unauthorized, setUnauthorized] = useState<boolean>(false);
  const isFetchingRef = useRef<boolean>(false);

  const refreshSessions = useCallback(async () => {
    if (!enabled || !adminKey || isFetchingRef.current) return;
    isFetchingRef.current = true;
    try {
      const data = await fetchAdminSessions(adminKey);
      setSessions(data);
      setError(null);
      setUnauthorized(false);
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setUnauthorized(true);
        setError('Nicht autorisiert: Ungültiges Admin-Secret.');
      } else if (err instanceof Error) {
        setError(err.message);
      }
    } finally {
      setLoading(false);
      isFetchingRef.current = false;
    }
  }, [adminKey, enabled]);

  useEffect(() => {
    if (!enabled || !adminKey) {
      setLoading(false);
      return;
    }
    refreshSessions();
    const interval = window.setInterval(() => {
      refreshSessions();
    }, 1500);
    return () => window.clearInterval(interval);
  }, [enabled, adminKey, refreshSessions]);

  return {
    sessions,
    loading,
    error,
    unauthorized,
    refreshSessions,
  };
}
