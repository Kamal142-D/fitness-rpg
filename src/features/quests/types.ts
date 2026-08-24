export interface UserQuestView {
  /** user_quests row id (target of claim). */
  id: string;
  questId: string;
  name: string;
  description: string | null;
  type: string; // 'daily' | 'weekly' | ...
  requirementValue: number;
  progress: number;
  completed: boolean;
  claimed: boolean;
  xpReward: number;
  expiresAt: string | null;
}
