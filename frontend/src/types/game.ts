export type SessionStatus = 'LOBBY' | 'RUNNING' | 'FINISHED';
export type GameMode = 'NORMAL' | 'WITH_CONTRACTS';
export type RoundPhase = 'CONTRACT_JOIN' | 'CONTRACT_REVIEW' | 'CONTRACT_VOTE' | 'INVESTMENT';
export type ContractJoinChoice = 'YES' | 'CONDITIONAL' | 'NO';

export interface JoinLobbyResponse {
  sessionId: string;
  sessionCode: string;
  playerNumber: number;
  sessionToken: string;
}

export interface SubmitSurveyPayload {
  expectedProbabilityPct: number;
  reactionToUnfairness: string;
  motivationRanking: string[];
  contractJoinedReason?: string | null;
  contractNotJoinedReason?: string | null;
  contractLeftReason?: string | null;
  contractStayedReason?: string | null;
  age: number;
  gender: string;
  twintPhone: string;
}

export interface ContractStateDto {
  roundPhase: RoundPhase;
  contractActive: boolean;
  contractAbolished: boolean;
  memberCount: number;
  minInvestmentRappen: number;
  canLeaveContractNow: boolean;
  hasDecidedCurrentContractPhase: boolean;
  lastContractEvent: string | null;
}

export interface PlayerStateDto {
  playerNumber: number;
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  ownTotalInvestedRappen: number;
  hasSubmittedCurrentRound: boolean;
  hasSubmittedSurvey: boolean;
  inContract: boolean;
  everJoinedContract: boolean;
  leftContract: boolean;
  contractJoinChoice: ContractJoinChoice | null;
  currentRoundInvestmentRappen: number | null;
  allowedInvestmentOptions: number[];
  finalPayoutRappen: number | null;
}

export interface PlayerSlotStatusDto {
  playerNumber: number;
  isConnected: boolean;
  isReady: boolean;
  inContract: boolean;
  leftContract: boolean;
}

export interface PlayerInvestmentDto {
  playerNumber: number;
  amountRappen: number;
  isAutoBet: boolean;
}

export interface RoundHistoryDto {
  round: number;
  roundTotalRappen: number;
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
  roundPhase: RoundPhase;
  contractState: ContractStateDto;
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
  inContract: boolean;
  leftContract: boolean;
  finalPayoutRappen: number | null;
  lastSeenAt: string | null;
}

export interface AdminSessionResponse {
  id: string;
  code: string;
  status: SessionStatus;
  gameMode: GameMode;
  roundPhase: RoundPhase;
  contractActive: boolean;
  contractMemberCount: number;
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
