import { HttpError } from '../errors.js';
// Deliberately fail closed until a gateway, country support and payout policy are configured.
// Completion of a service job never implies payment capture or provider payout.
export class DisabledPayments {
  async authorize() { throw new HttpError(503,'Payment gateway is not configured'); }
  async capture() { throw new HttpError(503,'Payment gateway is not configured'); }
  async transfer() { throw new HttpError(503,'Payment gateway is not configured'); }
}
