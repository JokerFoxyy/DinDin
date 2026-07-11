import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { CategoriesPanel } from './categories-panel';
import { CategoryService } from './category.service';
import { Category } from './settings.models';

describe('CategoriesPanel', () => {
  let fixture: ComponentFixture<CategoriesPanel>;
  let component: CategoriesPanel;
  let categoryService: jasmine.SpyObj<CategoryService>;

  const mercado: Category = { id: '1', name: 'Mercado', icon: '🛒', color: '#3fb950', kind: 'EXPENSE' };

  beforeEach(async () => {
    categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['list', 'create', 'update', 'delete']);
    categoryService.list.and.returnValue(of([mercado]));

    await TestBed.configureTestingModule({
      imports: [CategoriesPanel],
      providers: [{ provide: CategoryService, useValue: categoryService }]
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesPanel);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should list categories on init with tag styling', () => {
    const tag = fixture.nativeElement.querySelector('.tag');
    expect(tag.textContent).toContain('Mercado');
    expect(fixture.nativeElement.textContent).toContain('Gasto');
  });

  it('should create a category converting empty icon to null', () => {
    categoryService.create.and.returnValue(of(mercado));

    component.openCreate();
    component.form.setValue({ name: 'Lazer', icon: '', color: '#4f8ef7', kind: 'EXPENSE' });
    component.submit();

    expect(categoryService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ name: 'Lazer', icon: null })
    );
  });

  it('should not call the service when name is empty', () => {
    component.openCreate();
    component.form.setValue({ name: '', icon: '', color: '#4f8ef7', kind: 'EXPENSE' });

    component.submit();

    expect(categoryService.create).not.toHaveBeenCalled();
  });

  it('should show duplicate message when API returns 409', () => {
    categoryService.create.and.returnValue(throwError(() => ({ status: 409 })));

    component.openCreate();
    component.form.setValue({ name: 'Mercado', icon: '', color: '#4f8ef7', kind: 'EXPENSE' });
    component.submit();

    expect(component.errorMessage()).toBe('Categoria já existe');
  });

  it('should update the category when editing', () => {
    categoryService.update.and.returnValue(of(mercado));

    component.openEdit(mercado);
    component.form.patchValue({ name: 'Supermercado' });
    component.submit();

    expect(categoryService.update).toHaveBeenCalledWith('1', jasmine.objectContaining({ name: 'Supermercado' }));
  });

  it('should delete the category and reload the list', () => {
    categoryService.delete.and.returnValue(of(void 0));

    component.remove(mercado);

    expect(categoryService.delete).toHaveBeenCalledWith('1');
    expect(categoryService.list).toHaveBeenCalledTimes(2);
  });
});
