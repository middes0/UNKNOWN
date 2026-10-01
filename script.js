const savedMessages=JSON.parse(localStorage.getItem("unknown_messages")||"null");
const state={unlocked:false,currentApp:null,notes:JSON.parse(localStorage.getItem("unknown_notes")||"[]"),messages:savedMessages||[
 {name:"Número desconhecido",text:"Você não deveria ter encontrado isso.",time:"02:17",unread:true,history:["Você não deveria ter encontrado isso.","Se você está lendo esta mensagem, ainda há tempo."]},
 {name:"Mãe",text:"Me avisa quando chegar.",time:"ontem",unread:false,history:["Me avisa quando chegar."]}
]};
function saveMessages(){localStorage.setItem("unknown_messages",JSON.stringify(state.messages));}

const $=s=>document.querySelector(s);
const lockScreen=$("#lockScreen"),homeScreen=$("#homeScreen"),appScreen=$("#appScreen"),appBody=$("#appBody"),toast=$("#toast"),notification=$("#notification");

function clock(){
 const d=new Date();
 const time=d.toLocaleTimeString("pt-BR",{hour:"2-digit",minute:"2-digit"});
 $("#lockTime").textContent=time; $("#homeTime").textContent=time;
 $("#lockDate").textContent=d.toLocaleDateString("pt-BR",{weekday:"long",day:"numeric",month:"long"});
 $("#homeDate").textContent=d.toLocaleDateString("pt-BR",{day:"2-digit",month:"short"}).replace(".","").toUpperCase();
}
clock(); setInterval(clock,1000);

function unlock(){
 if(state.unlocked)return;
 state.unlocked=true; lockScreen.classList.add("hidden"); homeScreen.classList.remove("hidden"); homeScreen.classList.add("fade-in");
 setTimeout(()=>showNotification(0),3500);
}
let startY=0;
lockScreen.addEventListener("touchstart",e=>startY=e.touches[0].clientY);
lockScreen.addEventListener("touchend",e=>{if(startY-e.changedTouches[0].clientY>45)unlock()});
lockScreen.addEventListener("click",e=>{if(e.target.closest(".swipe-area"))unlock()});

function openApp(app){
 state.currentApp=app; homeScreen.classList.add("hidden"); appScreen.classList.remove("hidden"); renderApp(app);
}
function closeApp(){appScreen.classList.add("hidden");homeScreen.classList.remove("hidden");state.currentApp=null}
function toastMsg(msg){toast.textContent=msg;toast.classList.add("show");setTimeout(()=>toast.classList.remove("show"),1800)}
function showNotification(i){
 const m=state.messages[i]; if(!m||!m.unread)return;
 $("#notificationTitle").textContent=m.name;
 $("#notificationBody").textContent=m.text;
 notification.classList.remove("hidden");
 clearTimeout(window.notificationTimer);
 window.notificationTimer=setTimeout(()=>notification.classList.add("hidden"),6500);
}
notification.onclick=()=>{notification.classList.add("hidden");openApp("messages");};

function renderApp(app){
 const data={
 messages:["Mensagens","2 conversas"],
 phone:["Telefone","Chamadas recentes"],
 gallery:["Galeria","3 itens"],
 files:["Arquivos","Armazenamento interno"],
 browser:["Navegador","Pesquisa"],
 email:["E-mail","Caixa de entrada"],
 settings:["Ajustes","Sistema"],
 notes:["Notas","Anotações locais"]
 }[app]||[app,""];
 $("#appTitle").textContent=data[0];$("#appSubtitle").textContent=data[1];appBody.innerHTML="";appBody.className="app-body fade-in";
 ({messages:renderMessages,phone:renderPhone,gallery:renderGallery,files:renderFiles,browser:renderBrowser,email:renderEmail,settings:renderSettings,notes:renderNotes}[app]||renderEmpty)();
}

