import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchAdminConfig, fetchAdminSessions } from '../api/adminApi';
import { ApiError } from '../api/client';
import { AdminSessionResponse } from '../types/game';

interface UseAdminPollingResult {
  sessions: AdminSessionResponse[];
  groupSize: number | null;
  loading: boolean;
  error: string | null;
  unauthorized: boolean;
  refreshSessions: () => Promise<void>;
}

export function useAdminPolling(adminKey: string, enabled: boolean = true): UseAdminPollingResult {
  const [sessions, setSessions] = useState<AdminSessionResponse[]>([]);
  const [groupSize, setGroupSize] = useState<number | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [unauthorized, setUnauthorized] = useState<boolean>(false);
  const isFetchingRef = useRef<boolean>(false);

  const refreshSessions = useCallback(async () => {
    if (!enabled || !adminKey || isFetchingRef.current) return;
    isFetchingRef.current = true;
    try {
      const [data, config] = await Promise.all([
        fetchAdminSessions(adminKey),
        fetchAdminConfig(adminKey).catch(() => null),
      ]);
      setSessions(data);
      if (config && typeof config.groupSize === 'number') {
        setGroupSize(config.groupSize);
      } else if (data.length > 0) {
        setGroupSize(data[0].groupSize);
      }
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
    groupSize,
    loading,
    error,
    unauthorized,
    refreshSessions,
  };
}
