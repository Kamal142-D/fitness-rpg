import { getExercise, type ExerciseFrame } from '@bryllim/workout-guide';
import { useEffect, useMemo, useState } from 'react';
import { Image, View } from 'react-native';

import { Text } from '@/components/ui';
import { Palette } from '@/constants/theme';
import { exerciseFrameUrl } from '@/features/exercises';

const FRAME_SEQUENCE: ExerciseFrame['index'][] = [1, 2, 3, 2];

interface ExerciseArtworkProps {
  slug: string;
  size: number;
  animated?: boolean;
  tintColor?: string;
}

/** Transparent exercise art with an optional gentle three-frame movement loop. */
export function ExerciseArtwork({
  slug,
  size,
  animated = false,
  tintColor = Palette.textPrimary,
}: ExerciseArtworkProps) {
  const exercise = useMemo(() => getExercise(slug), [slug]);
  const [sequenceIndex, setSequenceIndex] = useState(0);

  useEffect(() => {
    if (!exercise) return;

    const urls = exercise.frames
      .map((frame) => exerciseFrameUrl(exercise, frame.index))
      .filter((url): url is string => !!url);
    for (const url of urls) void Image.prefetch(url);

    if (!animated) return;
    const timer = setInterval(
      () => setSequenceIndex((current) => (current + 1) % FRAME_SEQUENCE.length),
      720,
    );
    return () => clearInterval(timer);
  }, [animated, exercise]);

  if (!exercise) {
    return (
      <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
        <Text variant="caption" color="tertiary">
          NO ART
        </Text>
      </View>
    );
  }

  const frameIndex = animated ? FRAME_SEQUENCE[sequenceIndex] : 1;
  const uri = exerciseFrameUrl(exercise, frameIndex);

  return (
    <Image
      accessibilityLabel={`${exercise.name}, movement frame ${frameIndex}`}
      source={uri ? { uri } : undefined}
      resizeMode="contain"
      fadeDuration={120}
      tintColor={tintColor}
      style={{ width: size, height: size }}
    />
  );
}
