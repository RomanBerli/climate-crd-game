import { apiRequest, setStoredPlayerToken } from './client';
import { GameStateResponse, JoinLobbyResponse, SubmitSurveyPayload } from '../types/game';

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
  } catch {
    // ignore network errors on leave
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

export async function submitContractJoin(join: boolean): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>('/api/game/contract/join', {
    method: 'POST',
    body: JSON.stringify({ join }),
  });
}

export async function submitContractReview(
  action: 'STAY' | 'LEAVE' | 'START_VOTE'
): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>('/api/game/contract/review', {
    method: 'POST',
    body: JSON.stringify({ action }),
  });
}

export async function submitContractVote(abolish: boolean): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>('/api/game/contract/vote', {
    method: 'POST',
    body: JSON.stringify({ abolish }),
  });
}

export async function submitSurvey(payload: SubmitSurveyPayload): Promise<GameStateResponse> {
  return apiRequest<GameStateResponse>('/api/game/survey', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
