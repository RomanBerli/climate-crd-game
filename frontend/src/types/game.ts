export type SessionStatus = 'LOBBY' | 'RUNNING' | 'FINISHED';
export type GameMode = 'NORMAL' | 'WITH_CONTRACTS';

export interface JoinLobbyResponse {
  sessionId: string;
  sessionCode: string;
  playerNumber: number;
  sessionToken: string;
}

export interface PlayerStateDto {
  playerNumber: number;
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  ownTotalInvestedRappen: number;
  hasSubmittedCurrentRound: boolean;
  currentRoundInvestmentRappen: number | null;
  allowedInvestmentOptions: number[];
  finalPayoutRappen: number | null;
}

export interface PlayerSlotStatusDto {
  playerNumber: number;
  isConnected: boolean;
  isReady: boolean;
}

export interface PlayerInvestmentDto {
  playerNumber: number;
  amountRappen: number;
  isAutoBet: boolean;
}

export interface RoundHistoryDto {
  round: number;
  roundTotalRappen: number;
  cumulativePotRappen: number;
  investments: PlayerInvestmentDto[];
}

export interface EndgameResultDto {
  isGoalReached: boolean;
  riskOccurred: boolean | null;
  riskLevel: number;
  riskPercent: number;
  finalPayoutRappen: number;
  remainingBalanceBeforeRiskRappen: number;
  scenarioCode: string;
  message: string;
}

export interface GameStateResponse {
  sessionId: string;
  sessionCode: string;
  status: SessionStatus;
  gameMode: GameMode;
  riskLevel: number;
  riskPercent: number;
  targetSumRappen: number;
  totalPotRappen: number;
  currentRound: number;
  totalRounds: number;
  connectedPlayersCount: number;
  groupSize: number;
  player: PlayerStateDto;
  playersStatus: PlayerSlotStatusDto[];
  history: RoundHistoryDto[];
  endgame: EndgameResultDto | null;
}

export interface AdminPlayerDto {
  playerNumber: number;
  isConnected: boolean;
  isReadyCurrentRound: boolean;
  finalPayoutRappen: number | null;
  lastSeenAt: string | null;
}

export interface AdminSessionResponse {
  id: string;
  code: string;
  status: SessionStatus;
  gameMode: GameMode;
  riskLevel: number;
  riskPercent: number;
  currentRound: number;
  totalRounds: number;
  targetSumRappen: number;
  totalPotRappen: number;
  connectedPlayersCount: number;
  readyPlayersCount: number;
  groupSize: number;
  canAdvanceRound: boolean;
  isGoalReached: boolean | null;
  riskOccurred: boolean | null;
  createdAt: string;
  updatedAt: string;
  players: AdminPlayerDto[];
}
