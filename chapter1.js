/* UNKNOWN — CAPÍTULO 1 */
(function(){
  const chapterStyle=document.createElement("style");
  chapterStyle.textContent=
    ".story-tag{display:inline-block;font-size:9px;letter-spacing:1.4px;color:#777;border:1px solid #292929;border-radius:20px;padding:5px 8px;margin-bottom:9px}" +
    ".story-btn{display:block;width:100%;padding:12px 14px;margin-top:8px;border:1px solid #282828;border-radius:13px;background:#151515;text-align:left;font-size:12px}" +
    ".story-btn:active{transform:scale(.99);background:#1d1d1d}" +
    ".choice-card{border-color:#292929}" +
    ".story-hint{background:#101010}" +
    ".muted,.document-muted{color:#777!important;font-size:11px!important;line-height:1.5}" +
    ".media-preview{height:180px;border-radius:12px;background:linear-gradient(145deg,#101010,#282828);display:grid;place-items:center;font-size:48px;margin-bottom:10px}" +
    ".media-preview.small{height:110px;font-size:34px;margin-bottom:9px}" +
    ".media-meta b{display:block;font-size:13px}.media-meta small{display:block;color:#777;font-size:10px;margin-top:4px}" +
    ".evidence{margin:10px 0;padding:12px;border:1px solid #222;border-radius:13px;background:#0d0d0d}" +
    ".evidence b{display:block;font-size:9px;letter-spacing:1.2px;color:#777;margin-bottom:6px}.evidence strong{display:block;font-size:14px}.evidence p{margin:0!important;color:#c2c2c2!important}" +
    ".evidence.danger{border-color:#332323}.evidence.danger b{color:#8d6b6b}" +
    ".terminal{background:#050505;border:1px solid #222;border-radius:13px;padding:14px;font-family:monospace;font-size:11px;line-height:1.7}" +
    ".terminal-line{color:#777}.terminal-ok{color:#cfcfcf;margin:4px 0 7px}" +
    ".document-view{padding:4px 1px 22px}.document-top{display:flex;justify-content:space-between;align-items:center}.doc-time{font-size:10px;color:#666}.document-view h2{font-size:22px;margin:10px 0 2px;letter-spacing:.6px}.document-divider{height:1px;background:#1b1b1b;margin:16px 0}" +
    ".ending-screen{text-align:center;padding:44px 14px 30px}.ending-mark{font-size:12px;letter-spacing:5px;color:#555;margin-bottom:30px}.ending-kicker{font-size:10px;letter-spacing:2px;color:#777}.ending-screen h2{font-size:23px;margin:10px 0 15px}.ending-screen p{font-size:13px;line-height:1.6;color:#aaa}.ending-quote{margin:20px 0;padding:18px 14px;border-top:1px solid #262626;border-bottom:1px solid #262626;font-size:14px;color:#ddd}.ending-muted{color:#666!important;font-size:11px!important}";
  document.head.appendChild(chapterStyle);

  function hasFlag(key){ return !!state.story[key]; }
  function setFlag(key,value){ state.story[key]=value===undefined?true:value; saveStory(); }

  function chapterAddMessage(name,text,history){
    let m=state.messages.find(x=>x.name===name);
    if(!m){
      m={name:name,text:text,time:"agora",unread:true,history:history||[text]};
      state.messages.unshift(m);
    }else{
      m.text=text;
      m.time="agora";
      m.unread=true;
      if(history)m.history=history;
    }
    saveMessages();
    if(typeof notifyMessage==="function")notifyMessage(m);
    return m;
  }

  function chapterTrigger(event){
    if(hasFlag(event))return;
    setFlag(event,true);

    if(event==="choiceAsk"){
      chapterAddMessage("Número desconhecido","Então procure IMG_0001.jpg. Não confie no nome do arquivo.");
      toastMsg("Nova pista recebida");
      return;
    }

    if(event==="choiceWho"){
      chapterAddMessage("Número desconhecido","Meu nome não vai ajudar. O horário vai: 02:17.");
      toastMsg("Uma pista foi deixada");
      return;
    }

    if(event==="photoOpened"){
      chapterAddMessage("Número desconhecido","O arquivo parece comum. O horário não é: 02:17. Transforme isso em quatro dígitos.");
      toastMsg("A foto revelou uma pista");
      return;
    }

    if(event==="fileOpened"){
      chapterAddMessage("Número desconhecido","Você chegou ao arquivo protegido. A senha não está escrita nele.");
      toastMsg("Arquivo protegido");
      return;
    }

    if(event==="fileUnlocked"){
      chapterAddMessage("Número desconhecido","Agora use 0217 no navegador. O acesso fica registrado.");
      toastMsg("Próxima pista desbloqueada");
      return;
    }

    if(event==="browserCode"){
      chapterAddMessage("admin@unknown.local","Você encontrou a página. O acesso foi registrado.");
      chapterAddMessage("Ana","Lucas, me responde. Você disse que eu deveria apagar as mensagens se alguém encontrasse o telefone.");
      chapterAddMessage("Daniel","Não abra o relatório final. Se você já abriu, não fale com a Ana.");
      toastMsg("Dois contatos apareceram");
      return;
    }

    if(event==="reportOpened"){
      chapterAddMessage("admin@unknown.local","O relatório não deveria estar acessível fora da rede. Agora já é tarde.");
      toastMsg("Relatório desbloqueado");
      return;
    }

    if(event==="anaRead"){
      const m=state.messages.find(x=>x.name==="Ana");
      if(m){
        m.history=[
          "Lucas, me responde. Você disse que eu deveria apagar as mensagens se alguém encontrasse o telefone.",
          "Eu não sabia o que o Lucas estava investigando. Só sabia que ele estava com medo.",
          "Ele me disse uma coisa estranha: se o telefone voltasse a funcionar, alguém já teria encontrado."
        ];
        m.text=m.history[m.history.length-1];
        m.unread=true;
        m.time="agora";
        saveMessages();
        if(typeof notifyMessage==="function")notifyMessage(m);
      }
      toastMsg("Ana revelou o que sabia");
      maybeUnlockSubject();
      return;
    }

    if(event==="danielRead"){
      const m=state.messages.find(x=>x.name==="Daniel");
      if(m){
        m.history=[
          "Não abra o relatório final. Se você já abriu, não fale com a Ana.",
          "Se aparecer um arquivo chamado sujeito_001.txt, não abra.",
          "O Lucas dizia que aquilo respondia sozinho."
        ];
        m.text=m.history[m.history.length-1];
        m.unread=true;
        m.time="agora";
        saveMessages();
        if(typeof notifyMessage==="function")notifyMessage(m);
      }
      toastMsg("Daniel deixou um aviso");
      maybeUnlockSubject();
      return;
    }

    if(event==="subjectOpened"){
      chapterAddMessage("Número desconhecido","Agora você sabe o nome. Ainda não sabe quem está usando o telefone.");
      toastMsg("A última pista apareceu");
      return;
    }
  }

  function maybeUnlockSubject(){
    if(hasFlag("reportOpened")&&hasFlag("anaRead")&&hasFlag("danielRead")){
      setFlag("subjectUnlocked",true);
      chapterAddMessage("Número desconhecido","Procure por sujeito_001.txt nos arquivos.");
    }
  }

  function chapterStatus(){
    if(hasFlag("chapter1Ending"))return "Capítulo 1 concluído";
    if(hasFlag("subjectUnlocked"))return "Última pista desbloqueada";
    if(hasFlag("reportOpened"))return "Descobrindo quem era Lucas";
    if(hasFlag("browserCode"))return "O acesso foi registrado";
    if(hasFlag("photoOpened"))return "A investigação começou";
    if(hasFlag("choiceMade"))return "Seguindo a primeira pista";
    return "Tudo parece normal.";
  }

  function chapterUpdateHome(){
    const e=document.querySelector(".widget-sub");
    if(e)e.textContent=chapterStatus();
  }

  window.triggerStory=chapterTrigger;
  window.updateHomeStatus=chapterUpdateHome;

  window.addMessage=chapterAddMessage;

  window.renderMessages=function(){
    const visible=state.messages.filter(function(m){
      if(m.name==="Ana"||m.name==="Daniel"||m.name==="admin@unknown.local")return hasFlag("browserCode");
      if(m.name==="Lucas")return hasFlag("chapter1Ending");
      return true;
    });

    appBody.innerHTML=
      '<div class="app-card"><h3>Caixa de mensagens</h3><p>'+visible.filter(function(m){return m.unread;}).length+' não lida(s) • progresso salvo</p></div>'+
      visible.map(function(m){
        const i=state.messages.indexOf(m);
        const avatar=m.name==="Ana"?"A":m.name==="Daniel"?"D":m.name==="Lucas"?"L":"✉";
        return '<button class="list-row" data-chat="'+i+'" style="width:100%;text-align:left"><span class="avatar">'+avatar+'</span><span class="row-main"><b>'+escapeHtml(m.name)+'</b><small>'+escapeHtml(m.text)+'</small></span><span class="time">'+escapeHtml(m.time)+(m.unread?" •":"")+'</span></button>';
      }).join("")+
      '<div class="app-card story-hint"><span class="story-tag">CAPÍTULO 1</span><p>Algumas conversas só aparecem depois que certas pistas são encontradas.</p></div>';

    appBody.querySelectorAll("[data-chat]").forEach(function(b){
      b.onclick=function(){renderChat(Number(b.dataset.chat));};
    });
  };

  window.renderChat=function(i){
    const m=state.messages[i];
    if(!m)return;
    m.unread=false;
    saveMessages();

    const history=m.history||[m.text];
    appBody.innerHTML=
      '<div class="app-card"><h3>'+escapeHtml(m.name)+'</h3><p>'+(m.name==="Número desconhecido"?"Contato sem identificação":m.name==="Lucas"?"Mensagem recuperada":"Conversa recente")+'</p></div>'+
      '<div id="chat">'+history.map(function(msg,n){return '<div class="message-bubble '+(n%2===0?"":"me")+'">'+escapeHtml(msg)+'</div>';}).join("")+'</div>'+
      '<div class="chat-input"><input id="msgInput" autocomplete="off" placeholder="Mensagem"><button class="send" id="sendBtn">↑</button></div>'+
      '<div id="storyChoices"></div>';

    const choices=document.querySelector("#storyChoices");

    if(m.name==="Número desconhecido"&&!hasFlag("choiceMade")){
      choices.innerHTML=
        '<div class="app-card choice-card"><p>O que você responde?</p>'+
        '<button class="story-btn" data-choice="who">“Quem é você?”</button>'+
        '<button class="story-btn" data-choice="ask">“O que aconteceu?”</button></div>';

      choices.querySelectorAll("[data-choice]").forEach(function(btn){
        btn.onclick=function(){
          const choice=btn.dataset.choice;
          setFlag("choiceMade",choice);
          const reply=choice==="ask"
            ?"Não posso explicar por aqui. Procure por uma foto com o nome IMG_0001."
            :"Meu nome não importa. Procure a foto. O horário é 02:17.";

          m.history=m.history||[m.text];
          m.history.push(reply);
          m.text=reply;
          m.time="agora";
          m.unread=false;
          saveMessages();

          document.querySelector("#chat").insertAdjacentHTML("beforeend",'<div class="message-bubble">'+escapeHtml(reply)+'</div>');
          choices.innerHTML="";
          chapterTrigger(choice==="ask"?"choiceAsk":"choiceWho");
          chapterUpdateHome();
        };
      });
    }

    const send=function(){
      const input=document.querySelector("#msgInput");
      if(!input)return;
      const v=input.value.trim();
      if(!v)return;
      m.history=m.history||[m.text];
      m.history.push(v);
      m.text=v;
      m.time="agora";
      saveMessages();
      document.querySelector("#chat").insertAdjacentHTML("beforeend",'<div class="message-bubble me">'+escapeHtml(v)+'</div>');
      input.value="";
    };

    document.querySelector("#sendBtn").onclick=send;
    document.querySelector("#msgInput").addEventListener("keydown",function(e){if(e.key==="Enter")send();});

    if(m.name==="Ana"&&hasFlag("browserCode")&&!hasFlag("anaRead")){
      const b=document.createElement("button");
      b.className="story-btn";
      b.textContent="Continuar conversa";
      b.onclick=function(){chapterTrigger("anaRead");renderChat(i);};
      choices.appendChild(b);
    }

    if(m.name==="Daniel"&&hasFlag("browserCode")&&!hasFlag("danielRead")){
      const b=document.createElement("button");
      b.className="story-btn";
      b.textContent="Continuar conversa";
      b.onclick=function(){chapterTrigger("danielRead");renderChat(i);};
      choices.appendChild(b);
    }

    if(m.name==="admin@unknown.local"){
      const b=document.createElement("button");
      b.className="story-btn";
      b.textContent="Ler aviso completo";
      b.onclick=function(){
        choices.innerHTML='<div class="evidence danger"><b>AVISO</b><p>Não procure por quem é Lucas. Procure por quem continua falando como ele.</p></div>';
      };
      choices.appendChild(b);
    }
  };

  window.renderGallery=function(){
    appBody.innerHTML=
      '<div class="app-card"><h3>Galeria</h3><p>3 itens • sincronização local</p></div>'+
      '<button class="app-card" id="photoClue" style="width:100%;text-align:left;color:inherit">'+
        '<div class="media-preview"><span>▧</span></div>'+
        '<div class="media-meta"><b>IMG_0001.jpg</b><small>'+(hasFlag("photoOpened")?"Metadados: 02:17 • detalhe encontrado":"Toque para abrir")+'</small></div>'+
      '</button>'+
      '<div class="app-card"><div class="media-preview small"><span>?</span></div><p>arquivo_corrompido.png</p><small class="muted">Pré-visualização indisponível.</small></div>'+
      '<div class="app-card"><div class="media-preview small"><span>▤</span></div><p>captura_sem_nome.png</p><small class="muted">Sem dados adicionais.</small></div>';

    document.querySelector("#photoClue").onclick=function(){
      chapterTrigger("photoOpened");
      document.querySelector("#photoClue small").textContent="Metadados: 02:17 • detalhe encontrado";
      if(!hasFlag("codeFound")){
        setTimeout(function(){chapterTrigger("codeFound");},900);
      }
    };
  };

  window.renderFiles=function(){
    appBody.innerHTML=
      '<div class="app-card"><h3>Armazenamento interno</h3><p>4,8 GB usados de 64 GB</p></div>'+
      '<div class="list-row"><span class="avatar">□</span><span class="row-main"><b>DCIM</b><small>12 arquivos</small></span></div>'+
      '<div class="list-row"><span class="avatar">□</span><span class="row-main"><b>Downloads</b><small>4 arquivos</small></span></div>'+
      '<button class="list-row" id="secretFile" style="width:100%;text-align:left"><span class="avatar">□</span><span class="row-main"><b>documentos</b><small>1 arquivo • '+(hasFlag("fileOpened")?"aberto":"protegido")+'</small></span><span class="time">'+(hasFlag("fileOpened")?"›":"🔒")+'</span></button>'+
      (hasFlag("browserCode")?'<button class="list-row" id="reportFile" style="width:100%;text-align:left"><span class="avatar">TXT</span><span class="row-main"><b>relatorio_final.txt</b><small>arquivo recém-disponível</small></span><span class="time">›</span></button>':"")+
      (hasFlag("subjectUnlocked")?'<button class="list-row" id="subjectFile" style="width:100%;text-align:left"><span class="avatar">001</span><span class="row-main"><b>sujeito_001.txt</b><small>registro interno</small></span><span class="time">›</span></button>':"");

    document.querySelector("#secretFile").onclick=function(){
      if(hasFlag("photoOpened")||hasFlag("choiceMade")){
        chapterTrigger("fileOpened");
        openProtectedFile();
      }else{
        toastMsg("Arquivo protegido");
      }
    };

    document.querySelector("#reportFile")?.addEventListener("click",openChapterReport);
    document.querySelector("#subjectFile")?.addEventListener("click",openSubjectFile);
  };

  function openProtectedFile(){
    appBody.innerHTML=
      '<div class="app-card">'+
      '<span class="story-tag">DOCUMENTOS / PROTEGIDO</span>'+
      '<h3>relatorio_final.txt</h3>'+
      '<p>O arquivo existe, mas está bloqueado.</p>'+
      '<p class="muted">A foto mostrou um horário: 02:17. Talvez seja mais do que um horário.</p>'+
      '<input class="search" id="filePassword" inputmode="numeric" maxlength="4" placeholder="Digite a senha de 4 dígitos">'+
      '<button class="story-btn" id="unlockFile">Desbloquear</button>'+
      '<div id="fileFeedback"></div></div>';

    document.querySelector("#unlockFile").onclick=function(){
      const v=document.querySelector("#filePassword").value.trim();
      if(v==="0217"){
        setFlag("fileUnlocked",true);
        chapterTrigger("fileUnlocked");
        document.querySelector("#fileFeedback").innerHTML='<div class="evidence"><b>ACESSO PARCIAL</b><p>O navegador é necessário para concluir o acesso.</p><small>Digite 0217 no navegador.</small></div>';
      }else{
        document.querySelector("#fileFeedback").innerHTML='<div class="evidence danger"><b>SENHA INCORRETA</b><p>O arquivo permanece bloqueado.</p></div>';
      }
    };
  }

  window.renderBrowser=function(){
    appBody.innerHTML=
      '<input class="search" id="browserSearch" autocomplete="off" placeholder="Pesquisar ou digitar endereço">'+
      '<div class="app-card"><h3>UNKNOWN Browser</h3><p>'+(
        hasFlag("browserCode")?"A página acessada continua disponível neste dispositivo.":"Sem conexão com a internet. Alguns endereços internos podem funcionar."
      )+'</p></div><div id="browserResult"></div>';

    document.querySelector("#browserSearch").addEventListener("keydown",function(e){
      if(e.key!=="Enter")return;
      const q=e.target.value.trim().toLowerCase();

      if(q==="0217"||q==="unknown.local/0217"||q==="https://unknown.local/0217"){
        chapterTrigger("browserCode");
        document.querySelector("#browserResult").innerHTML=
          '<div class="terminal"><div class="terminal-line">unknown.local/0217</div><div class="terminal-ok">ACESSO CONCEDIDO.</div><p>Usuário: externo</p><p>Registro: 02:17</p><button class="story-btn" id="openWebReport">Abrir relatorio_final.txt</button></div>';
        document.querySelector("#openWebReport").onclick=openChapterReport;
        return;
      }

      if((q==="lucas"||q==="projeto unknown"||q==="unknown")&&hasFlag("browserCode")){
        document.querySelector("#browserResult").innerHTML=
          '<div class="app-card"><h3>Pesquisa interna</h3><p>Resultados encontrados: <b>LUCAS A.</b> e <b>PROJECT UNKNOWN</b>.</p><button class="story-btn" id="searchReport">Abrir referência</button></div>';
        document.querySelector("#searchReport").onclick=openChapterReport;
        return;
      }

      document.querySelector("#browserResult").innerHTML=q
        ?'<div class="app-card"><h3>Pesquisa</h3><p>Nenhum resultado para “'+escapeHtml(e.target.value)+'”.</p></div>'
        :"";
    });
  };

  function openChapterReport(){
    if(!hasFlag("browserCode")){
      toastMsg("Primeiro encontre o acesso no navegador");
      return;
    }

    if(!hasFlag("reportOpened"))chapterTrigger("reportOpened");

    appBody.innerHTML=
      '<div class="document-view">'+
        '<div class="document-top"><span class="story-tag">UNKNOWN / RESTRITO</span><span class="doc-time">02:17</span></div>'+
        '<h2>RELATÓRIO FINAL</h2><p class="document-muted">Projeto UNKNOWN • Registro interno</p>'+
        '<div class="evidence"><b>SUJEITO</b><strong>LUCAS A.</strong></div>'+
        '<div class="evidence"><b>OBJETIVO</b><p>Reconstruir padrões de comportamento a partir de rastros digitais.</p></div>'+
        '<div class="evidence"><b>DADOS ANALISADOS</b><p>Mensagens • chamadas • fotos • buscas • arquivos • horários • escolhas.</p></div>'+
        '<div class="evidence"><b>STATUS</b><p><strong>ATIVO</strong></p></div>'+
        '<div class="evidence danger"><b>OBSERVAÇÃO</b><p>O sujeito apresenta consciência do processo de reconstrução.</p></div>'+
        '<div class="document-divider"></div>'+
        '<button class="story-btn" id="nextEvidence">Ver registro seguinte</button>'+
      '</div>';

    document.querySelector("#nextEvidence").onclick=function(){
      setFlag("evidenceOpened",true);
      chapterTrigger("subjectOpened");
      appBody.insertAdjacentHTML("beforeend",
        '<div class="evidence"><b>ÚLTIMA ANOTAÇÃO</b><p>“Se alguém estiver vendo isso, não confie nas pessoas que estão tentando me encontrar.”</p><small>— Lucas A.</small></div>'+
        '<div class="evidence"><b>REGISTRO DE ATIVIDADE</b><p>02:17 — acesso externo detectado.</p><p>02:17 — sujeito marcado como <strong>ATIVO</strong>.</p></div>'+
        '<div class="app-card story-hint"><span class="story-tag">AGORA</span><p>Leia as conversas de Ana e Daniel.</p></div>'
      );
      document.querySelector("#nextEvidence").remove();
      chapterUpdateHome();
    };
  }

  function openSubjectFile(){
    appBody.innerHTML=
      '<div class="document-view">'+
        '<span class="story-tag">DOCUMENTO INTERNO</span>'+
        '<h2>sujeito_001.txt</h2>'+
        '<p class="document-muted">Última alteração: 02:17</p>'+
        '<div class="evidence"><b>IDENTIFICAÇÃO</b><p>Lucas A.</p></div>'+
        '<div class="evidence"><b>STATUS</b><p><strong>ACTIVE</strong></p></div>'+
        '<div class="evidence"><b>NOTA</b><p>“O modelo não copia uma pessoa. Ele aprende a continuar depois dela.”</p></div>'+
        '<div class="evidence danger"><b>ERRO</b><p>Fonte de atividade: <strong>indeterminada</strong>.</p></div>'+
        '<button class="story-btn" id="finishChapter">Continuar</button>'+
      '</div>';

    document.querySelector("#finishChapter").onclick=function(){
      setFlag("subjectOpened",true);
      renderChapterEnding();
    };
  }

  window.renderEmail=function(){
    const unlocked=hasFlag("browserCode");
    appBody.innerHTML=unlocked?
      '<div class="list-row"><span class="avatar">@</span><span class="row-main"><b>admin@unknown.local</b><small>Assunto: acesso registrado</small></span><span class="time">03:41</span></div>'+
      '<button class="app-card" id="openAdminMail" style="width:100%;text-align:left;color:inherit"><span class="story-tag">E-MAIL NOVO</span><h3>não abra o arquivo</h3><p>O sistema registrou uma atividade que não deveria existir.</p></button>'
      :
      '<div class="empty">Caixa de entrada vazia.</div><div class="app-card"><p>Algumas mensagens aparecem depois que certas pistas são encontradas.</p></div>';

    document.querySelector("#openAdminMail")?.addEventListener("click",function(){
      appBody.innerHTML='<div class="document-view"><span class="story-tag">admin@unknown.local</span><h2>não abra o arquivo</h2><p class="document-muted">03:41</p><div class="evidence danger"><b>AVISO</b><p>O acesso que você acabou de fazer foi registrado como externo.</p></div><p>Não procure por quem é Lucas. Procure por quem continua falando como ele.</p><p class="muted">Esta mensagem não contém assinatura.</p></div>';
    });
  };

  window.renderPhone=function(){
    const unlocked=hasFlag("browserCode");
    appBody.innerHTML=
      '<div class="app-card"><h3>Chamadas recentes</h3><p>'+(
        unlocked?"Uma chamada desconhecida foi registrada às 02:17.":"Nenhuma chamada registrada."
      )+'</p></div>'+
      (unlocked?
        '<button class="list-row" id="unknownCall" style="width:100%;text-align:left"><span class="avatar">⌕</span><span class="row-main"><b>Número desconhecido</b><small>Chamada perdida • 02:17</small></span><span class="time">02:17</span></button><div class="app-card"><p>Você não lembra de ter recebido essa chamada.</p></div>'
        :'<div class="empty">O telefone está em silêncio.</div>');
    document.querySelector("#unknownCall")?.addEventListener("click",function(){toastMsg("Número indisponível");});
  };

  window.renderSettings=function(){
    appBody.innerHTML=
      '<div class="app-card"><h3>Sobre este dispositivo</h3><p>UNKNOWN Phone<br>Capítulo 1<br>ID: U-0001</p></div>'+
      '<div class="app-card"><h3>Progresso</h3><p>'+escapeHtml(chapterStatus())+'</p></div>'+
      '<div class="app-card"><h3>Privacidade</h3><p>Os dados da partida ficam salvos neste dispositivo.</p></div>'+
      '<div class="app-card"><button id="resetGame" style="width:100%;text-align:left">Redefinir dados da partida</button></div>';

    document.querySelector("#resetGame").onclick=function(){
      if(confirm("Apagar os dados locais desta partida?")){
        localStorage.clear();
        location.reload();
      }
    };
  };

  function renderChapterEnding(){
    setFlag("chapter1Ending",true);
    appBody.innerHTML=
      '<div class="ending-screen">'+
        '<div class="ending-mark">UNKNOWN</div>'+
        '<div class="ending-kicker">CAPÍTULO 1</div>'+
        '<h2>SUJEITO 001 — ACTIVE</h2>'+
        '<p>Você encontrou o relatório. Encontrou as pessoas que conheciam Lucas. Mas o telefone continua recebendo mensagens como se ele estivesse aqui.</p>'+
        '<div class="ending-quote">“Eu não mandei as primeiras mensagens.”</div>'+
        '<p class="ending-muted">Fim do primeiro capítulo.</p>'+
        '<button class="story-btn" id="endingBack">Voltar ao telefone</button>'+
      '</div>';

    document.querySelector("#endingBack").onclick=closeApp;
    chapterUpdateHome();
  }

  chapterUpdateHome();
  if(hasFlag("browserCode")&&!state.messages.some(function(m){return m.name==="Ana";})){
    chapterAddMessage("Ana","Lucas, me responde. Você disse que eu deveria apagar as mensagens se alguém encontrasse o telefone.");
    chapterAddMessage("Daniel","Não abra o relatório final. Se você já abriu, não fale com a Ana.");
  }
})();