import { Injectable } from '@angular/core';
import { SiuClaimDetails } from './siu.service';

export interface FraudRule {
  id: string;
  name: string;
  description: string;
  impactScore: number;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  isTriggered: boolean;
}

export interface FraudAnalysisResult {
  totalScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  triggeredRules: FraudRule[];
  allRules: FraudRule[];
  analysisTimestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class FraudEngineService {

  /**
   * Performs a comprehensive heuristic scan on a claim to identify potential fraud patterns.
   */
  analyzeClaim(claim: SiuClaimDetails, documents: any[] = []): FraudAnalysisResult {
    const rules: FraudRule[] = this.getBaseRules();
    let totalScore = 0;

    // 1. Ultra-Temporal Anomaly (Policy < 1 day old)
    const policyStart = new Date(claim.createdAt).getTime(); // Using createdAt of subscription/claim as proxy if policy date missing
    const incidentDate = new Date(claim.incidentDate).getTime();
    const hoursDiff = (incidentDate - policyStart) / (1000 * 60 * 60);
    
    if (hoursDiff < 24 && hoursDiff >= 0) {
      this.triggerRule(rules, 'TEMP_ANOMALY', 35);
    }

    // 2. Evidence Gap Analysis (Claims > ₹2,000,000 missing FIR)
    const isHighValue = claim.claimAmount > 2000000;
    const hasFIR = !!claim.firNumber || this.hasDocument(documents, 'FIR');
    const hasFireReport = !!claim.fireBrigadeReportNumber || this.hasDocument(documents, 'FIRE');
    
    if (isHighValue && (!hasFIR || !hasFireReport)) {
      this.triggerRule(rules, 'EVIDENCE_GAP', 40);
    }

    // 3. Coverage Disparity (Claim > 90% of Max Coverage)
    if (claim.policy?.maxCoverageAmount && claim.claimAmount > (claim.policy.maxCoverageAmount * 0.9)) {
       this.triggerRule(rules, 'COVERAGE_LIMIT', 15);
    }

    // 4. Behavioral Analysis (Suspicious Keywords)
    const suspiciousKeywords = ['immediate', 'urgent', 'unattended', 'fast-burning', 'no witness'];
    const desc = (claim.description || '').toLowerCase();
    const matched = suspiciousKeywords.filter(k => desc.includes(k));
    if (matched.length >= 2) {
      this.triggerRule(rules, 'BEHAVIORAL_PATTERN', 10);
    }

    // Calculate final score (capped at 100)
    totalScore = rules.reduce((acc, rule) => acc + (rule.isTriggered ? rule.impactScore : 0), 0);
    totalScore = Math.min(100, totalScore);

    return {
      totalScore,
      riskLevel: this.getRiskLevel(totalScore),
      triggeredRules: rules.filter(r => r.isTriggered),
      allRules: rules,
      analysisTimestamp: new Date()
    };
  }

  private getBaseRules(): FraudRule[] {
    return [
       {
        id: 'TEMP_ANOMALY',
        name: 'Ultra-Temporal Anomaly',
        description: 'Incident occurred within 24 hours of policy activation.',
        impactScore: 35,
        severity: 'CRITICAL',
        isTriggered: false
      },
      {
        id: 'EVIDENCE_GAP',
        name: 'Evidence Gap Analysis',
        description: 'Large-scale claim (> ₹2M) missing mandatory FIR or Fire Brigade documentation.',
        impactScore: 40,
        severity: 'HIGH',
        isTriggered: false
      },
      {
        id: 'COVERAGE_LIMIT',
        name: 'Coverage Saturation',
        description: 'Claim amount is suspiciously close to or exceeds the maximum policy coverage.',
        impactScore: 15,
        severity: 'MEDIUM',
        isTriggered: false
      },
      {
        id: 'BEHAVIORAL_PATTERN',
        name: 'Linguistic Risk Pattern',
        description: 'Description contains multiple keywords typically associated with fraudulent reporting.',
        impactScore: 10,
        severity: 'LOW',
        isTriggered: false
      }
    ];
  }

  private triggerRule(rules: FraudRule[], id: string, score: number): void {
    const rule = rules.find(r => r.id === id);
    if (rule) {
      rule.isTriggered = true;
    }
  }

  private hasDocument(documents: any[], keyword: string): boolean {
    return documents.some(doc => 
      (doc.fileName && doc.fileName.toUpperCase().includes(keyword.toUpperCase())) ||
      (doc.documentType && doc.documentType.toUpperCase().includes(keyword.toUpperCase()))
    );
  }

  private getRiskLevel(score: number): 'LOW' | 'MEDIUM' | 'HIGH' {
    if (score >= 70) return 'HIGH';
    if (score >= 35) return 'MEDIUM';
    return 'LOW';
  }
}
