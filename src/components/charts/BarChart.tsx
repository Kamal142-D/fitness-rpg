import { useState } from 'react';
import { type LayoutChangeEvent, View } from 'react-native';
import Svg, { Rect, Text as SvgText } from 'react-native-svg';

import { Text } from '@/components/ui';
import { Fonts, Palette } from '@/constants/theme';
import type { SeriesPoint } from '@/features/analytics/types';

export interface BarChartProps {
  data: SeriesPoint[];
  height?: number;
  color?: string;
  /** Accessible summary title for the chart. */
  title?: string;
}

/** A compact bar chart (SVG). Themed, responsive to its container width. */
export function BarChart({ data, height = 140, color = Palette.primary, title }: BarChartProps) {
  const [width, setWidth] = useState(0);
  const onLayout = (e: LayoutChangeEvent) => setWidth(e.nativeEvent.layout.width);

  const padTop = 6;
  const padBottom = 18;
  const max = Math.max(1, ...data.map((d) => d.value));
  const n = data.length;
  const gap = 6;
  const barW = n > 0 ? Math.max(2, (width - gap * (n - 1)) / n) : 0;
  const plotH = height - padTop - padBottom;
  const labelEvery = Math.max(1, Math.ceil(n / 5));

  return (
    <View
      onLayout={onLayout}
      accessibilityRole="image"
      accessibilityLabel={
        title ? `${title}. Latest ${data[n - 1]?.value ?? 0}, peak ${max}.` : undefined
      }
    >
      {width > 0 ? (
        <Svg width={width} height={height}>
          {data.map((d, i) => {
            const bh = (d.value / max) * plotH;
            const x = i * (barW + gap);
            const y = height - padBottom - bh;
            return (
              <Rect
                key={i}
                x={x}
                y={y}
                width={barW}
                height={Math.max(0, bh)}
                rx={3}
                fill={d.value > 0 ? color : Palette.surface3}
              />
            );
          })}
          {data.map((d, i) =>
            i % labelEvery === 0 ? (
              <SvgText
                key={`l${i}`}
                x={i * (barW + gap) + barW / 2}
                y={height - 5}
                fill={Palette.textTertiary}
                fontSize={9}
                fontFamily={Fonts.sans}
                textAnchor="middle"
              >
                {d.label}
              </SvgText>
            ) : null,
          )}
        </Svg>
      ) : (
        <View style={{ height }} />
      )}
      {data.every((d) => d.value === 0) ? (
        <Text variant="caption" color="tertiary" style={{ marginTop: 4 }}>
          No data yet
        </Text>
      ) : null}
    </View>
  );
}
