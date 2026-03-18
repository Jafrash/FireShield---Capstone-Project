// Application Constants

// API Endpoints
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
    PROFILE: '/auth/profile'
  },
  CLAIMS: {
    BASE: '/claims',
    BY_ID: (id: number) => `/claims/${id}`,
    APPROVE: (id: number) => `/claims/${id}/approve`,
    REJECT: (id: number) => `/claims/${id}/reject`
  },
  POLICIES: {
    BASE: '/policies',
    BY_ID: (id: number) => `/policies/${id}`,
    QUOTES: '/policies/quotes'
  },
  PROPERTIES: {
    BASE: '/properties',
    BY_ID: (id: number) => `/properties/${id}`
  },
  INSPECTIONS: {
    PROPERTY: '/inspections/property',
    CLAIM: '/inspections/claim',
    BY_ID: (id: number) => `/inspections/${id}`,
    SUBMIT: (id: number) => `/inspections/${id}/submit`
  },
  DOCUMENTS: {
    BASE: '/documents',
    UPLOAD: '/documents/upload',
    BY_ID: (id: number) => `/documents/${id}`,
    DOWNLOAD: (id: number) => `/documents/${id}/download`
  },
  CUSTOMERS: {
    BASE: '/customers',
    BY_ID: (id: number) => `/customers/${id}`
  },
  SURVEYORS: {
    BASE: '/surveyors',
    BY_ID: (id: number) => `/surveyors/${id}`
  },
  SUBSCRIPTIONS: {
    BASE: '/subscriptions',
    BY_ID: (id: number) => `/subscriptions/${id}`
  }
};

// Status Codes
export const CLAIM_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED'
} as const;

export const POLICY_STATUS = {
  ACTIVE: 'ACTIVE',
  PENDING: 'PENDING',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED'
} as const;

export const INSPECTION_STATUS = {
  SCHEDULED: 'SCHEDULED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
} as const;

export const DOCUMENT_TYPE = {
  CLAIM_DOCUMENT: 'CLAIM_DOCUMENT',
  POLICY_DOCUMENT: 'POLICY_DOCUMENT',
  PROPERTY_DOCUMENT: 'PROPERTY_DOCUMENT',
  INSPECTION_DOCUMENT: 'INSPECTION_DOCUMENT',
  PROFILE_DOCUMENT: 'PROFILE_DOCUMENT'
} as const;

// User Roles
export const USER_ROLES = {
  ADMIN: 'ADMIN',
  CUSTOMER: 'CUSTOMER',
  SURVEYOR: 'SURVEYOR'
} as const;

// Application Config
export const APP_CONFIG = {
  DEFAULT_PAGE_SIZE: 10,
  MAX_FILE_SIZE: 5 * 1024 * 1024, // 5MB
  ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'application/pdf'],
  TOKEN_REFRESH_THRESHOLD: 5 * 60 * 1000, // 5 minutes
  REQUEST_TIMEOUT: 30000 // 30 seconds
};
