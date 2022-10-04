import {Component, Input, OnInit} from '@angular/core';
import * as Highcharts from 'highcharts';

Highcharts.setOptions({
  time: {
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
  }
});

@Component({
  selector: 'app-file-upload-chart',
  templateUrl: './file-upload-chart.component.html',
  styleUrls: ['./file-upload-chart.component.scss']
})
export class FileUploadChartComponent implements OnInit {

  @Input()
  ingestionLogs: any [];

  constructor() {
  }

  ngOnInit(): void {
    this.createChartLineUpload();
  }


  private createChartLineUpload(): void {

    const chart = Highcharts.chart('chart-line-upload', {
      chart: {
        type: 'column',
        zoomType: 'x'
      },
      time: {
        useUTC: false
      },
      title: {
        text: 'Uploaded files per minute (Completed REST calls)',
      },
      credits: {
        enabled: false,
      },
      legend: {
        enabled: false,
      },
      yAxis: {
        title: {
          text: 'Uploaded files',
        }
      },
      xAxis: {
        type: 'datetime',
        //ordinal: true
      },
      tooltip: {
        headerFormat: `<div>Date: {point.key}</div>`,
        pointFormat: `<div>{series.name}: {point.y}</div>`,
        shared: true,
        useHTML: true,
      },
      series: [{
        name: 'Amount',
        data: this.ingestionLogs,
      }],
    } as any);
  }
}
