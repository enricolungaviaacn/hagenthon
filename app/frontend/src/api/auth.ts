import client from './client';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const login = async (data: LoginRequest): Promise<AuthResponse> => {
  const res = await client.post<AuthResponse>('/auth/login', data);
  return res.data;
};

export const register = async (data: RegisterRequest): Promise<AuthResponse> => {
  const res = await client.post<AuthResponse>('/auth/register', data);
  return res.data;
};

export const forgotPassword = async (email: string): Promise<void> => {
  await client.post('/auth/forgot-password', { email });
};

export const resetPassword = async (token: string, newPassword: string): Promise<void> => {
  await client.post('/auth/reset-password', { token, newPassword });
};
