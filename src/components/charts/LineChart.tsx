import { useState } from 'react';
import { type LayoutChangeEvent, View } from 'react-native';
import Svg, { Circle, Polyline, Text as SvgText } from 'react-native-svg';

import { Text } from '@/components/ui';
import { Fonts, Palette } from '@/constants/theme';

export interface LinePoint {
  label: string;
  value: number;
}

export interface LineChartProps {
  data: LinePoint[];
  height?: number;
  color?: string;
  title?: string;
  unit?: string;
}

/** A compact line chart (SVG) with an auto-scaled y-range, for trends. */
export function LineChart({
  data,
  height = 150,
  color = Palette.primary,
  title,
  unit = '',
}: LineChartProps) {
  const [width, setWidth] = useState(0);
  const onLayout = (e: LayoutChangeEvent) => setWidth(e.nativeEvent.layout.width);

  if (data.length < 2) {
    return (
      <View onLayout={onLayout} style={{ height }}>
        <Text variant="caption" color="tertiary">
          Not enough data yet
        </Text>
      </View>
    );
  }

  const padTop = 10;
  const padBottom = 20;
  const padX = 6;
  const values = data.map((d) => d.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const plotH = height - padTop - padBottom;
  const plotW = Math.max(1, width - padX * 2);

  const x = (i: number) => padX + (i / (data.length - 1)) * plotW;
  const y = (v: number) => padTop + (1 - (v - min) / range) * plotH;
  const points = data.map((d, i) => `${x(i)},${y(d.value)}`).join(' ');

  return (
    <View
      onLayout={onLayout}
      accessibilityRole="image"
      accessibilityLabel={
        title
          ? `${title}. From ${data[0].value}${unit} to ${data[data.length - 1].value}${unit}.`
          : undefined
      }
    >
      {width > 0 ? (
        <Svg width={width} height={height}>
          <Polyline
            points={points}
            fill="none"
            stroke={color}
            strokeWidth={2}
            strokeLinejoin="round"
          />
          {data.map((d, i) => (
            <Circle key={i} cx={x(i)} cy={y(d.value)} r={2.5} fill={color} />
          ))}
          <SvgText x={padX} y={12} fill={Palette.textTertiary} fontSize={9} fontFamily={Fonts.sans}>
            {`${Math.round(max)}${unit}`}
          </SvgText>
          <SvgText
            x={padX}
            y={height - 6}
            fill={Palette.textTertiary}
            fontSize={9}
            fontFamily={Fonts.sans}
          >
            {`${Math.round(min)}${unit}`}
          </SvgText>
        </Svg>
      ) : (
        <View style={{ height }} />
      )}
    </View>
  );
}
