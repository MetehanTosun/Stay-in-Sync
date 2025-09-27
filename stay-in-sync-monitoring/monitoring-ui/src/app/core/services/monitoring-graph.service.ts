import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class MonitoringGraphService{
  constructor(private http: HttpClient) {}

  getMonitoringGraphData() {
    return this.http.get<any>('/api/monitoringgraph');
  }

}
