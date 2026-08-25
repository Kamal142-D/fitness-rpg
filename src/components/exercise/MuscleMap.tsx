import { View } from 'react-native';
import Svg, { Circle, Ellipse, G, Path, Rect } from 'react-native-svg';

import { Text } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';

type Level = 'primary' | 'secondary' | 'inactive';

function normalize(value: string) {
  return value
    .toLowerCase()
    .replace(/[^a-z]+/g, ' ')
    .trim();
}

function levelFor(primary: string, secondary: string[], aliases: string[]): Level {
  const normalizedPrimary = normalize(primary);
  const normalizedSecondary = secondary.map(normalize);
  if (aliases.some((alias) => normalizedPrimary.includes(alias))) return 'primary';
  if (normalizedSecondary.some((muscle) => aliases.some((alias) => muscle.includes(alias)))) {
    return 'secondary';
  }
  return 'inactive';
}

function regionColor(level: Level) {
  if (level === 'primary') return Palette.primary;
  if (level === 'secondary') return Palette.accent;
  return Palette.surface3;
}

function BodyBase() {
  return (
    <>
      <Circle cx="60" cy="18" r="9" />
      <Path d="M48 31 Q60 26 72 31 L78 70 Q70 82 69 105 L51 105 Q50 82 42 70 Z" />
      <Path d="M44 34 Q34 39 28 56 L20 84 Q19 90 24 92 Q29 93 31 86 L40 62 L49 47 Z" />
      <Path d="M76 34 Q86 39 92 56 L100 84 Q101 90 96 92 Q91 93 89 86 L80 62 L71 47 Z" />
      <Path d="M52 102 L48 132 L43 171 Q42 178 48 180 Q54 180 56 173 L61 137 L63 108 Z" />
      <Path d="M68 102 L72 132 L77 171 Q78 178 72 180 Q66 180 64 173 L59 137 L57 108 Z" />
    </>
  );
}

function HumanFigure({
  primary,
  secondary,
  back,
}: {
  primary: string;
  secondary: string[];
  back?: boolean;
}) {
  const region = (aliases: string[]) => regionColor(levelFor(primary, secondary, aliases));

  return (
    <Svg width="120" height="190" viewBox="0 0 120 190">
      <G fill={Palette.surface3} stroke={Palette.hairlineStrong} strokeWidth="1.2">
        <BodyBase />
      </G>

      {back ? (
        <G>
          <Ellipse cx="48" cy="42" rx="8" ry="6" fill={region(['rear delt', 'shoulder'])} />
          <Ellipse cx="72" cy="42" rx="8" ry="6" fill={region(['rear delt', 'shoulder'])} />
          <Path
            d="M47 46 Q60 38 73 46 L70 65 Q60 73 50 65 Z"
            fill={region(['upper back', 'back'])}
          />
          <Path d="M49 52 L42 70 L51 86 L58 64 Z" fill={region(['lat', 'back'])} />
          <Path d="M71 52 L78 70 L69 86 L62 64 Z" fill={region(['lat', 'back'])} />
          <Rect
            x="53"
            y="69"
            width="14"
            height="24"
            rx="5"
            fill={region(['lower back', 'posterior chain'])}
          />
          <Ellipse
            cx="53"
            cy="104"
            rx="9"
            ry="8"
            fill={region(['glute', 'hip', 'posterior chain'])}
          />
          <Ellipse
            cx="67"
            cy="104"
            rx="9"
            ry="8"
            fill={region(['glute', 'hip', 'posterior chain'])}
          />
          <Path
            d="M48 117 Q55 111 59 120 L56 148 L47 148 Z"
            fill={region(['hamstring', 'leg', 'posterior chain'])}
          />
          <Path
            d="M72 117 Q65 111 61 120 L64 148 L73 148 Z"
            fill={region(['hamstring', 'leg', 'posterior chain'])}
          />
          <Ellipse cx="49" cy="160" rx="6" ry="12" fill={region(['calf', 'leg'])} />
          <Ellipse cx="71" cy="160" rx="6" ry="12" fill={region(['calf', 'leg'])} />
          <Ellipse cx="34" cy="60" rx="5" ry="14" fill={region(['tricep', 'arm'])} />
          <Ellipse cx="86" cy="60" rx="5" ry="14" fill={region(['tricep', 'arm'])} />
        </G>
      ) : (
        <G>
          <Ellipse cx="47" cy="42" rx="9" ry="6" fill={region(['shoulder', 'delt'])} />
          <Ellipse cx="73" cy="42" rx="9" ry="6" fill={region(['shoulder', 'delt'])} />
          <Path d="M48 43 Q60 37 72 43 L68 61 Q60 67 52 61 Z" fill={region(['chest', 'pec'])} />
          <Rect
            x="53"
            y="62"
            width="14"
            height="29"
            rx="5"
            fill={region(['core', 'ab', 'mobility'])}
          />
          <Ellipse cx="34" cy="59" rx="5" ry="14" fill={region(['bicep', 'arm'])} />
          <Ellipse cx="86" cy="59" rx="5" ry="14" fill={region(['bicep', 'arm'])} />
          <Ellipse cx="25" cy="80" rx="4" ry="11" fill={region(['forearm'])} />
          <Ellipse cx="95" cy="80" rx="4" ry="11" fill={region(['forearm'])} />
          <Path
            d="M48 114 Q55 108 59 119 L56 147 L47 147 Z"
            fill={region(['quad', 'leg', 'adductor'])}
          />
          <Path
            d="M72 114 Q65 108 61 119 L64 147 L73 147 Z"
            fill={region(['quad', 'leg', 'adductor'])}
          />
          <Ellipse cx="49" cy="160" rx="6" ry="12" fill={region(['calf', 'leg'])} />
          <Ellipse cx="71" cy="160" rx="6" ry="12" fill={region(['calf', 'leg'])} />
        </G>
      )}
    </Svg>
  );
}

export function MuscleMap({ primary, secondary }: { primary: string; secondary: string[] }) {
  return (
    <View style={{ gap: Spacing.md }}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-around' }}>
        <View style={{ alignItems: 'center' }}>
          <Text variant="caption" color="tertiary">
            FRONT
          </Text>
          <HumanFigure primary={primary} secondary={secondary} />
        </View>
        <View style={{ alignItems: 'center' }}>
          <Text variant="caption" color="tertiary">
            BACK
          </Text>
          <HumanFigure primary={primary} secondary={secondary} back />
        </View>
      </View>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.md }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
          <View
            style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: Palette.primary }}
          />
          <Text variant="caption" color="secondary">
            PRIMARY · {primary}
          </Text>
        </View>
        {secondary.length ? (
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
            <View
              style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: Palette.accent }}
            />
            <Text variant="caption" color="secondary">
              SECONDARY · {secondary.join(', ')}
            </Text>
          </View>
        ) : null}
      </View>
    </View>
  );
}
