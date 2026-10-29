# Trabalho 2 — Biblioteca com Java RMI

Solução da questão 1 do `Trabalho_2_RMI.pdf`, tomando como base a questão 4 em `Capitulo4/atividade-biblioteca`: autenticação e distribuição de notificações. A entrega está nesta pasta; os exemplos anteriores permanecem disponíveis para comparação.

Arquivos de entrega: código em `src/biblioteca`, [relatório](RELATORIO.md) e [roteiro do vídeo](ROTEIRO-VIDEO.md). Substitua os campos de identificação do relatório e acrescente o endereço do seu repositório antes de entregar no Moodle. O PDF pede o URL do código e o relatório; o roteiro atende ao pedido adicional do aluno.

## Pré-requisito

JDK 17 ou superior com `java` e `javac` no PATH. Não precisa de Maven, bibliotecas externas, `rmic` ou iniciar `rmiregistry` separadamente. O servidor cria o registro.

```text
java -version
javac -version
```

## Windows / PowerShell

Abra um terminal na raiz do repositório:

```powershell
cd .\Capitulo5\atividade-rmi
New-Item -ItemType Directory -Force target/classes | Out-Null
$fontes = Get-ChildItem src/biblioteca/*.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d target/classes $fontes
java -cp target/classes biblioteca.Servidor
```

Mantenha esse terminal aberto. Abra outros três terminais, todos na pasta `Capitulo5/atividade-rmi`.

Terminal 2 — primeiro assinante:

```powershell
java -cp target/classes biblioteca.Cliente 127.0.0.1 aluno 1234
```

Terminal 3 — segundo assinante:

```powershell
java -cp target/classes biblioteca.Cliente 127.0.0.1 aluno 1234
```

Terminal 4 — funcionário que publica:

```powershell
java -cp target/classes biblioteca.Cliente 127.0.0.1 admin 1234
```

No console do funcionário, digite, uma linha por vez:

```text
perfil
publicar NOTIFICACAO Novo livro disponível
publicar ALERTA Biblioteca fecha em 10 minutos
publicar ATUALIZACAO Catálogo atualizado
historico
```

Os dois alunos recebem `NOTIFICAÇÃO:` sem precisar consultar o servidor. A ordem de chegada pode variar porque os callbacks são enviados concorrentemente. As respostas são envelopes JSON; o campo `result` contém a representação JSON do resultado como string, por isso aspas internas aparecem escapadas.

No console de um aluno, execute:

```text
perfil
historico
publicar ALERTA Tentativa sem autorização
inexistente
```

As últimas duas chamadas retornam `status=ERRO`. Em outro terminal, demonstre a recusa de autenticação:

```powershell
java -cp target/classes biblioteca.Cliente 127.0.0.1 aluno errada
```

Digite `sair` em cada cliente. Encerre o servidor com `Ctrl+C`. Os dados ficam em memória e desaparecem ao reiniciar.

## Linux / WSL

Na raiz do repositório:

```bash
cd Capitulo5/atividade-rmi
mkdir -p target/classes
javac -encoding UTF-8 -d target/classes src/biblioteca/*.java
java -cp target/classes biblioteca.Servidor
```

Use os mesmos comandos `java` da demonstração acima em terminais adicionais. Para evitar diferenças de rede entre Windows e WSL, execute inicialmente todos os processos no mesmo ambiente.

## Máquinas diferentes

Exemplo: servidor `192.168.1.10`, cliente `192.168.1.20`. Compile o código nas duas máquinas.

Servidor:

```text
java -cp target/classes biblioteca.Servidor 192.168.1.10 1099 1100
```

Primeiro cliente:

```text
java -cp target/classes biblioteca.Cliente 192.168.1.10 aluno 1234 1099 192.168.1.20 1201
```

Segundo cliente na mesma máquina:

```text
java -cp target/classes biblioteca.Cliente 192.168.1.10 admin 1234 1099 192.168.1.20 1202
```

Argumentos do servidor: `IP anunciado`, `porta do registro`, `porta dos objetos remotos`.
Argumentos do cliente: `IP do servidor`, `login`, `senha`, `porta do registro`, `IP anunciado pelo cliente`, `porta do callback`.

Libere TCP 1099 e 1100 no servidor e as portas escolhidas para callbacks nos clientes. O servidor precisa alcançar o IP do cliente para entregar notificações. Localmente a porta do callback é automática. O programa não cria sockets explicitamente; as conexões são administradas pelo RMI.

## Validação

Após compilar, na pasta desta atividade, com Python 3 instalado:

```bash
python3 validar.py
```

No Windows também pode usar `py -3 validar.py`. O teste inicia o servidor e dois assinantes, publica os três tipos e verifica autenticação, perfil, histórico, permissão, método inválido e saída. Ele precisa das portas 1099 e 1100 livres. Feche o servidor da demonstração antes de executar.

Validação realizada com OpenJDK 17: compilação e teste integrado passaram. A comunicação entre máquinas distintas não foi testada neste ambiente.

## Problemas comuns

- `Address already in use`: encerre o servidor anterior ou escolha outras portas e informe a porta do registro aos clientes.
- `Connection refused`: inicie o servidor e confira IP/porta.
- Cliente autentica, mas não recebe eventos em outra máquina: confira IP anunciado pelo cliente e acesso à porta de callback.
- `ClassNotFoundException`: execute na pasta desta atividade após a compilação.
