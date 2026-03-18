export type NotificationType = 'CLAIM' | 'PROPERTY_INSPECTION' | 'CLAIM_INSPECTION' | 'USER';

export interface AppNotification {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  status: string;
  actionUrl: string;
  createdAt: string;
  isRead?: boolean;
}
