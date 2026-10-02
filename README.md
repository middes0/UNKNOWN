# Middes Launcher 2.0

Reformulação nativa do Middes Launcher.

## Interface
- Home contextual com relógio, apps favoritos, dock e cena atual.
- Drawer com pesquisa instantânea, favoritos, recentes e menu de gerenciamento.
- Gestos: deslizar para cima abre os apps; deslizar para baixo abre a busca.
- Wallpaper persistente.

## Cenas
- Normal
- Estudo com foco de 25 minutos.
- Música com acesso rápido ao player instalado.
- Noite com interface escura e brilho reduzido na janela do launcher.
- Gaming com atalhos de jogos e métricas reais disponibilizadas pelo Android.

## Middes Flow
Um espaço visual separado em que os aplicativos mais relevantes ficam organizados em uma órbita dinâmica, usando favoritos, recentes e a cena ativa como contexto.

## NEXA
Assistente de voz integrado ao launcher com:
- wake word "Nexa" ou "Nessa";
- reconhecimento em português do Brasil;
- respostas por voz;
- barge-in durante a fala;
- comandos para abrir aplicativos;
- comandos para trocar cenas;
- comandos para abrir Flow, Apps e Configurações;
- controle do foco de estudo.

O listener da NEXA é encerrado quando o launcher perde o foco, evitando captura de microfone enquanto outro aplicativo está aberto.

## Arquitetura
A antiga MainActivity monolítica foi dividida em componentes:
- MainActivity
- HomeView
- DrawerView
- SettingsView
- MiddesFlowView
- GameModeView
- NexaController
- LauncherStore
- AppRepository
- SceneManager

## Build
O GitHub Actions gera automaticamente Middes-Launcher-debug em pushes no main e no branch de redesign.

A compilação final da reformulação foi validada pelo GitHub Actions no commit d2ebbb4c87f8d6f760caacd6d1939d81a25540ef.