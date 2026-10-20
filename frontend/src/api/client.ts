const PLAYER_TOKEN_STORAGE_KEY = 'climate_player_token';
const ADMIN_SECRET_STORAGE_KEY = 'climate_admin_secret';

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function getStoredPlayerToken(): string | null {
  try {
    return localStorage.getItem(PLAYER_TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setStoredPlayerToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(PLAYER_TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(PLAYER_TOKEN_STORAGE_KEY);
    }
  } catch {
    // ignore storage errors
  }
}

export function getStoredAdminSecret(): string {
  try {
    return localStorage.getItem(ADMIN_SECRET_STORAGE_KEY) || 'admin_geheim_123';
  } catch {
    return 'admin_geheim_123';
  }
}

export function setStoredAdminSecret(secret: string): void {
  try {
    localStorage.setItem(ADMIN_SECRET_STORAGE_KEY, secret);
  } catch {
    // ignore
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  extraHeaders: Record<string, string> = {}
): Promise<T> {
  const token = getStoredPlayerToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...extraHeaders,
  };

  if (token && !headers['X-Player-Token']) {
    headers['X-Player-Token'] = token;
  }

  const response = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: {
      ...headers,
      ...(options.headers as Record<string, string> | undefined),
    },
  });

  if (!response.ok) {
    let errorMessage = `HTTP Fehler ${response.status}`;
    try {
      const data = await response.json();
      if (data && typeof data.error === 'string') {
        errorMessage = data.error;
      }
    } catch {
      // ignore json parse failure
    }
    throw new ApiError(response.status, errorMessage);
  }

  if (response.status === 204) {
    return undefined as unknown as T;
  }

  return response.json() as Promise<T>;
}
