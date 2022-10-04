import {Component, Input, OnInit} from '@angular/core';
import * as Highcharts from 'highcharts';
import HighchartsMore from 'highcharts/highcharts-more';

HighchartsMore(Highcharts);

Highcharts.setOptions({
  time: {
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
  }
});

@Component({
  selector: 'app-file-ingestion-chart',
  templateUrl: './file-ingestion-chart.component.html',
  styleUrls: ['./file-ingestion-chart.component.scss']
})
export class FileIngestionChartComponent implements OnInit {

  @Input()
  ingestionsPerMinute: any [];

  constructor() {
  }

  ngOnInit(): void {
    this.createChartLineCompleted();
  }

  private createChartLineCompleted(): void {
    const chart = Highcharts.chart('chart-line-completed', {
      chart: {
        type: 'column',
        zoomType: 'x'
      },
      time: {
        useUTC: false
      },
      title: {
        text: 'Indexed files per minute (Completed ingestions)',
      },
      credits: {
        enabled: false,
      },
      legend: {
        enabled: false,
      },
      yAxis: {
        title: {
          text: 'Indexed files',
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
        data: this.ingestionsPerMinute,
      }],
    } as any);
  }


}
