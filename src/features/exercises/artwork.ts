import type { Exercise, ExerciseFrame } from '@bryllim/workout-guide';

const WORKOUT_GUIDE_VERSION = '1.0.0';
const WORKOUT_GUIDE_CDN = `https://cdn.jsdelivr.net/npm/@bryllim/workout-guide@${WORKOUT_GUIDE_VERSION}/`;

/** Immutable CDN URL for one transparent Workout Guide frame. */
export function exerciseFrameUrl(exercise: Exercise, frameIndex: ExerciseFrame['index']) {
  const frame = exercise.frames.find((candidate) => candidate.index === frameIndex);
  return frame ? `${WORKOUT_GUIDE_CDN}${frame.path}` : null;
}

export const workoutGuideCredit =
  'Original exercise artwork by Everkinetic, expanded by Bryl Lim · CC BY-SA 4.0';
