import { requireCondition } from './errors.js';
export function coordinates(lat, lng) {
  requireCondition(typeof lat === 'number' && Number.isFinite(lat) && lat >= -90 && lat <= 90 &&
    typeof lng === 'number' && Number.isFinite(lng) && lng >= -180 && lng <= 180, 400, 'Invalid coordinates');
}
export function text(value, name, max = 1000) {
  requireCondition(typeof value === 'string' && value.trim().length > 0 && value.length <= max, 400, `Invalid ${name}`);
  return value.trim();
}
export function integer(value, name) {
  requireCondition(Number.isSafeInteger(value) && value > 0, 400, `Invalid ${name}`);
  return value;
}
