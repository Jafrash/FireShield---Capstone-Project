export interface AnalyticsCard {
  title: string;
  value: number | string;
  icon: string;
  color: string;
  description?: string;
}

export const ADVANCED_ANALYTICS: AnalyticsCard[] = [
  {
    title: 'Claims Approval Rate',
    value: '--',
    icon: 'percent',
    color: '#C72B32',
    description: 'Percentage of claims approved out of total claims.'
  },
  {
    title: 'Average Claim Amount',
    value: '--',
    icon: 'payments',
    color: '#E2725B',
    description: 'Mean value of all claims submitted.'
  },
  {
    title: 'Average Settlement Time',
    value: '--',
    icon: 'schedule',
    color: '#10b981',
    description: 'Average days taken to settle a claim.'
  },
  {
    title: 'Active Policy Ratio',
    value: '--',
    icon: 'pie_chart',
    color: '#FF6B35',
    description: 'Ratio of active policies to total policies.'
  }
];
