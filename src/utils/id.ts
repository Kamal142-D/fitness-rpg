/**
 * Generate a locally-unique id for client-side entities (active-workout sets,
 * exercises). Not a UUID — good enough for in-memory/persisted local state. A
 * real UUID is used for the persisted session id (see uuid.ts).
 */
let counter = 0;
export function genId(prefix = 'id'): string {
  counter = (counter + 1) % 1_000_000;
  return `${prefix}_${Date.now().toString(36)}_${counter.toString(36)}`;
}
