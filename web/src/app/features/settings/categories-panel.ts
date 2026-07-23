import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoryStore } from '../../core/state/category.store';
import { CATEGORY_KIND_LABELS, Category, CategoryKind } from './settings.models';

@Component({
  selector: 'app-categories-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './categories-panel.html'
})
export class CategoriesPanel implements OnInit {
  private readonly categoryStore = inject(CategoryStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly categories = this.categoryStore.categories;
  readonly editing = signal<Category | null>(null);
  readonly showForm = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly kindLabels = CATEGORY_KIND_LABELS;
  readonly kindOptions = Object.keys(CATEGORY_KIND_LABELS) as CategoryKind[];

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    icon: [''],
    color: ['#4f8ef7'],
    kind: ['EXPENSE' as CategoryKind, Validators.required]
  });

  ngOnInit(): void {
    this.categoryStore.ensureLoaded();
  }

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ name: '', icon: '', color: '#4f8ef7', kind: 'EXPENSE' });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(category: Category): void {
    this.editing.set(category);
    this.form.reset({
      name: category.name,
      icon: category.icon ?? '',
      color: category.color ?? '#4f8ef7',
      kind: category.kind
    });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  cancel(): void {
    this.showForm.set(false);
    this.editing.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = { name: raw.name, icon: raw.icon || null, color: raw.color || null, kind: raw.kind };
    const editing = this.editing();
    const request$ = editing
      ? this.categoryStore.update(editing.id, payload)
      : this.categoryStore.create(payload);
    request$.subscribe({
      next: () => this.cancel(),
      error: (error) =>
        this.errorMessage.set(error?.status === 409 ? 'Categoria já existe' : 'Erro ao salvar a categoria')
    });
  }

  remove(category: Category): void {
    this.categoryStore.delete(category.id).subscribe({
      error: () => this.errorMessage.set(
        'Não foi possível excluir: a categoria pode ter transações ou orçamentos vinculados.')
    });
  }
}
