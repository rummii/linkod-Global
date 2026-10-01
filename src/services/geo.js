const rad = value => value * Math.PI / 180;
export function distanceKm(lat1, lng1, lat2, lng2) {
  const a = Math.sin(rad(lat2-lat1)/2)**2 + Math.cos(rad(lat1))*Math.cos(rad(lat2))*Math.sin(rad(lng2-lng1)/2)**2;
  return 6371.0088 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0,a))));
}
export function nearby(db, job, now, radius = 15) {
  // Exact matching handles dateline and poles. Add a spatial prefilter when the provider pool grows.
  return db.prepare(`SELECT p.* FROM providers p JOIN provider_skills s ON s.provider_id=p.id
    WHERE p.active=1 AND p.approved=1 AND s.category_id=? AND p.last_seen>=?
    AND p.lat IS NOT NULL AND p.lng IS NOT NULL
    AND NOT EXISTS(SELECT 1 FROM jobs j WHERE j.provider_id=p.id AND j.status IN ('accepted','in_transit','in_progress'))
    AND NOT EXISTS(SELECT 1 FROM offers o WHERE o.job_id=? AND o.provider_id=p.id)`)
    .all(job.category_id, now-600000, job.id)
    .map(p => ({...p, distance_km: distanceKm(job.lat,job.lng,p.lat,p.lng)}))
    .filter(p => p.distance_km<=Math.min(radius,p.radius_km))
    .sort((a,b) => a.distance_km-b.distance_km || a.id-b.id);
}
