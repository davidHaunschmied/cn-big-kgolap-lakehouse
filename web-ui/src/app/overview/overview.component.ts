import {Component, OnInit} from '@angular/core';
import {combineLatest, Observable} from 'rxjs';
import {Level} from '../model/level';
import {LakehouseStats} from '../model/lakehouse-stats';
import {QueryLog} from '../model/query-log';
import {LakehouseService} from '../services/lakehouse.service';
import {DataType} from '../model/data-type';
import {shareReplay} from 'rxjs/operators';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.scss']
})
export class OverviewComponent implements OnInit {

  selectedFiles: File[];
  cubeResult: string;
  queryError: string;
  kgOlapQuery: string = 'SELECT * ROLLUP ON topic_all, location_all, time_all';
  levels$: Observable<Map<string, Level[]>>;
  lakehouseStats: LakehouseStats;
  queryLogs: QueryLog [];
  fileUploadsPerMin: any [];
  completionsPerMin: any [];
  showSyntax = false;
  cubeResultSizeMb: number;

  constructor(private readonly lakehouseService: LakehouseService) {
  }

  ngOnInit() {
    this.cubeResult = null;
    this.loadQueryLogs();
    this.loadFileIngestionLogs();
    this.loadLevels();
  }

  handleFileInput(event: Event) {
    // @ts-ignore
    this.selectedFiles = event.target.files;
  }

  uploadFiles() {
    const uploads: Observable<string>[] = [];
    for (const file of this.selectedFiles) {
      uploads.push(this.lakehouseService.upload(file, DataType.AIXM));
    }

    combineLatest(uploads).subscribe((values: string []) => {
      console.log(`Loaded ${values.length} files into the data lake house.`);
    });

  }

  query() {
    this.queryError = '';
    this.cubeResult = null;
    this.lakehouseService.queryCube(this.kgOlapQuery).subscribe(value => {
        this.cubeResult = value?.slice(0, 5000);
        this.cubeResultSizeMb = new Blob([value]).size / (1024 * 1024);
      },
      error => {
        console.log(error);
        try {
          this.queryError = JSON.parse(error.error).message;
        } catch (e) {
          this.queryError = "Request failed and error could not be parsed! Check network log for details."
        }
      });
  }

  public loadInfos() {
    this.lakehouseService.getStats().subscribe(value => {
      this.lakehouseStats = value;
    });
  }

  public loadQueryLogs() {
    this.lakehouseService.getQueryLogs().subscribe(value => {
      this.queryLogs = value.sort((a, b) => new Date(b.start).valueOf() - new Date(a.start).valueOf()).slice(10, 12);
    });
  }

  public loadFileIngestionLogs() {
    this.lakehouseService.getFileIngestionLogs().subscribe(logs => {
      const uploads = new Map();
      const completions = new Map();
      logs.forEach(log => {
        const startMinute = Date.parse(log.start) / 1000 / 60 >> 0;
        const endMinute = Date.parse(log.end) / 1000 / 60 >> 0;

        if (uploads.has(startMinute)) {
          uploads.set(startMinute, uploads.get(startMinute) + 1);
        } else {
          uploads.set(startMinute, 1);
        }

        if (completions.has(endMinute)) {
          if (endMinute > 0) {
            completions.set(endMinute, completions.get(endMinute) + 1);
          } else {
            completions.set(startMinute, 0);
          }
        } else {
          if (endMinute > 0) {
            completions.set(endMinute, 1);
          } else {
            completions.set(startMinute, 0);
          }
        }
      });

      const fileUploadsPerMin = [];
      const completionsPerMin = [];
      uploads.forEach((value, key) => fileUploadsPerMin.push([this.minToMilli(key), value]));
      completions.forEach((value, key) => completionsPerMin.push([this.minToMilli(key), value]));
      this.fileUploadsPerMin = fileUploadsPerMin.sort();
      this.completionsPerMin = completionsPerMin.sort();
    });
  }

  private minToMilli(minute: number) {
    return minute * 60 * 1000;
  }

  getKeys(map: Map<string, Level []>): string [] {
    return Array.from(map.keys());
  }

  getValue(map: Map<string, Level[]>, dimension: string) {
    return map.get(dimension);
  }

  calculateVirtualGraphSize(lakehouseStats: LakehouseStats) {
    if (!lakehouseStats.totalIndices) {
      return 0;
    }
    return (lakehouseStats.totalStoredFileSizeMB / 1024 / lakehouseStats.totalFiles) * lakehouseStats.totalIndices;
  }

  private loadLevels() {
    this.levels$ = this.lakehouseService.getLevels().pipe(shareReplay(1));
  }

  toString(queryLog: QueryLog) {
    return 'Query ' + queryLog.query + ' took ' + queryLog.durationMillis / 1000 + ' ms' + new Date(queryLog.start).toISOString() + ': ';
  }
}
