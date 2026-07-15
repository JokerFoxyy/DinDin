import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Importer } from './importer';
import { ImportService } from './import.service';
import { AccountService } from '../settings/account.service';
import { CategoryService } from '../settings/category.service';
import { ImportCommitResult, ImportPreview } from './import.models';
import { Account, Category } from '../settings/settings.models';

describe('Importer', () => {
  let fixture: ComponentFixture<Importer>;
  let component: Importer;
  let importService: jasmine.SpyObj<ImportService>;

  const accounts: Account[] = [
    { id: 'a1', name: 'Uniclass', type: 'CHECKING', closingDay: null, dueDay: null }
  ];
  const categories: Category[] = [
    { id: 'c1', name: 'Mercado', icon: '🛒', color: '#3fb950', kind: 'EXPENSE' }
  ];
  const preview: ImportPreview = {
    rows: [
      { sheet: 'Julho', section: 'FIXOS', description: 'Netflix', date: '2026-07-01', accountName: 'Nubank',
        categoryName: 'Assinaturas', amount: 39.9, type: 'EXPENSE' }
    ],
    unmatchedAccounts: ['Nubank'],
    unmatchedCategories: ['Assinaturas']
  };
  const commitResult: ImportCommitResult = {
    transactionsCreated: 1, transactionsSkippedAsDuplicate: 0, accountsCreated: 1, categoriesCreated: 1
  };

  function fakeFileEvent(): Event {
    const file = new File(['x'], 'planilha.xlsx');
    const input = document.createElement('input');
    input.type = 'file';
    Object.defineProperty(input, 'files', { value: [file] });
    return { target: input } as unknown as Event;
  }

  beforeEach(async () => {
    importService = jasmine.createSpyObj<ImportService>('ImportService', ['preview', 'commit']);
    importService.preview.and.returnValue(of(preview));
    importService.commit.and.returnValue(of(commitResult));
    const accountService = jasmine.createSpyObj<AccountService>('AccountService', ['list']);
    accountService.list.and.returnValue(of(accounts));
    const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['list']);
    categoryService.list.and.returnValue(of(categories));

    await TestBed.configureTestingModule({
      imports: [Importer],
      providers: [
        { provide: ImportService, useValue: importService },
        { provide: AccountService, useValue: accountService },
        { provide: CategoryService, useValue: categoryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Importer);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should not analyze without a selected file', () => {
    component.analyze();

    expect(importService.preview).not.toHaveBeenCalled();
  });

  it('should analyze the file and populate mapping rows for unmatched names', () => {
    component.onFileSelected(fakeFileEvent());

    component.analyze();

    expect(importService.preview).toHaveBeenCalledWith(jasmine.any(File), 2026);
    expect(component.preview()?.rows.length).toBe(1);
    expect(component.accountMappings()).toEqual([{ name: 'Nubank', mode: 'create', existingId: '', createType: 'CHECKING' }]);
    expect(component.categoryMappings()).toEqual([{ name: 'Assinaturas', mode: 'create', existingId: '', createKind: 'EXPENSE' }]);
  });

  it('should update mapping rows when the user changes mode', () => {
    component.onFileSelected(fakeFileEvent());
    component.analyze();

    component.updateAccountMapping(0, { mode: 'existing', existingId: 'a1' });

    expect(component.accountMappings()[0].mode).toBe('existing');
    expect(component.accountMappings()[0].existingId).toBe('a1');
  });

  it('should confirm the import with the resolved mapping', () => {
    component.onFileSelected(fakeFileEvent());
    component.analyze();
    component.updateAccountMapping(0, { mode: 'existing', existingId: 'a1' });
    component.updateCategoryMapping(0, { mode: 'existing', existingId: 'c1' });

    component.confirm();

    expect(importService.commit).toHaveBeenCalledWith(jasmine.any(File), 2026, {
      accounts: { Nubank: { existingAccountId: 'a1', createType: null } },
      categories: { Assinaturas: { existingCategoryId: 'c1', createKind: null } }
    });
    expect(component.result()).toEqual(commitResult);
    expect(component.preview()).toBeNull();
  });

  it('should show backend error message when preview fails', () => {
    importService.preview.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Arquivo inválido' } })));
    component.onFileSelected(fakeFileEvent());

    component.analyze();

    expect(component.errorMessage()).toContain('Arquivo inválido');
  });

  it('should show backend error message when commit fails', () => {
    importService.commit.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Erro ao importar' } })));
    component.onFileSelected(fakeFileEvent());
    component.analyze();

    component.confirm();

    expect(component.errorMessage()).toContain('Erro ao importar');
  });
});
