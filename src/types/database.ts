/**
 * Supabase database types.
 *
 * Generated from the applied migrations in supabase/migrations. On a machine
 * with Docker + the Supabase CLI, regenerate the canonical version with:
 *
 *   npx supabase gen types typescript --local > src/types/database.ts
 *
 * (or --project-id <ref> against your linked project). The shape here matches
 * the CLI output so the switch is seamless.
 */

export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[];

export type Database = {
  public: {
    Tables: {
      body_assessments: {
        Row: {
          id: string;
          user_id: string;
          weight_kg: number | null;
          body_fat_percent: number | null;
          skeletal_muscle_mass_kg: number | null;
          body_fat_mass_kg: number | null;
          visceral_fat_level: number | null;
          inbody_score: number | null;
          assessment_date: string;
          source: string;
          source_file_path: string | null;
          created_at: string;
        };
        Insert: {
          id?: string;
          user_id: string;
          weight_kg?: number | null;
          body_fat_percent?: number | null;
          skeletal_muscle_mass_kg?: number | null;
          body_fat_mass_kg?: number | null;
          visceral_fat_level?: number | null;
          inbody_score?: number | null;
          assessment_date?: string;
          source?: string;
          source_file_path?: string | null;
          created_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          weight_kg?: number | null;
          body_fat_percent?: number | null;
          skeletal_muscle_mass_kg?: number | null;
          body_fat_mass_kg?: number | null;
          visceral_fat_level?: number | null;
          inbody_score?: number | null;
          assessment_date?: string;
          source?: string;
          source_file_path?: string | null;
          created_at?: string;
        };
        Relationships: [];
      };
      exercise_user_stats: {
        Row: {
          user_id: string;
          exercise_id: string;
          best_weight_kg: number | null;
          best_reps: number | null;
          best_estimated_1rm_kg: number | null;
          best_volume_kg: number | null;
          rank_score: number | null;
          exercise_rank: string | null;
          qualifying_session_count: number;
          total_sessions: number;
          last_performed_at: string | null;
          updated_at: string;
        };
        Insert: {
          user_id: string;
          exercise_id: string;
          best_weight_kg?: number | null;
          best_reps?: number | null;
          best_estimated_1rm_kg?: number | null;
          best_volume_kg?: number | null;
          rank_score?: number | null;
          exercise_rank?: string | null;
          qualifying_session_count?: number;
          total_sessions?: number;
          last_performed_at?: string | null;
          updated_at?: string;
        };
        Update: {
          user_id?: string;
          exercise_id?: string;
          best_weight_kg?: number | null;
          best_reps?: number | null;
          best_estimated_1rm_kg?: number | null;
          best_volume_kg?: number | null;
          rank_score?: number | null;
          exercise_rank?: string | null;
          qualifying_session_count?: number;
          total_sessions?: number;
          last_performed_at?: string | null;
          updated_at?: string;
        };
        Relationships: [];
      };
      exercises: {
        Row: {
          id: string;
          name: string;
          category: string;
          primary_muscle_group: string | null;
          secondary_muscle_groups: string[];
          equipment: string | null;
          exercise_type: string;
          ranking_enabled: boolean;
          created_at: string;
        };
        Insert: {
          id?: string;
          name: string;
          category: string;
          primary_muscle_group?: string | null;
          secondary_muscle_groups?: string[];
          equipment?: string | null;
          exercise_type: string;
          ranking_enabled?: boolean;
          created_at?: string;
        };
        Update: {
          id?: string;
          name?: string;
          category?: string;
          primary_muscle_group?: string | null;
          secondary_muscle_groups?: string[];
          equipment?: string | null;
          exercise_type?: string;
          ranking_enabled?: boolean;
          created_at?: string;
        };
        Relationships: [];
      };
      personal_records: {
        Row: {
          id: string;
          user_id: string;
          exercise_id: string;
          workout_set_id: string | null;
          record_type: string;
          previous_value: number | null;
          new_value: number;
          achieved_at: string;
        };
        Insert: {
          id?: string;
          user_id: string;
          exercise_id: string;
          workout_set_id?: string | null;
          record_type: string;
          previous_value?: number | null;
          new_value: number;
          achieved_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          exercise_id?: string;
          workout_set_id?: string | null;
          record_type?: string;
          previous_value?: number | null;
          new_value?: number;
          achieved_at?: string;
        };
        Relationships: [];
      };
      player_progression: {
        Row: {
          user_id: string;
          level: number;
          current_xp: number;
          lifetime_xp: number;
          hunter_score: number;
          hunter_rank: string;
          strength_score: number;
          physique_score: number;
          endurance_score: number;
          discipline_score: number;
          current_streak_days: number;
          longest_streak_days: number;
          updated_at: string;
        };
        Insert: {
          user_id: string;
          level?: number;
          current_xp?: number;
          lifetime_xp?: number;
          hunter_score?: number;
          hunter_rank?: string;
          strength_score?: number;
          physique_score?: number;
          endurance_score?: number;
          discipline_score?: number;
          current_streak_days?: number;
          longest_streak_days?: number;
          updated_at?: string;
        };
        Update: {
          user_id?: string;
          level?: number;
          current_xp?: number;
          lifetime_xp?: number;
          hunter_score?: number;
          hunter_rank?: string;
          strength_score?: number;
          physique_score?: number;
          endurance_score?: number;
          discipline_score?: number;
          current_streak_days?: number;
          longest_streak_days?: number;
          updated_at?: string;
        };
        Relationships: [];
      };
      profiles: {
        Row: {
          id: string;
          email: string | null;
          display_name: string | null;
          date_of_birth: string | null;
          sex: string | null;
          height_cm: number | null;
          current_weight_kg: number | null;
          experience_level: string | null;
          fitness_goal: string | null;
          training_days_per_week: number | null;
          training_location: string | null;
          preferred_workout_minutes: number | null;
          onboarding_completed: boolean;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id: string;
          email?: string | null;
          display_name?: string | null;
          date_of_birth?: string | null;
          sex?: string | null;
          height_cm?: number | null;
          current_weight_kg?: number | null;
          experience_level?: string | null;
          fitness_goal?: string | null;
          training_days_per_week?: number | null;
          training_location?: string | null;
          preferred_workout_minutes?: number | null;
          onboarding_completed?: boolean;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          email?: string | null;
          display_name?: string | null;
          date_of_birth?: string | null;
          sex?: string | null;
          height_cm?: number | null;
          current_weight_kg?: number | null;
          experience_level?: string | null;
          fitness_goal?: string | null;
          training_days_per_week?: number | null;
          training_location?: string | null;
          preferred_workout_minutes?: number | null;
          onboarding_completed?: boolean;
          created_at?: string;
          updated_at?: string;
        };
        Relationships: [];
      };
      quests: {
        Row: {
          id: string;
          name: string;
          description: string | null;
          type: string;
          requirement_type: string;
          requirement_value: number;
          xp_reward: number;
          gold_reward: number;
          active: boolean;
          created_at: string;
        };
        Insert: {
          id?: string;
          name: string;
          description?: string | null;
          type: string;
          requirement_type: string;
          requirement_value: number;
          xp_reward?: number;
          gold_reward?: number;
          active?: boolean;
          created_at?: string;
        };
        Update: {
          id?: string;
          name?: string;
          description?: string | null;
          type?: string;
          requirement_type?: string;
          requirement_value?: number;
          xp_reward?: number;
          gold_reward?: number;
          active?: boolean;
          created_at?: string;
        };
        Relationships: [];
      };
      user_quests: {
        Row: {
          id: string;
          user_id: string;
          quest_id: string;
          progress: number;
          completed: boolean;
          claimed: boolean;
          assigned_at: string;
          expires_at: string | null;
          completed_at: string | null;
          claimed_at: string | null;
        };
        Insert: {
          id?: string;
          user_id: string;
          quest_id: string;
          progress?: number;
          completed?: boolean;
          claimed?: boolean;
          assigned_at?: string;
          expires_at?: string | null;
          completed_at?: string | null;
          claimed_at?: string | null;
        };
        Update: {
          id?: string;
          user_id?: string;
          quest_id?: string;
          progress?: number;
          completed?: boolean;
          claimed?: boolean;
          assigned_at?: string;
          expires_at?: string | null;
          completed_at?: string | null;
          claimed_at?: string | null;
        };
        Relationships: [];
      };
      workout_exercises: {
        Row: {
          id: string;
          session_id: string;
          exercise_id: string;
          order_index: number;
          exercise_score: number | null;
          exercise_rank_at_time: string | null;
          performance_grade: string | null;
          notes: string | null;
        };
        Insert: {
          id?: string;
          session_id: string;
          exercise_id: string;
          order_index: number;
          exercise_score?: number | null;
          exercise_rank_at_time?: string | null;
          performance_grade?: string | null;
          notes?: string | null;
        };
        Update: {
          id?: string;
          session_id?: string;
          exercise_id?: string;
          order_index?: number;
          exercise_score?: number | null;
          exercise_rank_at_time?: string | null;
          performance_grade?: string | null;
          notes?: string | null;
        };
        Relationships: [];
      };
      workout_sessions: {
        Row: {
          id: string;
          user_id: string;
          template_id: string | null;
          name: string | null;
          gate_difficulty: string | null;
          started_at: string;
          completed_at: string | null;
          duration_seconds: number | null;
          total_volume_kg: number | null;
          completion_score: number | null;
          progress_score: number | null;
          quality_score: number | null;
          gate_score: number | null;
          gate_clear_rank: string | null;
          xp_earned: number;
          status: string;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          user_id: string;
          template_id?: string | null;
          name?: string | null;
          gate_difficulty?: string | null;
          started_at?: string;
          completed_at?: string | null;
          duration_seconds?: number | null;
          total_volume_kg?: number | null;
          completion_score?: number | null;
          progress_score?: number | null;
          quality_score?: number | null;
          gate_score?: number | null;
          gate_clear_rank?: string | null;
          xp_earned?: number;
          status?: string;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          template_id?: string | null;
          name?: string | null;
          gate_difficulty?: string | null;
          started_at?: string;
          completed_at?: string | null;
          duration_seconds?: number | null;
          total_volume_kg?: number | null;
          completion_score?: number | null;
          progress_score?: number | null;
          quality_score?: number | null;
          gate_score?: number | null;
          gate_clear_rank?: string | null;
          xp_earned?: number;
          status?: string;
          created_at?: string;
          updated_at?: string;
        };
        Relationships: [];
      };
      workout_sets: {
        Row: {
          id: string;
          workout_exercise_id: string;
          set_number: number;
          weight_kg: number | null;
          reps: number | null;
          rpe: number | null;
          duration_seconds: number | null;
          distance_meters: number | null;
          is_warmup: boolean;
          is_completed: boolean;
          is_pr: boolean;
          estimated_1rm_kg: number | null;
          completed_at: string | null;
        };
        Insert: {
          id?: string;
          workout_exercise_id: string;
          set_number: number;
          weight_kg?: number | null;
          reps?: number | null;
          rpe?: number | null;
          duration_seconds?: number | null;
          distance_meters?: number | null;
          is_warmup?: boolean;
          is_completed?: boolean;
          is_pr?: boolean;
          estimated_1rm_kg?: number | null;
          completed_at?: string | null;
        };
        Update: {
          id?: string;
          workout_exercise_id?: string;
          set_number?: number;
          weight_kg?: number | null;
          reps?: number | null;
          rpe?: number | null;
          duration_seconds?: number | null;
          distance_meters?: number | null;
          is_warmup?: boolean;
          is_completed?: boolean;
          is_pr?: boolean;
          estimated_1rm_kg?: number | null;
          completed_at?: string | null;
        };
        Relationships: [];
      };
      workout_template_exercises: {
        Row: {
          id: string;
          template_id: string;
          exercise_id: string;
          order_index: number;
          target_sets: number | null;
          target_reps_min: number | null;
          target_reps_max: number | null;
          target_rpe: number | null;
          rest_seconds: number | null;
        };
        Insert: {
          id?: string;
          template_id: string;
          exercise_id: string;
          order_index: number;
          target_sets?: number | null;
          target_reps_min?: number | null;
          target_reps_max?: number | null;
          target_rpe?: number | null;
          rest_seconds?: number | null;
        };
        Update: {
          id?: string;
          template_id?: string;
          exercise_id?: string;
          order_index?: number;
          target_sets?: number | null;
          target_reps_min?: number | null;
          target_reps_max?: number | null;
          target_rpe?: number | null;
          rest_seconds?: number | null;
        };
        Relationships: [];
      };
      workout_templates: {
        Row: {
          id: string;
          user_id: string | null;
          name: string;
          description: string | null;
          estimated_duration_minutes: number | null;
          difficulty: string | null;
          is_system_template: boolean;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          user_id?: string | null;
          name: string;
          description?: string | null;
          estimated_duration_minutes?: number | null;
          difficulty?: string | null;
          is_system_template?: boolean;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string | null;
          name?: string;
          description?: string | null;
          estimated_duration_minutes?: number | null;
          difficulty?: string | null;
          is_system_template?: boolean;
          created_at?: string;
          updated_at?: string;
        };
        Relationships: [];
      };
    };
    Views: {
      [_ in never]: never;
    };
    Functions: {
      complete_workout: {
        Args: { payload: Json };
        Returns: string;
      };
      apply_workout_results: {
        Args: { p_session_id: string; p_prs: Json; p_stats: Json };
        Returns: undefined;
      };
      ensure_active_quests: {
        Args: Record<string, never>;
        Returns: undefined;
      };
      record_workout_for_quests: {
        Args: { p_session_id: string };
        Returns: undefined;
      };
      apply_session_progression: {
        Args: { p_session_id: string; p: Json };
        Returns: undefined;
      };
      claim_quest: {
        Args: { p_user_quest_id: string };
        Returns: undefined;
      };
    };
    Enums: {
      [_ in never]: never;
    };
    CompositeTypes: {
      [_ in never]: never;
    };
  };
};

type PublicSchema = Database['public'];

export type Tables<T extends keyof PublicSchema['Tables']> = PublicSchema['Tables'][T]['Row'];
export type TablesInsert<T extends keyof PublicSchema['Tables']> =
  PublicSchema['Tables'][T]['Insert'];
export type TablesUpdate<T extends keyof PublicSchema['Tables']> =
  PublicSchema['Tables'][T]['Update'];
