export class QueryLog {
  query: string;
  start: string;
  dur1QueryRelevantMillis: number;
  dur2QueryGeneralMillis: number;
  dur3PrepareQueryMillis: number;
  dur4BuildGraphMillis: number;
  end: string;
  durationMillis: number;
  success: boolean;
  exceptionMessage: string;
  contextRequests: number;
  graphInMemCacheHitRatio: number;
  localFileCacheHitRatio: number;
  contexts: number;
  quads: number;
}