function renderMessages(){
 appBody.innerHTML=state.messages.map((m,i)=>`<button class="list-row" data-chat="${i}" style="width:100%;text-align:left"><span class="avatar">✉</span><span class="row-main"><b>${escapeHtml(m.name)}</b><small>${escapeHtml(m.text)}</small></span><span class="time">${escapeHtml(m.time)}${m.unread?" •":""}</span></button>`).join("");
 appBody.querySelectorAll("[data-chat]").forEach(b=>b.onclick=()=>renderChat(+b.dataset.chat));
}
function renderChat(i){
 const m=state.messages[i];m.unread=false;saveMessages();
 const history=m.history||[m.text];
 appBody.innerHTML=`<div class="app-card"><h3>${escapeHtml(m.name)}</h3><p>Conversa</p></div><div id="chat">${history.map((msg,n)=>`<div class="message-bubble ${n%2===0?"":"me"}">${escapeHtml(msg)}</div>`).join("")}</div><div class="chat-input"><input id="msgInput" placeholder="Mensagem"><button class="send" id="sendBtn">↑</button></div>`;
 $("#sendBtn").onclick=()=>{
   const v=$("#msgInput").value.trim();if(!v)return;
   m.history=m.history||[m.text];m.history.push(v);m.text=v;m.time="agora";saveMessages();
   $("#chat").insertAdjacentHTML("beforeend",`<div class="message-bubble me">${escapeHtml(v)}</div>`);
   $("#msgInput").value="";
 };
}
function renderPhone(){appBody.innerHTML=`<div class="app-card"><h3>Chamadas recentes</h3><p>Nenhuma chamada registrada.</p></div><div class="empty">O telefone está em silêncio.</div>`}
function renderGallery(){appBody.innerHTML=`<div class="app-card"><h3>Galeria</h3><p>3 itens • sincronização local</p></div><div class="app-card"><div style="height:180px;border-radius:12px;background:linear-gradient(145deg,#111,#282828);display:grid;place-items:center;font-size:48px">▧</div><p>IMG_0001.jpg</p></div><div class="app-card"><div style="height:120px;border-radius:12px;background:#111;display:grid;place-items:center;font-size:34px">?</div><p>arquivo_corrompido.png</p></div>`}
function renderFiles(){appBody.innerHTML=`<div class="app-card"><h3>Armazenamento interno</h3><p>4,8 GB usados de 64 GB</p></div><div class="list-row"><span class="avatar">□</span><span class="row-main"><b>DCIM</b><small>12 arquivos</small></span></div><div class="list-row"><span class="avatar">□</span><span class="row-main"><b>Downloads</b><small>4 arquivos</small></span></div><div class="list-row"><span class="avatar">□</span><span class="row-main"><b>documentos</b><small>1 arquivo</small></span></div>`}
function renderBrowser(){appBody.innerHTML=`<input class="search" id="browserSearch" placeholder="Pesquisar ou digitar endereço"><div class="app-card"><h3>Não há conexão</h3><p>Este navegador pertence ao jogo. Alguns sites serão desbloqueados conforme a história avança.</p></div><div id="browserResult"></div>`;$("#browserSearch").addEventListener("keydown",e=>{if(e.key==="Enter"){const q=e.target.value.trim();$("#browserResult").innerHTML=q?`<div class="app-card"><h3>Pesquisa</h3><p>Nenhum resultado para “${escapeHtml(q)}”.</p></div>`:""}})}
function renderEmail(){appBody.innerHTML=`<div class="list-row"><span class="avatar">@</span><span class="row-main"><b>admin@unknown.local</b><small>Assunto: não abra o arquivo</small></span><span class="time">03:41</span></div><div class="app-card"><h3>Caixa de entrada</h3><p>Existe 1 mensagem. Abra quando estiver pronto.</p></div>`}
function renderSettings(){appBody.innerHTML=`<div class="app-card"><h3>Sobre este dispositivo</h3><p>UNKNOWN Phone<br>Versão 0.1<br>ID: U-0001</p></div><div class="app-card"><h3>Privacidade</h3><p>Os dados da partida ficam salvos neste dispositivo.</p></div><div class="app-card"><button id="resetGame" style="width:100%;text-align:left">Redefinir dados da partida</button></div>`;$("#resetGame").onclick=()=>{if(confirm("Apagar os dados locais desta partida?")){localStorage.clear();location.reload()}}}
function renderNotes(){appBody.innerHTML=`<div class="app-card"><h3>Nova nota</h3><input class="search" id="noteInput" placeholder="Escreva uma pista..."><button id="saveNote" class="send" style="width:100%;height:38px">Salvar nota</button></div><div id="notesList"></div>`;const list=$("#notesList");const draw=()=>list.innerHTML=state.notes.length?state.notes.map((n,i)=>`<div class="app-card"><p>${escapeHtml(n)} </p><button data-del="${i}" style="color:#777;font-size:11px">apagar</button></div>`).join(""):'<div class="empty">Nenhuma nota.</div>';draw();$("#saveNote").onclick=()=>{const n=$("#noteInput").value.trim();if(!n)return;state.notes.push(n);localStorage.setItem("unknown_notes",JSON.stringify(state.notes));$("#noteInput").value="";draw();toastMsg("Nota salva")};list.onclick=e=>{if(e.target.dataset.del!==undefined){state.notes.splice(+e.target.dataset.del,1);localStorage.setItem("unknown_notes",JSON.stringify(state.notes));draw()}}}
function renderEmpty(){appBody.innerHTML='<div class="empty">Este aplicativo ainda está sendo desenvolvido.</div>'}
function escapeHtml(s){return s.replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"}[c]))}

document.addEventListener("click",e=>{
 const app=e.target.closest("[data-app]"); if(app){openApp(app.dataset.app);return}
 const action=e.target.closest("[data-action]");if(action&&action.dataset.action==="flash"){$("#flashOverlay").classList.add("on");setTimeout(()=>$("#flashOverlay").classList.remove("on"),300)}
});
$("#backButton").onclick=closeApp;$("#navHome").onclick=closeApp;$("#navBack").onclick=closeApp;
$("#appMenu").onclick=()=>toastMsg("Menu indisponível neste aplicativo.");
setTimeout(()=>{if(state.unlocked)showNotification(0)},12000);
