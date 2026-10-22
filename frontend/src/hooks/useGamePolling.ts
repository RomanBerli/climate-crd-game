import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchGameState } from '../api/gameApi';
import { ApiError, getStoredPlayerToken, setStoredPlayerToken } from '../api/client';
import { GameStateResponse } from '../types/game';

interface UseGamePollingResult {
  gameState: GameStateResponse | null;
  loading: boolean;
  error: string | null;
  refreshNow: () => Promise<void>;
  setGameStateDirectly: (state: GameStateResponse | null) => void;
}

export function useGamePolling(enabled: boolean = true): UseGamePollingResult {
  const [gameState, setGameState] = useState<GameStateResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(() => Boolean(getStoredPlayerToken()));
  const [error, setError] = useState<string | null>(null);
  const isFetchingRef = useRef<boolean>(false);
  const sessionEpochRef = useRef<number>(0);

  const setGameStateDirectly = useCallback((state: GameStateResponse | null) => {
    sessionEpochRef.current += 1;
    isFetchingRef.current = false;
    setGameState(state);
    if (state) {
      setError(null);
    }
  }, []);

  const refreshNow = useCallback(async () => {
    const tokenAtStart = getStoredPlayerToken();
    if (!tokenAtStart) {
      sessionEpochRef.current += 1;
      isFetchingRef.current = false;
      setGameState(null);
      setLoading(false);
      return;
    }
    if (isFetchingRef.current) return;
    isFetchingRef.current = true;
    const epochAtStart = sessionEpochRef.current;
    try {
      const state = await fetchGameState();
      if (
        sessionEpochRef.current !== epochAtStart ||
        getStoredPlayerToken() !== tokenAtStart
      ) {
        return;
      }
      setGameState(state);
      setError(null);
    } catch (err) {
      if (
        sessionEpochRef.current !== epochAtStart ||
        getStoredPlayerToken() !== tokenAtStart
      ) {
        return;
      }
      if (err instanceof ApiError && err.status === 401) {
        setStoredPlayerToken(null);
        setGameState(null);
      } else if (err instanceof Error) {
        setError(err.message);
      }
    } finally {
      if (sessionEpochRef.current === epochAtStart) {
        isFetchingRef.current = false;
      }
      setLoading(false);
    }
  }, []);

  // Einmaliger Reconnect-Check beim Laden, NUR falls ein gespeichertes Token vorhanden ist
  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    if (getStoredPlayerToken()) {
      refreshNow();
    } else {
      setLoading(false);
    }
  }, [enabled, refreshNow]);

  // Polling (alle 1200ms) NUR im Warteraum (LOBBY) oder während des laufenden Spiels (RUNNING).
  // Im Login-Dashboard, im Fragebogen und im Endresultat-Screen wird NICHT gepollt.
  const shouldPoll =
    enabled &&
    gameState !== null &&
    (gameState.status === 'LOBBY' || gameState.status === 'RUNNING');

  useEffect(() => {
    if (!shouldPoll) {
      return;
    }

    const pollInterval = window.setInterval(() => {
      refreshNow();
    }, 1200);

    return () => window.clearInterval(pollInterval);
  }, [shouldPoll, refreshNow]);

  return {
    gameState,
    loading,
    error,
    refreshNow,
    setGameStateDirectly,
  };
}
