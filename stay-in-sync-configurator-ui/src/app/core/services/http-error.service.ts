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
    this.messageService.add({ severity: 'error', summary: response.error.errorTitle, detail: response.error.errorMessage, life: 3000});
  }



}
