-- Minima Todo: tasks table schema.
-- Run once in the Supabase SQL editor for the shared project.
-- Mirrors the fields already used by app/src/main/java/com/samarthkulkarni/minimatodo/data/Task.kt

create table public.tasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  text text not null,
  is_completed boolean default false,
  start_date timestamptz,
  end_date timestamptz,
  completed_at timestamptz,
  updated_at timestamptz default now(),
  deleted_at timestamptz    -- soft delete, so sync can reconcile offline deletes
);

alter table public.tasks enable row level security;

create policy "Users can manage their own tasks"
  on public.tasks for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create index tasks_user_id_idx on public.tasks(user_id);
create index tasks_updated_at_idx on public.tasks(updated_at);
