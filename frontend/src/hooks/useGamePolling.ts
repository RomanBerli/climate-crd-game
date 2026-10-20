import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchGameState } from '../api/gameApi';
import { ApiError, setStoredPlayerToken } from '../api/client';
import { GameStateResponse } from '../types/game';

interface UseGamePollingResult {
  gameState: GameStateResponse | null;
  displaySeconds: number;
  loading: boolean;
  error: string | null;
  refreshNow: () => Promise<void>;
  setGameStateDirectly: (state: GameStateResponse | null) => void;
}

export function useGamePolling(pollingEnabled: boolean = true): UseGamePollingResult {
  const [gameState, setGameState] = useState<GameStateResponse | null>(null);
  const [displaySeconds, setDisplaySeconds] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const isFetchingRef = useRef<boolean>(false);

  const setGameStateDirectly = useCallback((state: GameStateResponse | null) => {
    setGameState(state);
    if (state) {
      setDisplaySeconds(state.remainingSeconds);
      setError(null);
    }
  }, []);

  const refreshNow = useCallback(async () => {
    if (isFetchingRef.current) return;
    isFetchingRef.current = true;
    try {
      const state = await fetchGameState();
      setGameState(state);
      setDisplaySeconds((prev) => {
        // Sanfte Synchronisation mit dem Server-Timer
        if (Math.abs(prev - state.remainingSeconds) > 1 || state.isPaused) {
          return state.remainingSeconds;
        }
        return state.remainingSeconds;
      });
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

  // Initialer Abruf + Polling alle 1200ms (Plan 3.2)
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

  // Lokale Smooth-Timer-Interpolation (1000ms Takt, stoppt bei Pause)
  useEffect(() => {
    if (!gameState || gameState.status !== 'RUNNING' || gameState.isPaused) {
      return;
    }

    const tickInterval = window.setInterval(() => {
      setDisplaySeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => window.clearInterval(tickInterval);
  }, [gameState?.status, gameState?.isPaused, gameState?.currentRound, gameState?.roundPhase]);

  return {
    gameState,
    displaySeconds,
    loading,
    error,
    refreshNow,
    setGameStateDirectly,
  };
}
