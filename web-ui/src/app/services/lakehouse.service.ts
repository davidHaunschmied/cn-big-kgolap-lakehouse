import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Level} from '../model/level';
import {map} from 'rxjs/operators';
import {LakehouseStats} from '../model/lakehouse-stats';
import {LakehouseStatsDto} from './dtos/lakehouse-stats-dto';
import {DataType} from '../model/data-type';
import {QueryLog} from '../model/query-log';
import {environment} from '../../environments/environment';
import {FileIngestionLog} from '../model/file-ingestion-log';

@Injectable({
  providedIn: 'root'
})
export class LakehouseService {

  constructor(private readonly http: HttpClient) {
  }

  public upload(file: File, type: DataType): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    // @ts-ignore
    return this.http.post<string>(environment.surfaceBaseUrl, formData, {responseType: 'text'});
  }

  getStats(): Observable<LakehouseStats> {
    return this.http.get<LakehouseStatsDto>(environment.surfaceBaseUrl + '/stats')
      .pipe(map(value => {
        if (!value) {
          return null;
        }
        return new LakehouseStats(value.totalIndices, value.totalContexts, value.totalFiles,
          value.totalStoredFileSizeBytes);
      }));
  }

  queryCube(kgOlapQuery: string): Observable<any> {
    return this.http.post<any>(environment.surfaceBaseUrl + '/cube', {query: kgOlapQuery},
      // @ts-ignore
      {responseType: 'text'}).pipe(map(value => {
      return value;
    }));
  }

  getLevels(): Observable<Map<string, Level[]>> {
    return this.http.get<Map<string, Level[]>>(environment.surfaceBaseUrl + '/levels')
      .pipe(map(value => {
        const levels = new Map<string, Level[]>();
        for (const entry of Object.keys(value)) {
          levels.set(entry, value[entry]);
        }
        return levels;
      }));
  }

  getQueryLogs(): Observable<QueryLog []> {
    return this.http.get<QueryLog []>(environment.surfaceBaseUrl + '/querylogs');
    /*.pipe(map(value => {
        if (!value) {
            return null;
        }
        return new QueryLogs(value.totalIndices, value.totalContexts, value.totalFiles,
            value.totalStoredFileSizeBytes);
    }));*/
  }

  getFileIngestionLogs(): Observable<FileIngestionLog []> {
    return this.http.get<FileIngestionLog []>(environment.surfaceBaseUrl + '/ingestionlogs');
  }
}
