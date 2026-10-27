/**
 * Formatiert einen internen Rappen-Betrag als Franken-Anzeige (z.B. 20 -> "0.2 Fr.", 400 -> "4 Fr.", 1200 -> "12 Fr.").
 */
export function formatFr(rappen: number): string {
  const chf = Number((rappen / 100).toFixed(2));
  return `${chf} Fr.`;
}
