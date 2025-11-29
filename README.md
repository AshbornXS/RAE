# RAE - Relação Aluno x Escola

O **RAE (Relação Aluno x Escola)** é uma aplicação Android nativa desenvolvida em Kotlin, concebida para facilitar a comunicação e a organização escolar. A aplicação serve como uma plataforma centralizada onde alunos podem consultar avisos, horários de aulas e informações específicas da sua turma, enquanto a administração escolar pode gerir e divulgar estas informações em tempo real.

## 📱 Funcionalidades

O sistema está dividido em dois perfis de utilizador: **Aluno** e **Administrador**.

### Para Todos os Utilizadores

  * **Autenticação Segura:** Login e Registo utilizando o **Firebase Authentication**.
      * *Nota:* O registo de alunos é restrito ao domínio institucional `@etec.sp.gov.br`.
  * **Perfil Personalizável:** Possibilidade de alterar a fotografia de perfil, nome e informações do curso.
  * **Modo Escuro (Dark Mode):** Suporte total a temas claros e escuros, com deteção automática do sistema.
  * **Navegação por Abas:** Interface intuitiva dividida em "Aulas", "Geral" e "Sala".

### Para Alunos

  * **Quadro de Avisos Gerais:** Visualização de comunicados importantes para toda a escola.
  * **Avisos Exclusivos:** Área dedicada a avisos específicos para a turma do aluno (filtrado por Série, Curso e Período).
  * **Horário de Aulas:** Visualização organizada da grelha horária semanal.
  * **Detalhes do Aviso:** Ao clicar num aviso, é possível ver detalhes completos, incluindo imagem, autor e data.

### Para Administradores (Coordenação/Secretaria)

  * **Gestão de Conteúdo:** Botão flutuante (FAB) exclusivo para adicionar novos conteúdos.
  * **Publicação de Avisos:**
      * Envio de avisos gerais.
      * Envio de avisos segmentados para turmas específicas (ex: "1º Ano - DS - Tarde").
      * Upload de imagens para ilustrar os avisos (integração com Firebase Storage).
  * **Gestão de Horários:** Ferramenta para upload e atualização da grelha de horários das turmas.
  * **Registo de Administradores:** Fluxo específico para criar novas contas com privilégios elevados.

## 🛠️ Tecnologias Utilizadas

O projeto utiliza uma arquitetura moderna de desenvolvimento Android:

  * **Linguagem:** [Kotlin](https://kotlinlang.org/)
  * **UI Toolkit:** XML Layouts & Material Design Components
  * **Backend (BaaS):** [Firebase](https://firebase.google.com/)
      * **Realtime Database:** Para armazenamento de dados de utilizadores, avisos e horários.
      * **Authentication:** Gestão de identidade e acesso.
      * **Storage:** Armazenamento de imagens de perfil e anexos de avisos.
  * **Carregamento de Imagens:** [Glide](https://github.com/bumptech/glide)
  * **Animações de Carregamento:** [Shimmer Android](https://github.com/facebook/shimmer-android)
  * **Arquitetura:** ViewBinding para interação com a UI e Coroutines para operações assíncronas.

## 🚀 Configuração e Instalação

Para executar este projeto localmente, siga os passos abaixo:

1.  **Clonar o repositório:**

    ```bash
    git clone https://github.com/AshbornXS/RAE.git
    ```

2.  **Abrir no Android Studio:**
    Abra o projeto utilizando a versão mais recente do Android Studio (compatível com Java 17).

3.  **Configuração do Firebase:**

      * O projeto depende do ficheiro `google-services.json` localizado em `app/`.
      * Certifique-se de que o ficheiro está presente e corresponde ao seu projeto na consola do Firebase.
      * Assegure-se de que as regras do *Realtime Database* e *Storage* permitem leitura/escrita conforme necessário para o desenvolvimento.

4.  **Compilar e Executar:**
    Sincronize os ficheiros Gradle e execute a aplicação num emulador ou dispositivo físico.

      * *Min SDK:* 24
      * *Target SDK:* 34

## 📂 Estrutura do Projeto

  * **`com.rae.daply.data`**: Classes de modelo de dados (`DataClass`) e Activities de manipulação de dados (Upload, Update, Detail).
  * **`com.rae.daply.fragment.ui`**: Fragmentos principais da navegação (Home, Exclusive, ClassesTime).
  * **`com.rae.daply.login`**: Lógica de autenticação e registo.
  * **`com.rae.daply.utils`**: Utilitários, incluindo configuração do Glide e persistência do Firebase.

## 🤝 Contribuição

Contribuições são bem-vindas\! Se deseja melhorar o RAE:

1.  Faça um Fork do projeto.
2.  Crie uma Branch para a sua funcionalidade (`git checkout -b feature/NovaFuncionalidade`).
3.  Faça o Commit das suas alterações (`git commit -m 'Adiciona NovaFuncionalidade'`).
4.  Faça o Push para a Branch (`git push origin feature/NovaFuncionalidade`).
5.  Abra um Pull Request.

## 📄 Licença

Este projeto está sob a licença MIT.

-----

Desenvolvido por **Pedro Henrique Alves de Azevedo**
