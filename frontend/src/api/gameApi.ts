import { apiRequest, setStoredPlayerToken } from './client';
import { GameStateResponse, JoinLobbyResponse } from '../types/game';

export async function joinLobby(code: string): Promise<JoinLobbyResponse> {
  const response = await apiRequest<JoinLobbyResponse>('/api/lobby/join', {
    method: 'POST',
    body: JSON.stringify({ code: code.trim().toUpperCase() }),
  });
  setStoredPlayerToken(response.sessionToken);
  return response;
}

export async function leaveLobby(): Promise<void> {
  try {
    await apiRequest<void>('/api/lobby/leave', { method: 'POST' });
  } finally {
    setStoredPlayerToken(null);
  }
}

export async function fetchGameState(): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>('/api/game/state', {
    method: 'GET',
  });
}

export async function submitInvestment(amountRappen: number): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>('/api/game/invest', {
    method: 'POST',
    body: JSON.stringify({ amountRappen }),
  });
}
