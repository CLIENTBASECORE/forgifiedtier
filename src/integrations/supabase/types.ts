export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.5"
  }
  public: {
    Tables: {
      app_settings: {
        Row: {
          key: string
          updated_at: string
          updated_by: string | null
          value: Json
        }
        Insert: {
          key: string
          updated_at?: string
          updated_by?: string | null
          value: Json
        }
        Update: {
          key?: string
          updated_at?: string
          updated_by?: string | null
          value?: Json
        }
        Relationships: []
      }
      news_posts: {
        Row: {
          body: string
          created_at: string
          created_by: string | null
          id: string
          is_important: boolean
          title: string
        }
        Insert: {
          body: string
          created_at?: string
          created_by?: string | null
          id?: string
          is_important?: boolean
          title: string
        }
        Update: {
          body?: string
          created_at?: string
          created_by?: string | null
          id?: string
          is_important?: boolean
          title?: string
        }
        Relationships: []
      }
      tier_entries: {
        Row: {
          created_at: string
          gamemode: Database["public"]["Enums"]["gamemode"]
          glow: boolean
          id: string
          is_manual: boolean
          mode_kind: string
          player_name: string
          points: number
          position: number
          region: string
          tier: Database["public"]["Enums"]["tier_rank"]
          title: string | null
        }
        Insert: {
          created_at?: string
          gamemode: Database["public"]["Enums"]["gamemode"]
          glow?: boolean
          id?: string
          is_manual?: boolean
          mode_kind?: string
          player_name: string
          points?: number
          position?: number
          region?: string
          tier: Database["public"]["Enums"]["tier_rank"]
          title?: string | null
        }
        Update: {
          created_at?: string
          gamemode?: Database["public"]["Enums"]["gamemode"]
          glow?: boolean
          id?: string
          is_manual?: boolean
          mode_kind?: string
          player_name?: string
          points?: number
          position?: number
          region?: string
          tier?: Database["public"]["Enums"]["tier_rank"]
          title?: string | null
        }
        Relationships: []
      }
      tier_history: {
        Row: {
          change_type: string
          changed_at: string
          changed_by: string | null
          gamemode: Database["public"]["Enums"]["gamemode"]
          id: string
          mode_kind: string
          new_tier: Database["public"]["Enums"]["tier_rank"] | null
          old_tier: Database["public"]["Enums"]["tier_rank"] | null
          player_name: string
        }
        Insert: {
          change_type: string
          changed_at?: string
          changed_by?: string | null
          gamemode: Database["public"]["Enums"]["gamemode"]
          id?: string
          mode_kind?: string
          new_tier?: Database["public"]["Enums"]["tier_rank"] | null
          old_tier?: Database["public"]["Enums"]["tier_rank"] | null
          player_name: string
        }
        Update: {
          change_type?: string
          changed_at?: string
          changed_by?: string | null
          gamemode?: Database["public"]["Enums"]["gamemode"]
          id?: string
          mode_kind?: string
          new_tier?: Database["public"]["Enums"]["tier_rank"] | null
          old_tier?: Database["public"]["Enums"]["tier_rank"] | null
          player_name?: string
        }
        Relationships: []
      }
      tiertagger_mods: {
        Row: {
          file_name: string
          file_path: string
          mode_kind: string
          size_bytes: number
          updated_at: string
          updated_by: string | null
        }
        Insert: {
          file_name: string
          file_path: string
          mode_kind: string
          size_bytes?: number
          updated_at?: string
          updated_by?: string | null
        }
        Update: {
          file_name?: string
          file_path?: string
          mode_kind?: string
          size_bytes?: number
          updated_at?: string
          updated_by?: string | null
        }
        Relationships: []
      }
      user_roles: {
        Row: {
          created_at: string
          id: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          role?: Database["public"]["Enums"]["app_role"]
          user_id?: string
        }
        Relationships: []
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      can_manage_tiers: { Args: { _user_id: string }; Returns: boolean }
      claim_ownership: { Args: never; Returns: boolean }
      clear_user_roles: { Args: { _user_id: string }; Returns: undefined }
      has_role: {
        Args: {
          _role: Database["public"]["Enums"]["app_role"]
          _user_id: string
        }
        Returns: boolean
      }
      is_staff: { Args: { _user_id: string }; Returns: boolean }
      list_users_with_roles: {
        Args: never
        Returns: {
          created_at: string
          email: string
          roles: Database["public"]["Enums"]["app_role"][]
          user_id: string
        }[]
      }
      set_user_role: {
        Args: {
          _role: Database["public"]["Enums"]["app_role"]
          _user_id: string
        }
        Returns: undefined
      }
    }
    Enums: {
      app_role: "owner" | "admin" | "tester"
      gamemode:
        | "boxing"
        | "bedfight"
        | "fireballfight"
        | "sumo"
        | "nodebuff"
        | "uhc"
        | "spleef"
        | "sword"
        | "nethop"
        | "axe"
        | "pot"
        | "vanilla"
        | "smp"
        | "mace"
      tier_rank:
        | "S+"
        | "S-"
        | "A+"
        | "A-"
        | "B+"
        | "B-"
        | "C+"
        | "C-"
        | "D+"
        | "D-"
        | "F+"
        | "F-"
        | "HT1"
        | "LT1"
        | "HT2"
        | "LT2"
        | "HT3"
        | "LT3"
        | "HT4"
        | "LT4"
        | "HT5"
        | "LT5"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      app_role: ["owner", "admin", "tester"],
      gamemode: [
        "boxing",
        "bedfight",
        "fireballfight",
        "sumo",
        "nodebuff",
        "uhc",
        "spleef",
        "sword",
        "nethop",
        "axe",
        "pot",
        "vanilla",
        "smp",
        "mace",
      ],
      tier_rank: [
        "S+",
        "S-",
        "A+",
        "A-",
        "B+",
        "B-",
        "C+",
        "C-",
        "D+",
        "D-",
        "F+",
        "F-",
        "HT1",
        "LT1",
        "HT2",
        "LT2",
        "HT3",
        "LT3",
        "HT4",
        "LT4",
        "HT5",
        "LT5",
      ],
    },
  },
} as const
