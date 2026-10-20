import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchAdminSessions } from '../api/adminApi';
import { AdminSessionResponse } from '../types/game';

interface UseAdminPollingResult {
  sessions: AdminSessionResponse[];
  loading: boolean;
  error: string | null;
  refreshSessions: () => Promise<void>;
}

export function useAdminPolling(adminKey: string, enabled: boolean = true): UseAdminPollingResult {
  const [sessions, setSessions] = useState<AdminSessionResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const isFetchingRef = useRef<boolean>(false);

  const refreshSessions = useCallback(async () => {
    if (!enabled || isFetchingRef.current) return;
    isFetchingRef.current = true;
    try {
      const data = await fetchAdminSessions(adminKey);
      setSessions(data);
      setError(null);
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      }
    } finally {
      setLoading(false);
      isFetchingRef.current = false;
    }
  }, [adminKey, enabled]);

  useEffect(() => {
    if (!enabled) return;
    refreshSessions();
    const interval = window.setInterval(() => {
      refreshSessions();
    }, 1500);
    return () => window.clearInterval(interval);
  }, [enabled, refreshSessions]);

  return {
    sessions,
    loading,
    error,
    refreshSessions,
  };
}
