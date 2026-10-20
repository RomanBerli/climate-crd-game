export type SessionStatus = 'LOBBY' | 'RUNNING' | 'PAUSED' | 'FINISHED';
export type RoundPhase = 'REGULAR' | 'OVERTIME';

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

/**
 * Spieler-State (Spec 1.4: enthält niemals totalPotRappen oder kollektiven Fortschritt!)
 */
export interface GameStateResponse {
  sessionId: string;
  sessionCode: string;
  status: SessionStatus;
  riskLevel: number;
  riskPercent: number;
  targetSumRappen: number;
  currentRound: number;
  totalRounds: number;
  roundPhase: RoundPhase;
  remainingSeconds: number;
  isPaused: boolean;
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
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  totalInvestedRappen: number;
  currentRoundInvestmentRappen: number | null;
  finalPayoutRappen: number | null;
  lastSeenAt: string | null;
}

export interface AdminSessionResponse {
  id: string;
  code: string;
  status: SessionStatus;
  riskLevel: number;
  riskPercent: number;
  currentRound: number;
  totalRounds: number;
  roundPhase: RoundPhase;
  remainingSeconds: number;
  isPaused: boolean;
  targetSumRappen: number;
  totalPotRappen: number;
  connectedPlayersCount: number;
  groupSize: number;
  isGoalReached: boolean | null;
  riskOccurred: boolean | null;
  createdAt: string;
  updatedAt: string;
  players: AdminPlayerDto[];
  history: RoundHistoryDto[];
}
