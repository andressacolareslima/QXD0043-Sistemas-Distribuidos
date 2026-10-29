"""Teste entre JVMs reais. Execute após compilar, na pasta atividade-rmi."""
import subprocess
import time


def cliente(login, senha='1234'):
    return subprocess.Popen(['java', '-cp', 'target/classes', 'biblioteca.Cliente',
                             '127.0.0.1', login, senha], stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)


servidor = subprocess.Popen(['java', '-cp', 'target/classes', 'biblioteca.Servidor'],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
processos = [servidor]
try:
    time.sleep(1)
    if servidor.poll() is not None:
        raise RuntimeError(servidor.stdout.read())
    alunos = [cliente('aluno'), cliente('aluno')]
    processos.extend(alunos)
    time.sleep(1)
    admin = cliente('admin')
    processos.append(admin)
    admin_out = admin.communicate('perfil\npublicar NOTIFICACAO Novo livro disponível\n'
                                 'publicar ALERTA Fecha em 10 minutos\n'
                                 'publicar ATUALIZACAO Catálogo atualizado\nhistorico\nsair\n', timeout=15)[0]
    time.sleep(1)
    for aluno in alunos:
        out = aluno.communicate('perfil\npublicar ALERTA Sem permissão\ninexistente\nhistorico\nsair\n', timeout=15)[0]
        assert out.count('NOTIFICAÇÃO:') == 3, out
        assert 'Publicação exige funcionário' in out, out
        assert 'Método desconhecido' in out, out
        assert 'Catálogo atualizado' in out, out
    negado = cliente('aluno', 'errada')
    processos.append(negado)
    assert 'Credenciais inválidas' in negado.communicate('', timeout=15)[0]
    assert 'FUNCIONARIO' in admin_out and '"status":"OK"' in admin_out, admin_out
    print('OK: autenticação, dois assinantes, três tipos, perfil, histórico, autorização, método inválido e saída.')
finally:
    for processo in processos:
        if processo.poll() is None:
            processo.terminate()
        processo.wait(timeout=5)
