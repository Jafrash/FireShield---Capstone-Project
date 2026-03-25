export type UserRole = 'ADMIN' | 'CUSTOMER' | 'SURVEYOR' | 'UNDERWRITER' | 'SIU_INVESTIGATOR';

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  fullName?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId?: number;
  email?: string;
  firstName?: string;
  lastName?: string;
  role?: UserRole;
}

export interface GoogleLoginRequest {
  credential: string;
}

export interface ForgotPasswordRequest {
  username: string;
  email: string;
  phoneNumber: string;
  newPassword: string;
}

export interface ForgotPasswordResponse {
  message: string;
}

export interface RegisterCustomerRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
  city: string;
  state: string;
}

export interface RegisterSurveyorRequest {
  username: string;
  email: string;
  password: string;
  phoneNumber: string;
  licenseNumber: string;
  experienceYears: number;
  assignedRegion: string;
}

export interface RegisterResponse {
  message: string;
  userId: number;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
}
