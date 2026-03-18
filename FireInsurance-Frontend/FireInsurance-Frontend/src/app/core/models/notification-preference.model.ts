export interface NotificationPreference {
  id?: number;
  username?: string;
  emailEnabled: boolean;
  enabledEventKeys: string[];
  updatedAt?: string;
  createdAt?: string;
}

export const DEFAULT_EVENT_KEYS: string[] = [
  'POLICY_SUBMITTED',
  'POLICY_APPROVAL',
  'POLICY_REJECTION',
  'POLICY_RENEWAL_REMINDER',
  'CLAIM_STATUS_CHANGE',
  'INSPECTION_ASSIGNED',
  'INSPECTION_COMPLETED'
];

export const EVENT_LABELS: { [key: string]: string } = {
  'POLICY_SUBMITTED': 'Policy Application Submitted',
  'POLICY_APPROVAL': 'Policy Approved',
  'POLICY_REJECTION': 'Policy Rejected',
  'POLICY_RENEWAL_REMINDER': 'Policy Renewal Reminder',
  'CLAIM_STATUS_CHANGE': 'Claim Status Changed',
  'INSPECTION_ASSIGNED': 'Inspection Assigned',
  'INSPECTION_COMPLETED': 'Inspection Completed'
};
