import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AccountService } from './account.service';
import { CardService } from './card.service';
import { Account, Card } from './settings.models';

@Component({
  selector: 'app-cards-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './cards-panel.html'
})
export class CardsPanel implements OnInit {
  private readonly cardService = inject(CardService);
  private readonly accountService = inject(AccountService);
  private readonly formBuilder = inject(FormBuilder);

  readonly cards = signal<Card[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly editing = signal<Card | null>(null);
  readonly showForm = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    accountId: ['', Validators.required],
    closingDay: [null as number | null, [Validators.required, Validators.min(1), Validators.max(31)]],
    dueDay: [null as number | null, [Validators.required, Validators.min(1), Validators.max(31)]]
  });

  ngOnInit(): void {
    this.load();
    this.accountService.list().subscribe((accounts) => this.accounts.set(accounts));
  }

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ name: '', accountId: '', closingDay: null, dueDay: null });
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(card: Card): void {
    this.editing.set(card);
    this.form.reset({
      name: card.name,
      accountId: card.accountId,
      closingDay: card.closingDay,
      dueDay: card.dueDay
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
    const payload = {
      name: raw.name,
      accountId: raw.accountId,
      closingDay: raw.closingDay!,
      dueDay: raw.dueDay!
    };
    const editing = this.editing();
    const request$ = editing
      ? this.cardService.update(editing.id, payload)
      : this.cardService.create(payload);
    request$.subscribe({
      next: () => {
        this.cancel();
        this.load();
      },
      error: () => this.errorMessage.set('Erro ao salvar o cartão')
    });
  }

  remove(card: Card): void {
    this.cardService.delete(card.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o cartão')
    });
  }

  private load(): void {
    this.cardService.list().subscribe((cards) => this.cards.set(cards));
  }
}
