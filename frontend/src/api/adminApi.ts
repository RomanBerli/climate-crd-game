import { apiRequest, getStoredAdminSecret } from './client';
import { AdminSessionResponse } from '../types/game';

function adminHeaders(customKey?: string): Record<string, string> {
  return {
    'X-Admin-Key': customKey ?? getStoredAdminSecret(),
  };
}

export async function fetchAdminSessions(adminKey?: string): Promise<AdminSessionResponse[]> {
  return apiRequest<AdminSessionResponse[]>('/api/admin/sessions', { method: 'GET' }, adminHeaders(adminKey));
}

export async function fetchAdminSessionById(sessionId: string, adminKey?: string): Promise<AdminSessionResponse> {
  return apiRequest<AdminSessionResponse>(`/api/admin/sessions/${sessionId}`, { method: 'GET' }, adminHeaders(adminKey));
}

export async function createAdminSession(riskLevel: number, adminKey?: string): Promise<AdminSessionResponse> {
  return apiRequest<AdminSessionResponse>(
    '/api/admin/sessions',
    {
      method: 'POST',
      body: JSON.stringify({ riskLevel }),
    },
    adminHeaders(adminKey)
  );
}

export async function startAdminSession(sessionId: string, adminKey?: string): Promise<AdminSessionResponse> {
  return apiRequest<AdminSessionResponse>(
    `/api/admin/sessions/${sessionId}/start`,
    { method: 'POST' },
    adminHeaders(adminKey)
  );
}

export async function pauseAdminSession(sessionId: string, adminKey?: string): Promise<AdminSessionResponse> {
  return apiRequest<AdminSessionResponse>(
    `/api/admin/sessions/${sessionId}/pause`,
    { method: 'POST' },
    adminHeaders(adminKey)
  );
}

export async function resumeAdminSession(sessionId: string, adminKey?: string): Promise<AdminSessionResponse> {
  return apiRequest<AdminSessionResponse>(
    `/api/admin/sessions/${sessionId}/resume`,
    { method: 'POST' },
    adminHeaders(adminKey)
  );
}

export async function downloadSessionCsv(sessionId: string, sessionCode: string, adminKey?: string): Promise<void> {
  const key = adminKey ?? getStoredAdminSecret();
  const response = await fetch(`/api/admin/sessions/${sessionId}/export.csv`, {
    method: 'GET',
    headers: {
      'X-Admin-Key': key,
    },
  });

  if (!response.ok) {
    throw new Error(`CSV-Export fehlgeschlagen (Status ${response.status})`);
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `milinski_session_${sessionCode}.csv`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
