import { Injectable } from '@angular/core';
import { SiuClaim } from './siu.service';
import { environment } from '../../../../environments/environment';

export interface PatternAnalysisResult {
  clusterFound: boolean;
  linkedClaims: string[];
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  aiSynthesis: string;
}

@Injectable({
  providedIn: 'root'
})
export class InvestigationToolsService {

  private readonly groqApiUrl = 'https://api.groq.com/openai/v1/chat/completions';
  private readonly groqModel = 'llama-3.1-8b-instant';

  private get apiKey(): string {
    return (environment as any).groqApiKey || '';
  }

  /**
   * Call Groq's OpenAI-compatible chat completions API.
   */
  private async callGroq(systemPrompt: string, userContent: string): Promise<string> {
    const response = await fetch(this.groqApiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`
      },
      body: JSON.stringify({
        model: this.groqModel,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userContent }
        ],
        temperature: 0.3,
        max_tokens: 512
      })
    });

    if (!response.ok) {
      const errorBody = await response.text();
      throw new Error(`Groq API error ${response.status}: ${errorBody}`);
    }

    const data = await response.json();
    return data.choices?.[0]?.message?.content ?? '';
  }

  /**
   * STEP 2: AI-Enhanced Pattern Analysis (Cross-Claim Clustering)
   * Feeds the list of active SIU claims to Groq/Llama to detect fraud rings.
   */
  async runPatternAnalysis(claims: SiuClaim[]): Promise<PatternAnalysisResult> {
    const claimDataPayload = claims.map(c => ({
      id: c.claimId,
      customer: c.customerName,
      policy: c.policyNumber,
      state: c.state,
      priority: c.priority,
      amount: c.claimAmount,
      suspiciousIndicators: c.suspiciousIndicators
    }));

    try {
      const systemPrompt = `You are an expert Insurance Fraud Special Investigator (SIU).
Analyze a batch of active claims to perform Cross-Claim Clustering. 
Look for linked claims sharing: identical customer names across different policies, 
clustering of high-value amounts on the same date, or overlapping suspiciousIndicators.
Determine if a "Coordinated Fraud Ring" pattern exists.
Respond ONLY with valid JSON (no markdown, no code fences):
{"clusterFound":true/false,"linkedClaims":["id1","id2"],"riskLevel":"HIGH"|"MEDIUM"|"LOW","aiSynthesis":"2-3 sentence professional summary."}`;

      const rawOutput = await this.callGroq(systemPrompt, `Claims Data: ${JSON.stringify(claimDataPayload)}`);

      // Strip any accidental markdown fences
      const jsonStr = rawOutput.replace(/```json/gi, '').replace(/```/gi, '').trim();
      return JSON.parse(jsonStr) as PatternAnalysisResult;

    } catch (error) {
      console.warn('Groq Pattern Analysis failed. Falling back to Heuristics.', error);
      return this.heuristicFallback(claims);
    }
  }

  /**
   * STEP 4: Risk Assessment Narrative
   */
  async runRiskAssessment(claim: SiuClaim): Promise<string> {
    try {
      const systemPrompt = `You are a senior insurance claims SIU director using professional insurance lexicon.`;
      const userContent = `Summarize the risk profile of this claim in exactly 2 sentences. 
Claim fraud score: ${claim.fraudScore}%. Suspicious indicators: ${JSON.stringify(claim.suspiciousIndicators)}.`;

      return await this.callGroq(systemPrompt, userContent);
    } catch (e) {
      return 'Unable to generate AI risk synthesis at this time.';
    }
  }

  /**
   * STEP 5: Executive Report Summary
   */
  async generateExecutiveSummary(claims: SiuClaim[]): Promise<string> {
    try {
      const systemPrompt = `You are an insurance fraud analytics director writing for executive management.`;
      const userContent = `Write a 1-paragraph executive summary highlighting total financial exposure 
and the most common fraud indicator across these ${claims.length} SIU investigation cases.
Data: ${JSON.stringify(claims.map(c => ({ id: c.claimId, amount: c.claimAmount, score: c.fraudScore, indicators: c.suspiciousIndicators })))}`;

      return await this.callGroq(systemPrompt, userContent);
    } catch (e) {
      return 'System default report generated. Unable to connect to AI analysis service.';
    }
  }

  /**
   * Heuristic fallback when Groq API is unavailable.
   */
  private heuristicFallback(claims: SiuClaim[]): PatternAnalysisResult {
    const duplicateNames = claims.filter(c =>
      claims.filter(inner => inner.customerName === c.customerName).length > 1
    );

    if (duplicateNames.length > 0) {
      return {
        clusterFound: true,
        linkedClaims: duplicateNames.map(c => c.claimId),
        riskLevel: 'HIGH',
        aiSynthesis: `Heuristic Analysis: Detected ${duplicateNames.length} claims filed under identical customer names across different policies. This pattern is highly indicative of entity cloning or organized fraud ring activity.`
      };
    }

    const highValueClaims = claims.filter(c => (c.claimAmount ?? 0) > 300000);
    if (highValueClaims.length >= 2) {
      return {
        clusterFound: true,
        linkedClaims: highValueClaims.map(c => c.claimId),
        riskLevel: 'MEDIUM',
        aiSynthesis: `Heuristic Analysis: Identified ${highValueClaims.length} high-value claims exceeding $300K. Statistically anomalous clustering of large-value claims warrants coordinated investigation to rule out inflated loss staging.`
      };
    }

    return {
      clusterFound: false,
      linkedClaims: [],
      riskLevel: 'LOW',
      aiSynthesis: 'No cross-claim correlations or professional fraud ring patterns detected across the current active investigation pool. Claims appear to be isolated incidents.'
    };
  }
}
