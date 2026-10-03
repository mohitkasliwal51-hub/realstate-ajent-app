import type { OrganizationType, UserProfile } from './auth/authTypes';

/**
 * Resolves which business mode the UI should run in.
 *
 * Until the backend exposes `organizationType` on auth/profile responses,
 * we fall back to HYBRID so that both owner and broker features are available
 * (landlord fields stay optional). Once the backend returns the field,
 * the UI switches automatically without further changes.
 */
export const getOrgMode = (user: UserProfile | null | undefined): OrganizationType =>
  user?.organizationType ?? 'HYBRID';

/** Broker features: landlords directory, landlord on property/lease, brokerage fees, payouts. */
export const isBrokerMode = (user: UserProfile | null | undefined): boolean =>
  getOrgMode(user) !== 'OWNER';

/** Pure owner mode: no landlords directory, no brokerage fees, no payouts. */
export const isOwnerMode = (user: UserProfile | null | undefined): boolean =>
  getOrgMode(user) === 'OWNER';

/** Landlord must be selected on property/lease only for pure brokerages. */
export const isLandlordRequired = (user: UserProfile | null | undefined): boolean =>
  getOrgMode(user) === 'BROKERAGE';
