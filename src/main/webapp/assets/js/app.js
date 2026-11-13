document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.querySelector('[data-nav-toggle]');
  toggle?.addEventListener('click', () => {
    const open = toggle.getAttribute('aria-expanded') !== 'true';
    toggle.setAttribute('aria-expanded', String(open));
    document.getElementById('main-navigation').classList.toggle('is-open', open);
  });
  const path = location.pathname;
  document.querySelectorAll('.site-links a').forEach(link => {
    const target = new URL(link.href);
    if (!target.hash && target.pathname === path) link.setAttribute('aria-current', 'page');
  });
  document.querySelectorAll('[data-password-toggle]').forEach(button => {
    button.addEventListener('click', () => {
      const field = document.getElementById(button.dataset.passwordToggle);
      const show = field.type === 'password';
      field.type = show ? 'text' : 'password';
      button.textContent = show ? 'Hide' : 'Show';
      button.setAttribute('aria-pressed', String(show));
    });
  });
  const search = document.querySelector('[data-journey-search]');
  if (search) {
    const from = search.elements.departureStation, to = search.elements.arrivalStation;
    const date = search.elements.travelDate;
    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
    date.min = today;
    date.value ||= today;
    const validate = () => to.setCustomValidity(from.value && from.value === to.value ? 'Choose a different destination station.' : '');
    from.addEventListener('change', validate); to.addEventListener('change', validate);
    document.querySelector('[data-swap-stations]')?.addEventListener('click', () => {
      [from.value, to.value] = [to.value, from.value]; validate();
    });
    document.querySelectorAll('[data-date-offset]').forEach(button => button.addEventListener('click', () => {
      const day = new Date(); day.setDate(day.getDate()+Number(button.dataset.dateOffset));
      date.value = `${day.getFullYear()}-${String(day.getMonth()+1).padStart(2,'0')}-${String(day.getDate()).padStart(2,'0')}`;
    }));
  }
  const sort = document.querySelector('[data-sort-journeys]');
  sort?.addEventListener('change', () => {
    const list = document.querySelector('[data-journey-list]');
    [...list.children].sort((a,b) => Number(a.dataset[sort.value])-Number(b.dataset[sort.value])).forEach(card => list.append(card));
  });
  document.querySelectorAll('[data-ticket-filter]').forEach(button => button.addEventListener('click', () => {
    document.querySelectorAll('[data-ticket-filter]').forEach(tab => {tab.classList.remove('active'); tab.setAttribute('aria-pressed','false');});
    button.classList.add('active'); button.setAttribute('aria-pressed','true');
    let shown = 0;
    document.querySelectorAll('[data-ticket-status]').forEach(card => {
      card.hidden = button.dataset.ticketFilter !== 'all' && card.dataset.ticketStatus !== button.dataset.ticketFilter;
      if (!card.hidden) shown++;
    });
    const empty = document.querySelector('[data-filter-empty]');
    if (empty) empty.hidden = shown > 0 || document.querySelectorAll('[data-ticket-status]').length === 0;
  }));
  const adminSearch = document.querySelector('[data-booking-search]');
  document.querySelectorAll('[data-group-passengers]').forEach(group => {
    const count=group.querySelector('[data-passenger-count]');
    const fields=group.querySelector('[data-passenger-fields]');
    const saved=[];
    count.addEventListener('change',()=>{
      fields.querySelectorAll('input').forEach((input,i)=>saved[i]=input.value);
      const total=Number(count.value);
      const prefix=count.id;
      fields.replaceChildren();
      for(let i=0;i<total;i++) {
        const field=document.createElement('div');field.className='field';
        const label=document.createElement('label');label.className='form-label';label.htmlFor=`${prefix}-name-${i}`;
        label.textContent=`Passenger ${i+1} · full name`;
        const input=document.createElement('input');input.className='form-control';input.id=label.htmlFor;input.name='passengerName';
        input.required=true;input.maxLength=100;input.value=saved[i]??'';input.autocomplete=i===0?'name':'off';
        field.append(label,input);fields.append(field);
      }
      group.querySelector('[data-passenger-summary]').textContent=`${total} passenger${total===1?'':'s'}`;
      group.querySelector('[data-group-total]').textContent=new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(Number(group.dataset.unitFare)*total);
    });
  });
  adminSearch?.addEventListener('input', () => {
    const query = adminSearch.value.trim().toLocaleLowerCase();
    let visible = 0;
    document.querySelectorAll('[data-booking-row]').forEach(row => {
      row.hidden = !row.textContent.toLocaleLowerCase().includes(query);
      if (!row.hidden) visible++;
    });
    const feedback = document.querySelector('[data-booking-search-count]');
    if (feedback) feedback.textContent = `${visible} booking${visible === 1 ? '' : 's'} shown`;
  });
});
