let accessToken = '', currentUser = null, activeVault = null;
const elementById = id => document.getElementById(id), inputValue = id => elementById(id).value.trim();
const formatMoney = inputCents => (inputCents/100).toLocaleString('es-CO',{style:'currency',currency:'COP'});
const inputCents = id => Math.round(Number(inputValue(id))*100);
const statusLabels = {pending: "Pendiente", approved: "Aprobada", rejected: "Rechazada", executed: "Ejecutada", ACCEPTED: "Aceptada", SIMULATED: "Simulada", FAILED: "Fallida", PENDING: "Pendiente"};
const statusInSpanish = status => statusLabels[status] || status;
const newIdempotencyKey = () => (crypto.randomUUID ? crypto.randomUUID() : String(Date.now())+Math.random());
function showAlert(t){let e=elementById('toast');e.textContent=t;e.style.display='block';setTimeout(()=>e.style.display='none',4500)}
function showResult(data){elementById('result').textContent=JSON.stringify(data,null,2)}
async function requestApi(path,method='GET',body=null){let h={'Content-Type':'application/json'};if(accessToken)h.Authorization='Bearer '+accessToken;let r=await fetch('/api/v1/'+path,{method,headers:h,body:body===null?null:JSON.stringify(body)});if(r.status===204)return null;let data=await r.json().catch(()=>({detail:'Respuesta no JSON'}));if(!r.ok)throw new Error(typeof data.detail==='string'?data.detail:JSON.stringify(data.detail));return data}
async function runAction(f){try{await f()}catch(e){showAlert(e.message);showResult({error:e.message})}}
function register(){runAction(async()=>{const d=await requestApi('identity/register','POST',{email:inputValue('registerEmail'),password:inputValue('registerPassword')});showResult(d);showAlert(d.message || 'Usuario registrado. Ya puedes iniciar sesión.')})}
function login(){runAction(async()=>{const d=await requestApi('identity/login','POST',{email:inputValue('loginEmail'),password:inputValue('loginPassword')});accessToken=d.access_token;if(d.activated){await startSession();showAlert('Sesión iniciada')}else{accessToken='';throw new Error('Cuenta anterior pendiente. Contacta al administrador de la demostración.')}})}
async function startSession(){currentUser=await requestApi('identity/me');elementById('who').textContent=currentUser.email;elementById('logoutBtn').style.display='inline-block';elementById('auth').classList.add('hide');elementById('dashboard').classList.remove('hide');elementById('myEmail').textContent=currentUser.email;elementById('myId').textContent=currentUser.id;elementById('vaultMembers').value=currentUser.id;elementById('beneficiary').value=currentUser.id;elementById('calendarDate').value=new Date().toISOString().slice(0,10);await refreshBalance();await loadVaultList()}
function logout(){accessToken='';currentUser=null;activeVault=null;elementById('reportSummary').textContent='';elementById('result').textContent='Sin operaciones.';elementById('auth').classList.remove('hide');elementById('dashboard').classList.add('hide');elementById('vaultPanel').classList.add('hide');elementById('logoutBtn').style.display='none';elementById('who').textContent='Sin sesión'}
function copyUserId(){navigator.clipboard?.writeText(currentUser.id).then(()=>showAlert('ID copiado')).catch(()=>showAlert('Copia el ID manualmente'))}
async function refreshBalance(){const b=await requestApi('ledger/balance');elementById('myBalance').textContent=formatMoney(b.balance_cents)}
function demoCredit(){runAction(async()=>{showResult(await requestApi('ledger/demo-credit','POST',{cents:5000000,idempotency_key:newIdempotencyKey()}));await refreshBalance()})}
function showStatement(){runAction(async()=>showResult(await requestApi('ledger/statement')))}
function verifyLedgerIntegrity(){runAction(async()=>showResult(await requestApi('ledger/verify-integrity')))}
function evaluateAccess(){runAction(async()=>showResult(await requestApi('identity/evaluate-action','POST')))}
function createCollectiveVault(){runAction(async()=>{const ids=inputValue('vaultMembers').split(',').map(s=>s.trim()).filter(Boolean);const d=await requestApi('vaults','POST',{name:inputValue('vaultName'),member_ids:ids,quorum:Number(inputValue('quorum')),milestones:inputValue('milestones').split(';').map(s=>s.trim()).filter(Boolean)});showResult(d);await loadVaultList();await openSelectedVault(d.vault_id);showAlert('Bóveda creada')})}
async function loadVaultList(){const list=await requestApi('vaults');const area=elementById('vaultList');area.replaceChildren();for(const v of list){const div=document.createElement('div');div.className='tile';const title=document.createElement('strong');title.textContent=`${v.name} (${v.quorum}/${v.members.length})`;const b=document.createElement('button');b.className='secondary';b.textContent='Abrir';b.onclick=()=>runAction(()=>openSelectedVault(v.id));div.append(title,document.createElement('br'),b);area.appendChild(div)}if(!list.length)area.textContent='Aún no tienes bóvedas.'}
async function openSelectedVault(id){activeVault=await requestApi('vaults/'+id);elementById('vaultPanel').classList.remove('hide');elementById('activeVaultName').textContent=activeVault.name;elementById('activeVaultId').textContent=activeVault.id;elementById('vaultBalance').textContent=formatMoney(activeVault.balance_cents);const ms=elementById('milestoneList'),select=elementById('proposalMilestone');ms.replaceChildren();select.replaceChildren();for(const m of activeVault.milestones){const card=document.createElement('div');card.className='tile';const title=document.createElement('strong');title.textContent=m.title;const b=document.createElement('button');b.className='secondary';b.textContent='Validar cumplimiento';b.onclick=()=>runAction(async()=>{let note=prompt('Nota de evidencia (al menos 5 caracteres)');if(note===null)return;showResult(await requestApi('vaults/milestones/'+m.id+'/attest','POST',{note}));await openSelectedVault(id)});card.append(title,document.createElement('br'),b);ms.appendChild(card);const o=document.createElement('option');o.value=m.id;o.textContent=m.title;select.appendChild(o)}const ps=elementById('proposalList');ps.replaceChildren();for(const p of activeVault.proposals){const card=document.createElement('div');card.className='tile';const title=document.createElement('strong');title.textContent=`${p.purpose} — ${formatMoney(p.amount_cents)}`;const txt=document.createElement('p');txt.textContent=`Estado: ${statusInSpanish(p.status)} | votos: ${p.approval_count}/${p.quorum} | hitos: ${p.attestation_count}/${p.quorum}`;card.append(title,txt);for(const [label,path] of [['Aprobar','approve'],['Rechazar','reject'],['Ejecutar','execute']]){const b=document.createElement('button');b.className=path==='reject'?'danger':'secondary';b.textContent=label;b.onclick=()=>runAction(async()=>{showResult(await requestApi('vaults/proposals/'+p.proposal_id+'/'+path,'POST'));await openSelectedVault(id);await refreshBalance()});card.appendChild(b)}ps.appendChild(card)}if(!activeVault.proposals.length)ps.textContent='No hay propuestas.'}
function makeContribution(){runAction(async()=>{const d=await requestApi('vaults/'+activeVault.id+'/contribute','POST',{cents:inputCents('contribution'),idempotency_key:newIdempotencyKey()});showResult(d);await refreshBalance();await openSelectedVault(activeVault.id)})}
function submitProposal(){runAction(async()=>{const d=await requestApi('vaults/'+activeVault.id+'/proposals','POST',{milestone_id:inputValue('proposalMilestone'),beneficiary_id:inputValue('beneficiary'),amount_cents:inputCents('proposalAmount'),purpose:inputValue('proposalPurpose')});showResult(d);await openSelectedVault(activeVault.id)})}
function verifyVaultAudit(){runAction(async()=>showResult(await requestApi('vaults/'+activeVault.id+'/verify-audit')))}
function getExchangeRate(){runAction(async()=>showResult(await requestApi('ledger/fx/quote?base='+encodeURIComponent(inputValue('fxBase'))+'&quote='+encodeURIComponent(inputValue('fxQuote'))+'&amount='+encodeURIComponent(inputValue('fxAmount')))))}
function getNextBusinessDay(){runAction(async()=>showResult(await requestApi('ledger/calendar/next-business-day?country=CO&on='+encodeURIComponent(inputValue('calendarDate')))))}

function loadReports() {
    runAction(async () => {
        const report = await requestApi('reports/summary');
        elementById('reportSummary').textContent = `Ingresos: ${formatMoney(report.income_cents)} | Egresos: ${formatMoney(report.expense_cents)} | Aportes a bóvedas: ${formatMoney(report.contribution_cents)} | Neto: ${formatMoney(report.net_cents)} | Movimientos: ${report.movement_count}`;
        showResult(report);
    });
}

function showReportSection(section) {
    runAction(async () => showResult(await requestApi('reports/' + section)));
}

function exportReport() {
    runAction(async () => {
        const response = await fetch('/api/v1/reports/export', {
            headers: {Authorization: 'Bearer ' + accessToken}
        });
        if (!response.ok) throw new Error('No se pudo exportar el reporte');
        const url = URL.createObjectURL(await response.blob());
        const link = document.createElement('a');
        link.href = url;
        link.download = 'smartvault-report.csv';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
}
