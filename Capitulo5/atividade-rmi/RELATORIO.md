# Relatório — Trabalho 2: RPC/RMI

Disciplina: QXD0043 — Sistemas Distribuídos
Professor: Rafael Braga
Estudante(s): **preencher**
Matrícula(s): **preencher**
Repositório: **preencher com o URL da entrega**

## Serviço implementado

O serviço permite autenticar usuários, consultar perfil, publicar notificações, consultar histórico e encerrar a assinatura. Ele reimplementa a autenticação e as notificações da questão 4 do Trabalho 1, disponível em `Capitulo4/atividade-biblioteca`. A solução anterior usa autenticação TCP e notificações UDP multicast. Nesta versão, a autenticação ocorre por RMI e a distribuição usa callbacks RMI para cada assinante. Isso preserva o envio a vários usuários e atende à proibição de criar sockets neste trabalho. Não há UDP multicast nesta implementação.

As contas demonstrativas são `aluno/1234` e `admin/1234`. O aluno recebe eventos e consulta dados. O funcionário também pode publicar. Os tipos aceitos são `NOTIFICACAO`, `ALERTA` e `ATUALIZACAO`. Não existe interface gráfica; a interação acontece no terminal.

## Organização e entidades

| Entidade | Responsabilidade |
| --- | --- |
| Usuario | Base abstrata para nome e perfil |
| Aluno | Usuário assinante |
| Funcionario | Usuário autorizado a publicar |
| Notificacao | Tipo, mensagem, instante e autor |
| Canal | Histórico compartilhado de notificações |

As duas relações “é-um” são `Aluno extends Usuario` e `Funcionario extends Usuario`. As duas relações “tem-um” são `Notificacao → Usuario` e `Canal → List<Notificacao>`. São agregações: o autor existe independentemente de uma notificação, e o canal reúne objetos de notificação. Há cinco classes de entidades contando a classe abstrata, ou quatro concretas.

`Servidor` implementa a autenticação; sua classe interna `Sessao` implementa o protocolo remoto. `Cliente` oferece o terminal e exporta um callback. `Json` codifica e decodifica objetos JSON com campos de texto, sem dependências externas.

## Referências remotas e objetos por valor

As interfaces `BibliotecaRemota`, `ProtocoloRemoto` e `NotificadorRemoto` estendem `Remote`, e seus métodos declaram `RemoteException`.

A autenticação retorna uma `ProtocoloRemoto`: o cliente recebe um stub para a sessão que permanece no servidor. O cliente também entrega um `NotificadorRemoto` ao servidor. O objeto callback permanece no cliente e o servidor chama `receber` por essa referência. Esses são exemplos de passagem por referência remota; os objetos não são copiados como entidades locais.

Na publicação, o cliente transforma uma `Notificacao` e seus dados de autor em JSON. Esse texto passa por valor. O servidor usa `Notificacao.fromJson` para reconstruir novos objetos locais `Notificacao` e `Usuario`. O objeto original do cliente não é compartilhado. Antes de distribuir, o servidor define autor e horário com os dados da sessão, evitando aceitar uma identidade arbitrária do cliente.

A representação externa da aplicação é JSON para identificação de métodos, argumentos e resultados. Strings são transportadas pelo Java RMI, que usa seu próprio protocolo internamente; as entidades de negócio não dependem de serialização nativa Java. O codec aceita objetos com campos string, inclusive JSON aninhado como string; o histórico é uma lista JSON de notificações. Protocol Buffers é uma sugestão do enunciado, que também aceita JSON e XML.

## Protocolo de requisição e resposta

O registro RMI fornece a referência inicial chamada `Biblioteca`. Após autenticar, o cliente usa a referência da sessão para executar operações. Cada chamada inclui os campos previstos na mensagem de requisição da seção 5.2:

```json
{
  "messageType": "REQUEST",
  "requestId": "identificador-único",
  "objectReference": "Biblioteca",
  "methodId": "perfil",
  "arguments": "{}"
}
```

A resposta usa o mesmo identificador:

```json
{
  "messageType": "REPLY",
  "requestId": "identificador-único",
  "status": "OK",
  "result": "{\"nome\":\"aluno\",\"perfil\":\"ALUNO\"}"
}
```

`messageType` diferencia requisição e resposta; `requestId` correlaciona as duas; `objectReference` identifica logicamente o serviço e `methodId` seleciona a operação. A referência efetiva ao objeto remoto é o stub da sessão, enquanto o nome no envelope identifica o serviço lógico. `arguments` contém os dados externos de entrada; `result` contém o resultado. `status` é uma extensão para distinguir sucesso e erro.

Os métodos sugeridos foram adaptados, como permitido pelo PDF:

- `Cliente.doOperation(remoto, metodo, argumentos)` gera o identificador, monta a requisição, invoca o stub e valida a correlação da resposta.
- `Sessao.getRequest(json)` recebe a mensagem já entregue pelo runtime RMI e a decodifica.
- `Sessao.sendReply(id, status, resultado)` constrói o envelope retornado pela chamada RMI.

Assim, `getRequest` e `sendReply` são etapas do tratamento da chamada, não leitores e escritores de sockets. O runtime RMI cuida de conexão, despacho e retorno ao chamador. IP e porta de resposta não precisam fazer parte da assinatura de `sendReply`.

| Operação remota via methodId | Efeito |
| --- | --- |
| perfil | Retorna nome e perfil da sessão |
| publicar | Reconstrói notificação, armazena e dispara callbacks |
| historico | Retorna notificações já publicadas |
| sair | Remove a assinatura e bloqueia novas operações da sessão |

São quatro operações invocáveis pelo protocolo de despacho remoto. Além delas, há métodos RMI tipados de autenticação (`autenticar`), transporte (`doOperation`) e notificação (`receber`).

## Concorrência, falhas e limites

O RMI atende chamadas remotas. O conjunto de sessões usa uma coleção concorrente, o histórico tem acesso sincronizado e um pool executa os callbacks. Um callback indisponível remove o assinante da distribuição. Operações inválidas retornam um envelope `ERRO`; falhas de comunicação são reportadas como falhas RMI. Publicações são armazenadas antes do envio; uma resposta de sucesso não garante que todos os callbacks já tenham concluído.

Não há garantia de ordenação de callbacks, confirmação individual de leitura, persistência ou deduplicação de requisições. O `requestId` correlaciona mensagens, mas não implementa sozinho execução exatamente uma vez. Sessões exportadas são mantidas pelo runtime até a coleta distribuída de referências ou encerramento do servidor; o comando `sair` remove a assinatura e invalida a sessão. Credenciais fixas e armazenamento em memória são escolhas didáticas.

## Atendimento ao enunciado e validação

A implementação fornece cinco entidades, duas agregações, duas extensões, quatro operações no protocolo remoto, referências remotas e cópias por valor em JSON. Não instancia `Socket`, `ServerSocket`, `DatagramSocket` ou `MulticastSocket`. As notificações da atividade anterior são distribuídas por callbacks RMI.

O código foi compilado com OpenJDK 17. O teste `validar.py` executou JVMs separadas e verificou dois assinantes recebendo os três tipos, autenticação recusada, consulta de perfil e histórico, publicação sem permissão, método inexistente e saída. Os comandos para repetir estão no README. A execução em máquinas distintas é documentada, mas não foi validada neste ambiente.
