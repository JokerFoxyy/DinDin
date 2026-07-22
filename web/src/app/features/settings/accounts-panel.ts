import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AccountService } from './account.service';
import { ACCOUNT_TYPE_LABELS, Account, AccountType } from './settings.models';

@Component({
  selector: 'app-accounts-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './accounts-panel.html'
})
export class AccountsPanel implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly formBuilder = inject(FormBuilder);

  readonly accounts = signal<Account[]>([]);
  readonly editing = signal<Account | null>(null);
  readonly showForm = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly typeLabels = ACCOUNT_TYPE_LABELS;
  readonly typeOptions = Object.keys(ACCOUNT_TYPE_LABELS) as AccountType[];

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    type: ['CHECKING' as AccountType, Validators.required]
  });

  ngOnInit(): void {
    this.load();
  }

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ name: '', type: 'CHECKING' });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(account: Account): void {
    this.editing.set(account);
    this.form.reset({ name: account.name, type: account.type });
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
    const payload = this.form.getRawValue();
    const editing = this.editing();
    const request$ = editing
      ? this.accountService.update(editing.id, payload)
      : this.accountService.create(payload);
    request$.subscribe({
      next: () => {
        this.cancel();
        this.load();
      },
      error: () => this.errorMessage.set('Erro ao salvar a conta')
    });
  }

  remove(account: Account): void {
    this.accountService.delete(account.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir a conta')
    });
  }

  private load(): void {
    this.accountService.list().subscribe((accounts) => this.accounts.set(accounts));
  }
}
