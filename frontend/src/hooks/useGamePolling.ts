import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchGameState } from '../api/gameApi';
import { ApiError, setStoredPlayerToken } from '../api/client';
import { GameStateResponse } from '../types/game';

interface UseGamePollingResult {
  gameState: GameStateResponse | null;
  loading: boolean;
  error: string | null;
  refreshNow: () => Promise<void>;
  setGameStateDirectly: (state: GameStateResponse | null) => void;
}

export function useGamePolling(pollingEnabled: boolean = true): UseGamePollingResult {
  const [gameState, setGameState] = useState<GameStateResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const isFetchingRef = useRef<boolean>(false);

  const setGameStateDirectly = useCallback((state: GameStateResponse | null) => {
    setGameState(state);
    if (state) {
      setError(null);
    }
  }, []);

  const refreshNow = useCallback(async () => {
    if (isFetchingRef.current) return;
    isFetchingRef.current = true;
    try {
      const state = await fetchGameState();
      setGameState(state);
      setError(null);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setStoredPlayerToken(null);
        setGameState(null);
      } else if (err instanceof Error) {
        setError(err.message);
      }
    } finally {
      setLoading(false);
      isFetchingRef.current = false;
    }
  }, []);

  useEffect(() => {
    if (!pollingEnabled) {
      setLoading(false);
      return;
    }

    refreshNow();
    const pollInterval = window.setInterval(() => {
      refreshNow();
    }, 1200);

    return () => window.clearInterval(pollInterval);
  }, [pollingEnabled, refreshNow]);

  return {
    gameState,
    loading,
    error,
    refreshNow,
    setGameStateDirectly,
  };
}
