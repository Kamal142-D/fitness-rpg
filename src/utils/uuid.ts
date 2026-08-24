/**
 * RFC-4122 v4 UUID. Used as a client-generated session id so completing a
 * workout is idempotent (a retry reuses the same id). Uses Math.random — fine
 * for an idempotency key, not for security tokens.
 */
export function uuidv4(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
