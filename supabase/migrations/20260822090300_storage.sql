-- ============================================================================
-- Storage: private bucket for body-assessment uploads (PLAN.txt §5).
--
-- Files are scoped by user id as the first path segment:
--     assessments/{auth.uid()}/{filename}
-- Storage policies ensure a user can only read/write objects under their own
-- folder. The bucket is private (not publicly listable/servable).
-- ============================================================================

insert into storage.buckets (id, name, public)
values ('assessments', 'assessments', false)
on conflict (id) do nothing;

-- Read own files
create policy "assessments_select_own"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'assessments'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Upload into own folder
create policy "assessments_insert_own"
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'assessments'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Update own files
create policy "assessments_update_own"
  on storage.objects for update to authenticated
  using (
    bucket_id = 'assessments'
    and (storage.foldername(name))[1] = auth.uid()::text
  )
  with check (
    bucket_id = 'assessments'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Delete own files
create policy "assessments_delete_own"
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'assessments'
    and (storage.foldername(name))[1] = auth.uid()::text
  );
