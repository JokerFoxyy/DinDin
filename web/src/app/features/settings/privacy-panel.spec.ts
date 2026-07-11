import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PrivacyPanel } from './privacy-panel';
import { PrivacyService } from './privacy.service';
import { AuthService } from '../../core/auth/auth.service';

describe('PrivacyPanel', () => {
  let fixture: ComponentFixture<PrivacyPanel>;
  let component: PrivacyPanel;
  let privacyService: jasmine.SpyObj<PrivacyService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    privacyService = jasmine.createSpyObj<PrivacyService>('PrivacyService', ['exportData', 'deleteAccount']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['clearSession']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [PrivacyPanel],
      providers: [
        { provide: PrivacyService, useValue: privacyService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PrivacyPanel);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should export data and trigger a download', () => {
    privacyService.exportData.and.returnValue(of(new Blob(['{}'])));
    const anchor = document.createElement('a');
    spyOn(anchor, 'click');
    spyOn(document, 'createElement').and.returnValue(anchor);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    spyOn(URL, 'revokeObjectURL');

    component.exportData();

    expect(privacyService.exportData).toHaveBeenCalled();
    expect(anchor.download).toBe('dindin-meus-dados.json');
    expect(anchor.click).toHaveBeenCalled();
  });

  it('should show an error when export fails', () => {
    privacyService.exportData.and.returnValue(throwError(() => new Error('boom')));

    component.exportData();

    expect(component.errorMessage()).toContain('exportar');
  });

  it('should not delete until the confirmation word is typed', () => {
    component.deleteAccount();

    expect(privacyService.deleteAccount).not.toHaveBeenCalled();
    expect(component.canDelete()).toBeFalse();

    component.onConfirmInput('EXCLUIR');

    expect(component.canDelete()).toBeTrue();
  });

  it('should delete the account then clear the session and go to login', () => {
    component.onConfirmInput('EXCLUIR');
    privacyService.deleteAccount.and.returnValue(of(void 0));

    component.deleteAccount();

    expect(privacyService.deleteAccount).toHaveBeenCalled();
    expect(authService.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should show an error when deletion fails', () => {
    component.onConfirmInput('EXCLUIR');
    privacyService.deleteAccount.and.returnValue(throwError(() => new Error('boom')));

    component.deleteAccount();

    expect(component.errorMessage()).toContain('excluir');
    expect(component.deleting()).toBeFalse();
  });
});
