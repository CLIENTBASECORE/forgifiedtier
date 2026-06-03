// Paginates through PostgREST in 1000-row chunks (server max).
// `makeQuery` receives the from/to range and returns a Supabase query.
export async function fetchAllRows<T>(
  makeQuery: (from: number, to: number) => PromiseLike<{ data: unknown; error: { message: string } | null }>
): Promise<{ data: T[]; error: { message: string } | null }> {
  const pageSize = 1000;
  const out: T[] = [];
  let from = 0;
  while (true) {
    const { data, error } = await makeQuery(from, from + pageSize - 1);
    if (error) return { data: out, error };
    const rows = (data ?? []) as T[];
    out.push(...rows);
    if (rows.length < pageSize) break;
    from += pageSize;
    // safety cap
    if (from > 200000) break;
  }
  return { data: out, error: null };
}
