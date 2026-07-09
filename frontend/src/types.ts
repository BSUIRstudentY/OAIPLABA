export interface User {
  id: string;
  email: string;
  displayName: string;
  role: 'USER' | 'ADMIN';
  walletBalance: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export type VideoStatus = 'OFFERED' | 'SOLD' | 'REJECTED';

export interface OfferBreakdown {
  category: string;
  categoryMultiplier: number;
  baseMonthlyViews: number;
  estimatedMonthlyViews: number;
  averageCpm: number;
  watchTimeFactor: number;
  estimatedMonthlyRevenue: number;
  projectionMonths: number;
  projectedRevenue: number;
  buyoutShare: number;
  grossOffer: number;
  platformFee: number;
  offerPrice: number;
  currency: string;
  steps: string[];
}

export interface Video {
  id: string;
  title: string;
  description: string | null;
  category: string;
  durationSeconds: number;
  sizeBytes: number;
  contentType: string | null;
  status: VideoStatus;
  estimatedMonthlyViews: number;
  offerPrice: number;
  offerBreakdown: string | null;
  ownerDisplayName: string;
  createdAt: string;
  resolvedAt: string | null;
}

export interface WalletTransaction {
  id: string;
  type: string;
  amount: number;
  balanceAfter: number;
  description: string;
  videoId: string | null;
  createdAt: string;
}

export interface Wallet {
  balance: number;
  transactions: WalletTransaction[];
}

export interface PricingConfig {
  averageCpm: number;
  watchTimeFactor: number;
  projectionMonths: number;
  buyoutShare: number;
  platformFee: number;
  baseMonthlyViews: number;
}
