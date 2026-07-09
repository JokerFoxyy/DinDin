export interface TokenResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface MeResponse {
  id: string;
  email: string;
}
