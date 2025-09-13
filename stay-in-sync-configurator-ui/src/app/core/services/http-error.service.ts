import {Injectable} from '@angular/core';
import {MessageService} from 'primeng/api';
import {HttpErrorResponse} from '@angular/common/http';

/**
 *  Service responsible for handling Error responses from api
 *
 */
@Injectable({
  providedIn: 'root'
})
export class HttpErrorService {

  constructor(readonly messageService: MessageService) { }

  handleError(response: HttpErrorResponse) {
    const err: any = response?.error || {};
    const summary = err.errorTitle || err.title || `HTTP ${response.status}`;
    const detail = err.errorMessage || err.message || (typeof err === 'string' ? err : 'Request failed');
    this.messageService.add({ severity: 'error', summary, detail, life: 3000});
  }



}
