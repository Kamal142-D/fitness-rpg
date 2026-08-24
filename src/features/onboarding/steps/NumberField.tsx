import { useState } from 'react';

import { TextField } from '@/components/ui';

export interface NumberFieldProps {
  label: string;
  value: number | null;
  onChange: (value: number | null) => void;
  placeholder?: string;
  error?: string;
  /** Allow decimals (default true). */
  decimal?: boolean;
}

/**
 * TextField adapter for numeric draft fields. Keeps a local text buffer so
 * partial input (e.g. "12.") edits cleanly, and reports null when empty.
 */
export function NumberField({
  label,
  value,
  onChange,
  placeholder,
  error,
  decimal = true,
}: NumberFieldProps) {
  const [text, setText] = useState(value == null ? '' : String(value));

  return (
    <TextField
      label={label}
      placeholder={placeholder}
      value={text}
      keyboardType={decimal ? 'decimal-pad' : 'number-pad'}
      error={error}
      onChangeText={(t) => {
        setText(t);
        const trimmed = t.trim();
        if (trimmed === '') {
          onChange(null);
          return;
        }
        const n = Number(trimmed);
        onChange(Number.isFinite(n) ? n : null);
      }}
    />
  );
}
