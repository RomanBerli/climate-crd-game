import { useCallback, useEffect, useState } from 'react';

/**
 * Isolierter lokaler Notizblock im Browser (Spec 1.4 & 2.1, Plan 3.3).
 * Wird niemals an den Server übertragen.
 */
export function useLocalNotepad(sessionId: string | undefined, playerNumber?: number) {
  const storageKey = sessionId
    ? `climate_notepad_${sessionId}_p${playerNumber ?? 0}`
    : 'climate_notepad_default';

  const [notes, setNotesState] = useState<string>(() => {
    try {
      return localStorage.getItem(storageKey) || '';
    } catch {
      return '';
    }
  });

  useEffect(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      setNotesState(saved || '');
    } catch {
      setNotesState('');
    }
  }, [storageKey]);

  const setNotes = useCallback(
    (value: string) => {
      setNotesState(value);
      try {
        localStorage.setItem(storageKey, value);
      } catch {
        // ignore storage quota errors
      }
    },
    [storageKey]
  );

  return { notes, setNotes };
}
