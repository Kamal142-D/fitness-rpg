import { createHash } from 'node:crypto';
import { writeFileSync } from 'node:fs';

const outputPath = new URL('../supabase/migrations/20260825070200_import_exercises_dataset.sql', import.meta.url);
const datasetUrl = 'https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/data/exercises.json';
const response = await fetch(datasetUrl);
if (!response.ok) throw new Error(`Dataset download failed: ${response.status}`);
const source = await response.json();
if (!Array.isArray(source) || source.length === 0) throw new Error('Dataset is empty or invalid');
if (source.some(row => !row.id || !row.name || !row.body_part || !row.equipment || !row.target)) {
  throw new Error('Dataset contains records missing required mapped fields');
}
if (new Set(source.map(row => row.id)).size !== source.length) throw new Error('Dataset contains duplicate source IDs');

const sql = (value) => value == null ? 'null' : `'${String(value).replaceAll("'", "''")}'`;
const array = (values = []) => `array[${values.map(sql).join(',')}]::text[]`;
const uuid = (id) => {
  const h = createHash('md5').update(`hasaneyldrm/exercises-dataset:${id}`).digest('hex');
  return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20,32)}`;
};
const category = (row) => ({
  chest: 'chest', back: 'back', shoulders: 'shoulders', cardio: 'cardio', waist: 'core',
  'upper arms': 'arms', 'lower arms': 'arms', 'upper legs': 'legs', 'lower legs': 'legs', neck: 'full_body',
}[row.body_part] ?? 'full_body');
const type = (row) => row.body_part === 'cardio' ? 'cardio' :
  row.equipment === 'body weight' ? 'bodyweight' : 'strength';

const seen = new Set();
const rows = [];
let skipped = 0;
for (const row of source) {
  const duplicateKey = [row.name.trim().toLowerCase(), row.equipment, row.target, row.body_part].join('|');
  if (seen.has(duplicateKey)) { skipped++; continue; }
  seen.add(duplicateKey);
  const base = 'https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/';
  rows.push(`(${sql(uuid(row.id))}::uuid,${sql(row.name.trim())},${sql(category(row))},${sql(row.target)},${array(row.secondary_muscles)},${sql(row.equipment)},${sql(type(row))},${row.body_part === 'cardio' ? 'false' : 'true'},${array([])},${sql(row.body_part)},${sql(row.target)},${array(row.instruction_steps?.en ?? [])},${sql(base + row.image)},${sql(base + row.gif_url)},'hasaneyldrm/exercises-dataset',${sql(row.id)},${sql(row.attribution)})`);
}

const chunks = [];
for (let i = 0; i < rows.length; i += 100) {
  chunks.push(`insert into public.exercises
  (id,name,category,primary_muscle_group,secondary_muscle_groups,equipment,exercise_type,ranking_enabled,
   aliases,body_part,target_muscle,instructions,image_url,video_url,source,source_id,attribution)
values
${rows.slice(i, i + 100).join(',\n')}
on conflict (source,source_id) where source is not null and source_id is not null do update set
  name=excluded.name, category=excluded.category, primary_muscle_group=excluded.primary_muscle_group,
  secondary_muscle_groups=excluded.secondary_muscle_groups, equipment=excluded.equipment,
  exercise_type=excluded.exercise_type, ranking_enabled=excluded.ranking_enabled,
  body_part=excluded.body_part, target_muscle=excluded.target_muscle, instructions=excluded.instructions,
  image_url=excluded.image_url, video_url=excluded.video_url, attribution=excluded.attribution;`);
}

writeFileSync(outputPath, `-- Generated from https://github.com/hasaneyldrm/exercises-dataset\n-- Source records: ${source.length}; imported: ${rows.length}; exact duplicates skipped: ${skipped}.\n-- Media remains © Gym visual; attribution is retained per row.\n\n${chunks.join('\n\n')}\n`);
console.log(JSON.stringify({source: source.length, imported: rows.length, skipped, uniqueIds: new Set(source.map(x => x.id)).size}));
