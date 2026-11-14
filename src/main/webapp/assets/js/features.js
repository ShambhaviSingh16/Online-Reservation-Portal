document.addEventListener('DOMContentLoaded',()=>{
  const base=document.querySelector('meta[name="trackease-context"]')?.content??'';
  const $=s=>document.querySelector(s);
  const node=(tag,text,className)=>{const e=document.createElement(tag);if(text!==undefined)e.textContent=text;if(className)e.className=className;return e;};
  const message=(text,error=true)=>{const box=$('[data-feature-message]');if(!box)return;box.hidden=false;box.className=`alert ${error?'alert-danger':'alert-info'}`;box.textContent=text;};
  async function api(path,method='GET',body){
    const headers={};if(method!=='GET'){
      const response=await fetch(base+'/api/auth/csrf',{credentials:'same-origin'});const token=await response.json();headers['X-CSRF-Token']=token.token;
      if(body!==undefined)headers['Content-Type']='application/json';
    }
    const response=await fetch(base+path,{method,headers,credentials:'same-origin',body:body===undefined?undefined:JSON.stringify(body)});
    const data=await response.json().catch(()=>({error:'Request could not be completed'}));
    if(!response.ok)throw new Error(data.error??data.message??`Request failed (${response.status})`);return data;
  }
  function busy(form,action){return async event=>{event.preventDefault();const button=form.querySelector('[type=submit]');if(button.disabled)return;button.disabled=true;try{await action();}catch(e){message(e.message);}finally{button.disabled=false;}};}
  const recover=$('[data-recover]');
  const resetRequest=$('[data-request-reset]');
  resetRequest?.addEventListener('submit',busy(resetRequest,async()=>{
    const result=await api('/api/auth/password-reset/request','POST',Object.fromEntries(new FormData(resetRequest)));message(result.message,false);
  }));
  const emailReset=$('[data-email-reset]');
  if(emailReset){
    const token=new URLSearchParams(location.hash.slice(1)).get('token');history.replaceState(null,'',location.pathname);
    if(!token||!/^[0-9a-f]{64}$/.test(token)){message('This reset link is incomplete. Request a new reset link.');emailReset.querySelector('button').disabled=true;}
    emailReset.addEventListener('submit',busy(emailReset,async()=>{
      const result=await api('/api/auth/password-reset/complete','POST',{token,newPassword:emailReset.elements.newPassword.value});emailReset.reset();message(result.message,false);
      const a=node('a',' Login now →','text-link');a.href=base+'/login.jsp';$('[data-feature-message]').append(a);
    }));
  }
  recover?.addEventListener('submit',busy(recover,async()=>{
    const result=await api('/api/auth/recover','POST',Object.fromEntries(new FormData(recover)));recover.reset();message(result.message,false);
    const a=node('a',' Login now →','text-link');a.href=base+'/login.jsp';$('[data-feature-message]').append(a);
  }));
  const generate=$('[data-generate-recovery]');
  generate?.addEventListener('submit',busy(generate,async()=>{
    const result=await api('/api/account/recovery-codes','POST',Object.fromEntries(new FormData(generate)));generate.reset();
    $('#recoveryCodes').value=result.codes.join('\n');$('[data-recovery-output]').hidden=false;message(result.message,false);
  }));
  $('[data-download-codes]')?.addEventListener('click',()=>{
    const url=URL.createObjectURL(new Blob(['TrackEase private recovery codes\nKeep these outside your account and browser.\n\n'+$('#recoveryCodes').value],{type:'text/plain'}));
    const a=node('a');a.href=url;a.download='trackease-private-recovery-codes.txt';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
  });
  const catalog=$('[data-catalog]');
  if(catalog){
    let page=0,total=0,sequence=0;
    const filters=()=>{
      const type=catalog.elements.type.value;const values=type==='bookings'?['all','CONFIRMED','RAC','WAITLISTED','CANCELLED']:type==='journeys'?['all','upcoming','departed']:['all','active','archived'];
      catalog.elements.filter.replaceChildren(...values.map(v=>new Option(v==='all'?'All records':v,v)));
    };
    async function load(){const current=++sequence;try{
      const query=new URLSearchParams(new FormData(catalog));query.set('page',page);
      const result=await api('/api/admin/catalog?'+query);if(current!==sequence)return;total=result.total;
      const head=$('[data-catalog-head]'),body=$('[data-catalog-body]');head.replaceChildren();body.replaceChildren();
      const columns=result.items.length?Object.keys(result.items[0]):[];const tr=node('tr');columns.forEach(key=>tr.append(node('th',key.replaceAll('_',' '))));head.append(tr);
      result.items.forEach(item=>{const row=node('tr');columns.forEach(key=>row.append(node('td',item[key]===null?'—':String(item[key]))));body.append(row);});
      if(!result.items.length){const row=node('tr');const cell=node('td','No matching records.');cell.colSpan=20;row.append(cell);body.append(row);}
      const size=Number(catalog.elements.size.value);$('[data-catalog-count]').textContent=`${total} matching records`;
      $('[data-catalog-page]').textContent=`Page ${page+1} of ${Math.max(1,Math.ceil(total/size))}`;
      $('[data-catalog-prev]').disabled=page===0;$('[data-catalog-next]').disabled=(page+1)*size>=total;
    }catch(e){message(e.message);}}
    filters();catalog.elements.type.addEventListener('change',()=>{filters();page=0;load();});
    catalog.addEventListener('submit',e=>{e.preventDefault();page=0;load();});
    catalog.elements.filter.addEventListener('change',()=>{page=0;load();});catalog.elements.size.addEventListener('change',()=>{page=0;load();});
    $('[data-catalog-prev]').addEventListener('click',()=>{if(page>0){page--;load();}});$('[data-catalog-next]').addEventListener('click',()=>{page++;load();});load();
  }
  function localDate(){const now=new Date();return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;}
  const advanced=$('[data-advanced-booking]');
  if(advanced){
    let journey=0,hold=null,expired=false,requestId=crypto.randomUUID(),selected=new Set(),timer=null,mapSequence=0,restoring=false;
    const from=$('[data-plan-from]'),to=$('[data-plan-to]'),travelClass=$('[data-plan-class]'),count=advanced.querySelector('[data-passenger-count]');
    const search=$('[data-segment-search]');const fields=()=>({journeyId:journey,from:Number(from.value),to:Number(to.value),travelClass:travelClass.value});
    function holdControls(){[from,to,travelClass,count].forEach(e=>e.disabled=Boolean(hold));$('[data-hold-seats]').disabled=Boolean(hold);$('[data-release-hold]').hidden=!hold&&!expired;}
    function updateHold(){
      if(!hold)return;const remaining=Math.ceil((Date.parse(hold.expiresAt)-Date.now())/1000);
      if(remaining<=0){hold=null;expired=true;clearInterval(timer);$('[data-hold-status]').textContent='Hold expired. Select seats again, or choose automatic allocation.';$('[data-release-hold]').textContent='Use automatic allocation';holdControls();refresh().catch(e=>message(e.message));return;}
      $('[data-hold-status]').textContent=`Seats held for ${Math.floor(remaining/60)}:${String(remaining%60).padStart(2,'0')}`;
    }
    async function refresh(){
      if(!journey)return;if(Number(to.value)<=Number(from.value))throw new Error('Choose an arrival stop after the boarding stop.');
      const sequence=++mapSequence;$('[data-seat-map]').setAttribute('aria-busy','true');const f=fields();const data=await api(`/api/journeys/${journey}/seats?`+new URLSearchParams({from:f.from,to:f.to,travelClass:f.travelClass}));if(sequence!==mapSequence)return;
      selected.clear();const map=$('[data-seat-map]');map.replaceChildren();let coach='';let grid;
      data.seats.forEach(seat=>{
        if(coach!==seat.coach){coach=seat.coach;map.append(node('h3',`${coach} · ${data.travelClass}`));grid=node('div',undefined,'seat-grid');map.append(grid);}
        const button=node('button',seat.number,'seat');button.type='button';button.dataset.seatKey=seat.key;button.disabled=seat.rac||seat.occupiedSlots>0||Boolean(hold);
        button.title=`${seat.coach} / ${seat.number} · ${seat.berth}${seat.rac?' · RAC sharing quota':''}`;button.setAttribute('aria-label',button.title);button.setAttribute('aria-pressed','false');
        if(seat.rac)button.classList.add('rac-seat');if(seat.occupiedSlots>0)button.classList.add('occupied');if(seat.heldByYou)button.classList.add('held-mine');
        button.addEventListener('click',()=>{if(selected.has(seat.key))selected.delete(seat.key);else if(selected.size<Number(count.value))selected.add(seat.key);else {message('Select no more seats than the passenger count.');return;}button.classList.toggle('selected',selected.has(seat.key));button.setAttribute('aria-pressed',String(selected.has(seat.key)));});grid.append(button);
      });
      const group=advanced.querySelector('[data-group-passengers]');group.dataset.unitFare=data.fare;
      group.querySelector('[data-group-total]').textContent=new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(Number(data.fare)*Number(count.value));
      $('[data-plan-fare]').textContent=new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(data.fare)+' / passenger';
      $('[data-seat-map]').setAttribute('aria-busy','false');
    }
    async function openPlan(id,start,end,cls){
      if(hold)throw new Error('Release your current seat hold before switching journeys.');
      const data=await api(`/api/journeys/${id}/plan`);journey=id;advanced.hidden=false;requestId=crypto.randomUUID();expired=false;
      const options=data.stops.map(s=>new Option(`${s.station_name} (${s.station_code}) · ${String(s.departure_time).replace('T',' ').slice(0,16)}`,s.stop_index));
      from.replaceChildren(...options);to.replaceChildren(...data.stops.map(s=>new Option(`${s.station_name} (${s.station_code})`,s.stop_index)));
      from.value=start??data.stops[0].stop_index;to.value=end??data.stops.at(-1).stop_index;
      travelClass.replaceChildren(...data.classes.map(c=>new Option(c.travel_class,c.travel_class)));if(cls)travelClass.value=cls;
      const active=await api('/api/bookings/holds/active?journeyId='+id);
      if(active.length){hold=active[0];from.value=hold.from;to.value=hold.to;travelClass.value=hold.travelClass;restoring=true;count.value=hold.passengers;count.dispatchEvent(new Event('change'));restoring=false;clearInterval(timer);timer=setInterval(updateHold,1000);updateHold();}
      holdControls();await refresh();advanced.scrollIntoView({behavior:'smooth',block:'start'});
    }
    api('/api/stations').then(stations=>{
      for(const select of [search.elements.departureStation,search.elements.arrivalStation])select.replaceChildren(new Option('Select a station',''),...stations.map(s=>new Option(`${s.stationName} (${s.stationCode})`,s.stationId)));
      search.elements.travelDate.value=localDate();search.elements.travelDate.min=localDate();
    }).catch(e=>message(e.message));
    search.addEventListener('submit',busy(search,async()=>{
      const results=await api('/api/journeys/segments?'+new URLSearchParams(new FormData(search)));const list=$('[data-segment-results]');list.replaceChildren();
      if(!results.length)list.append(node('p','No configured route matches this date and station pair. Try the standard journey search for single-class services.'));
      results.forEach(result=>{
        const row=node('article',undefined,'segment-result');const info=node('div');info.append(node('h3',`${result.train_name} · ${result.travel_class}`),node('p',`${result.departure} → ${result.arrival} · ${String(result.departure_time).replace('T',' ').slice(0,16)}`));
        const button=node('button',`View seats · ₹${result.fare}`,'button secondary small');button.type='button';button.addEventListener('click',()=>openPlan(result.journey_id,result.segment_start,result.segment_end,result.travel_class).catch(e=>message(e.message)));row.append(info,button);list.append(row);
      });
    }));
    [from,to,travelClass].forEach(e=>e.addEventListener('change',()=>{requestId=crypto.randomUUID();expired=false;refresh().catch(e=>message(e.message));}));
    count.addEventListener('change',()=>{if(restoring)return;requestId=crypto.randomUUID();refresh().catch(e=>message(e.message));});
    $('[data-refresh-seats]').addEventListener('click',()=>refresh().catch(e=>message(e.message)));
    $('[data-hold-seats]').addEventListener('click',async()=>{try{
      if(selected.size!==Number(count.value))throw new Error('Select exactly one seat per passenger before creating a hold.');
      hold=await api('/api/bookings/holds','POST',{...fields(),seats:[...selected]});expired=false;holdControls();updateHold();clearInterval(timer);timer=setInterval(updateHold,1000);await refresh();
    }catch(e){message(e.message);}});
    $('[data-release-hold]').addEventListener('click',async()=>{try{if(hold)await api('/api/bookings/holds/'+hold.holdId,'DELETE');hold=null;expired=false;clearInterval(timer);$('[data-hold-status]').textContent='Automatic allocation selected.';$('[data-release-hold]').textContent='Release hold';holdControls();await refresh();}catch(e){message(e.message);}});
    const reserve=$('[data-advanced-reserve]');reserve.addEventListener('submit',busy(reserve,async()=>{
      if(expired)throw new Error('Your selected-seat hold expired. Hold seats again or explicitly choose automatic allocation.');
      if(selected.size&&!hold)throw new Error('Hold the selected seats before confirming, or clear your selection to use automatic allocation.');
      const passengers=[...reserve.querySelectorAll('[name=passengerName]')].map(e=>e.value);
      const result=await api('/api/bookings/advanced','POST',{...fields(),passengers,preference:reserve.elements.preference.value,allowRac:reserve.elements.allowRac.checked,joinWaitingList:reserve.elements.joinWaitingList.checked,requestId,holdId:hold?.holdId??null});
      location.assign(base+'/user/bookingConfirmation.jsp?bookingId='+encodeURIComponent(result.bookingId));
    }));
    const id=Number(new URLSearchParams(location.search).get('journeyId'));if(id>0)openPlan(id).catch(e=>message(e.message));
  }
  const planner=$('[data-route-planner]');
  if(planner){
    let journeys=[],stations=[];let serial=0;
    const input=(label,type,value,required=true)=>{const wrap=node('div');const l=node('label',label,'form-label');const i=node('input',undefined,'form-control');i.type=type;i.value=value??'';i.id='planner-field-'+(++serial);i.required=required;l.htmlFor=i.id;wrap.append(l,i);return {wrap,input:i};};
    function addStop(){const row=node('div',undefined,'planner-row');row.dataset.stopRow='';const label=node('label','Station','form-label'),select=node('select',undefined,'form-select');select.dataset.station='';select.id='planner-station-'+(++serial);label.htmlFor=select.id;select.append(...stations.map(s=>new Option(s.stationName,s.stationId)));const field=node('div');field.append(label,select);row.append(field);
      for(const [name,text,type] of [['arrival','Arrival','datetime-local'],['departure','Departure','datetime-local'],['cumulativeFare','Cumulative fare','number']]){const f=input(text,type,'');f.input.dataset[name]='';if(type==='number'){f.input.min='0';f.input.step='.01';}row.append(f.wrap);}
      const remove=node('button','Remove','button secondary small');remove.type='button';remove.addEventListener('click',()=>row.remove());row.append(remove);$('[data-stop-rows]').append(row);
    }
    function addCoach(){const row=node('div',undefined,'planner-row');row.dataset.coachRow='';const label=input('Coach label','text','S'+($('[data-coach-rows]').children.length+1));label.input.dataset.coach='';label.input.pattern='[A-Z][A-Z0-9]{0,9}';row.append(label.wrap);
      const w=node('div'),l=node('label','Class','form-label'),sel=node('select',undefined,'form-select');sel.id='planner-class-'+(++serial);sel.dataset.travelClass='';l.htmlFor=sel.id;sel.append(...['STANDARD','SLEEPER','AC3','AC2'].map(c=>new Option(c,c)));w.append(l,sel);row.append(w);
      for(const [name,text,value,min,max,step] of [['seats','Seats / berths',40,1,80,1],['multiplier','Fare multiplier',1,.01,100,.01],['racBerths','RAC berths',0,0,10,1]]){const f=input(text,'number',value);f.input.dataset[name]='';Object.assign(f.input,{min,max,step});row.append(f.wrap);}
      const remove=node('button','Remove','button secondary small');remove.type='button';remove.addEventListener('click',()=>row.remove());row.append(remove);$('[data-coach-rows]').append(row);
    }
    Promise.all([api('/api/admin/journeys'),api('/api/stations')]).then(([js,ss])=>{
      journeys=js.filter(j=>!j.advancedReservations&&new Date(j.departureTime)>new Date());stations=ss;
      $('#plannerJourney').replaceChildren(...journeys.map(j=>new Option(`#${j.journeyId} · ${j.train.trainName} · ${j.departureStation.stationName} → ${j.arrivalStation.stationName}`,j.journeyId)));
      const preview=()=>{const j=journeys.find(j=>j.journeyId===Number($('#plannerJourney').value));if(!j)return;$('#routeFare').value=j.fare;$('[data-planner-preview]').textContent=`Departure ${String(j.departureTime).slice(0,16)} · Arrival ${String(j.arrivalTime).slice(0,16)}. Add intermediate stops between these times.`;};
      $('#plannerJourney').addEventListener('change',preview);preview();addCoach();if(!journeys.length)message('Create a future journey before configuring a route.');
    }).catch(e=>message(e.message));
    $('[data-add-stop]').addEventListener('click',addStop);$('[data-add-coach]').addEventListener('click',addCoach);
    planner.addEventListener('submit',busy(planner,async()=>{
      const journey=journeys.find(j=>j.journeyId===Number($('#plannerJourney').value));if(!journey)throw new Error('Select a journey.');
      const first=String(journey.departureTime).slice(0,19),last=String(journey.arrivalTime).slice(0,19);
      const stops=[{stationId:journey.departureStationId,arrival:first,departure:first,cumulativeFare:0}];
      document.querySelectorAll('[data-stop-row]').forEach(row=>stops.push({stationId:Number(row.querySelector('[data-station]').value),arrival:row.querySelector('[data-arrival]').value,departure:row.querySelector('[data-departure]').value,cumulativeFare:Number(row.querySelector('[data-cumulative-fare]').value)}));
      stops.push({stationId:journey.arrivalStationId,arrival:last,departure:last,cumulativeFare:Number($('#routeFare').value)});
      const coaches=[...document.querySelectorAll('[data-coach-row]')].map(row=>({coach:row.querySelector('[data-coach]').value,travelClass:row.querySelector('[data-travel-class]').value,seats:Number(row.querySelector('[data-seats]').value),multiplier:Number(row.querySelector('[data-multiplier]').value),racBerths:Number(row.querySelector('[data-rac-berths]').value)}));
      await api(`/api/admin/journeys/${journey.journeyId}/plan`,'PUT',{stops,coaches});message('Route and coaches configured. Passengers can now search its segments and classes.',false);
      const a=node('a',' Open passenger seat map →','text-link');a.href=base+'/user/reservation.jsp?journeyId='+journey.journeyId;$('[data-feature-message]').append(a);
    }));
  }
});
